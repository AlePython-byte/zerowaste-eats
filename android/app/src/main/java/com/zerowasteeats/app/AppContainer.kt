package com.zerowasteeats.app

import android.content.Context
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.auth.PrefsTokenStore
import com.zerowasteeats.app.data.auth.SupabaseAuthRepository
import com.zerowasteeats.app.data.settings.DataMode
import com.zerowasteeats.app.data.settings.DataModeController
import com.zerowasteeats.app.data.settings.DataModeStore
import com.zerowasteeats.app.data.settings.LiveSetup
import com.zerowasteeats.app.data.settings.buildLiveSetup
import com.zerowasteeats.app.data.users.UsersApi
import com.zerowasteeats.app.data.auth.TokenStore
import com.zerowasteeats.app.data.auth.supabase.SupabaseAuth
import com.zerowasteeats.app.data.auth.supabase.SupabaseAuthClient
import com.zerowasteeats.app.data.remote.ApiConfig
import com.zerowasteeats.app.data.remote.AuthTokenInterceptor
import com.zerowasteeats.app.data.remote.TokenAuthenticator
import com.zerowasteeats.app.data.remote.createAuthenticatedHttpClient
import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import com.zerowasteeats.app.data.remote.tokenRefresher
import retrofit2.Retrofit
import com.zerowasteeats.app.data.merchant.FakeMerchantData
import com.zerowasteeats.app.data.merchant.MerchantRepositories
import com.zerowasteeats.app.data.merchant.FakeMerchantReservationRepository
import com.zerowasteeats.app.data.merchant.MerchantOffersApi
import com.zerowasteeats.app.data.merchant.RemoteMerchantOfferRepository
import com.zerowasteeats.app.data.merchant.MerchantProfile
import com.zerowasteeats.app.data.merchant.MerchantProfileApi
import com.zerowasteeats.app.data.merchant.RemoteMerchantProfileRepository
import com.zerowasteeats.app.data.ai.AiSuggestionApi
import com.zerowasteeats.app.data.catalog.CustomerLocationSource
import com.zerowasteeats.app.data.catalog.DeviceLocationSource
import com.zerowasteeats.app.data.catalog.FakeOfferCatalogRepository
import com.zerowasteeats.app.data.catalog.OfferCatalogApi
import com.zerowasteeats.app.data.catalog.OfferCatalogRepository
import com.zerowasteeats.app.data.catalog.PastoLocationSource
import com.zerowasteeats.app.data.catalog.RemoteOfferCatalogRepository
import com.zerowasteeats.app.data.ai.DemoOfferSuggestionRepository
import com.zerowasteeats.app.data.ai.OfferSuggestionRepository
import com.zerowasteeats.app.data.ai.RemoteOfferSuggestionRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import com.zerowasteeats.app.data.photos.LocalOfferPhotoUploader
import com.zerowasteeats.app.data.photos.OfferPhotoPreparer
import com.zerowasteeats.app.data.photos.OfferPhotoUploader
import com.zerowasteeats.app.data.photos.SupabaseOfferPhotoUploader
import com.zerowasteeats.app.data.photos.OfferPhotoProcessor
import com.zerowasteeats.app.data.session.PrefsSessionStore
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.data.session.SessionManager
import com.zerowasteeats.app.data.session.SharedPreferencesKeyValueStore
import com.zerowasteeats.app.data.session.SessionStore

/**
 * Builds the app's long-lived objects by hand. Swapping the fake repositories for the real
 * ones later happens here, and nothing that uses them changes.
 */
class AppContainer(context: Context) {

    private val apiConfig = ApiConfig.fromBuildConfig()

    private val httpClient = createHttpClient()

    /** The Supabase tokens, in a file of their own that the session store never touches. */
    val tokenStore: TokenStore = PrefsTokenStore(
        SharedPreferencesKeyValueStore(context, PrefsTokenStore.FILE_NAME)
    )

    /** Live data needs both: Supabase for the account, the backend for everything else. */
    private class LiveClients(val supabase: SupabaseAuth, val backend: Retrofit)

    /**
     * The live clients, or why there are none: no endpoints in local.properties, or endpoints
     * the clients refused. Either way only the demo exists, and the app still starts.
     */
    private val liveSetup: LiveSetup<LiveClients> = buildLiveSetup(apiConfig.isComplete) {
        val supabase = SupabaseAuthClient.create(
            apiConfig.supabaseUrl,
            apiConfig.supabasePublishableKey,
            httpClient
        )
        // The backend gets the access token on every call and one renewal on a 401;
        // a renewal Supabase refuses ends the session
        val client = createAuthenticatedHttpClient(
            base = httpClient,
            interceptor = AuthTokenInterceptor(tokenStore),
            authenticator = TokenAuthenticator(
                tokenStore = tokenStore,
                refresh = supabase.tokenRefresher(),
                // Read when it fires: the session manager is built after this client
                onSessionExpired = { sessionManager.expireSession() }
            )
        )
        LiveClients(supabase, createRetrofit(apiConfig.apiBaseUrl, client))
    }

    /** True when the endpoints were set but unusable, so the app fell back to the demo. */
    val liveSetupInvalid: Boolean = liveSetup == LiveSetup.Invalid

    private val liveAvailable: Boolean = liveSetup is LiveSetup.Ready

    val sessionStore: SessionStore = PrefsSessionStore(
        SharedPreferencesKeyValueStore(context, PrefsSessionStore.FILE_NAME)
    )

    val sessionManager = SessionManager({ authRepository }, sessionStore, tokenStore)

    /** Live or demo data. Changes only while signed out, and rebuilds the repositories. */
    val dataMode = DataModeController(
        store = DataModeStore(SharedPreferencesKeyValueStore(context, DataModeStore.FILE_NAME)),
        liveAvailable = liveAvailable,
        isSignedIn = { sessionManager.isSignedIn },
        onChanged = { mode -> rebuildRepositories(mode) }
    )

    /** The account source of the current mode: Supabase plus the backend, or the demo accounts. */
    @Volatile
    var authRepository: AuthRepository = buildAuthRepository(dataMode.mode.value)
        private set

    private val merchantRepositories = mutableMapOf<String, MerchantRepositories>()

    private val offerCatalogs = mutableMapOf<String, OfferCatalogRepository>()

    private val deviceLocation: CustomerLocationSource = DeviceLocationSource(context)

    /** Readies a photo picked for an offer: upright, at most 1280 px, JPEG under 2 MB. */
    val offerPhotoPreparer: OfferPhotoPreparer = OfferPhotoProcessor(context.applicationContext)

    private val localPhotoUploader: OfferPhotoUploader = LocalOfferPhotoUploader()

    /**
     * Publishes a picked offer photo of the business [userId]: to Supabase Storage in live
     * mode, nowhere in demo mode (the offer keeps the file on the device).
     */
    fun offerPhotoUploader(userId: String): OfferPhotoUploader {
        val backend = liveSetup.clientsOrNull?.backend
        if (dataMode.mode.value != DataMode.LIVE || backend == null) return localPhotoUploader
        val users = backend.create(UsersApi::class.java)
        return SupabaseOfferPhotoUploader(
            // The backend's client: it carries the user's token and renews it on a 401
            client = backend.callFactory() as OkHttpClient,
            supabaseUrl = apiConfig.supabaseUrl,
            publishableKey = apiConfig.supabasePublishableKey,
            userId = userId,
            beforeUpload = {
                // A token about to expire is renewed through the backend, which answers it
                // with a 401 the authenticator handles; Storage might not
                val tokens = tokenStore.load()
                val nowSeconds = System.currentTimeMillis() / 1000
                if (tokens != null && tokens.expiresAtEpochSeconds - nowSeconds < TOKEN_MARGIN_SECONDS) {
                    runCatching { users.me() }
                }
            }
        )
    }

    private val demoSuggestions: OfferSuggestionRepository by lazy { DemoOfferSuggestionRepository() }

    private val liveSuggestions: OfferSuggestionRepository? by lazy {
        val backend = liveSetup.clientsOrNull?.backend ?: return@lazy null
        // The server may wait up to 30 s for the model: give the answer room to arrive
        val client = (backend.callFactory() as OkHttpClient).newBuilder()
            .readTimeout(SUGGESTION_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        RemoteOfferSuggestionRepository(
            backend.newBuilder().client(client).build().create(AiSuggestionApi::class.java)
        )
    }

    /**
     * Where offer suggestions come from in the current mode: the backend, or a local example
     * that works offline. The mode cannot change while a business is signed in.
     */
    fun offerSuggestions(): OfferSuggestionRepository =
        if (dataMode.mode.value == DataMode.LIVE) liveSuggestions ?: demoSuggestions
        else demoSuggestions

    private fun buildAuthRepository(mode: DataMode): AuthRepository {
        val live = liveSetup.clientsOrNull
        return if (mode == DataMode.LIVE && live != null) {
            SupabaseAuthRepository(live.supabase, live.backend.create(UsersApi::class.java), tokenStore)
        } else {
            FakeAuthRepository()
        }
    }

    private fun rebuildRepositories(mode: DataMode) {
        authRepository = buildAuthRepository(mode)
        synchronized(merchantRepositories) { merchantRepositories.clear() }
        synchronized(offerCatalogs) { offerCatalogs.clear() }
    }

    /**
     * The offers a signed-in customer browses: the backend in live mode, sample offers in demo
     * mode. Kept per account while the process lives, so Home, Explore and the detail share them.
     */
    fun offerCatalog(session: Session.Customer): OfferCatalogRepository =
        synchronized(offerCatalogs) {
            offerCatalogs.getOrPut(session.userId) {
                val backend = liveSetup.clientsOrNull?.backend
                if (dataMode.mode.value == DataMode.LIVE && backend != null) {
                    RemoteOfferCatalogRepository(backend.create(OfferCatalogApi::class.java))
                } else {
                    FakeOfferCatalogRepository()
                }
            }
        }

    /**
     * Where the customer looks for offers: their own location when they allowed it, or the
     * centre of Pasto. Demo mode always uses Pasto, where its sample offers are.
     */
    val customerLocation: CustomerLocationSource
        get() = if (dataMode.mode.value == DataMode.LIVE && liveAvailable) deviceLocation
        else PastoLocationSource

    /**
     * The repositories of a signed-in business. Asking needs a [Session.Merchant], so the
     * customer space has no way to reach them. They are kept per account while the process
     * lives, so every screen of the business sees the same data.
     */
    fun merchantRepositories(session: Session.Merchant): MerchantRepositories =
        synchronized(merchantRepositories) {
            merchantRepositories.getOrPut(session.userId) {
                val backend = liveSetup.clientsOrNull?.backend
                if (dataMode.mode.value == DataMode.LIVE && backend != null) {
                    liveMerchantRepositories(session, backend)
                } else {
                    FakeMerchantData.repositoriesFor(session)
                }
            }
        }

    /**
     * Offers come from the backend. Reservations start empty: the backend does not serve a
     * business's reservations yet, and the demo ones must not appear on a real account.
     */
    private fun liveMerchantRepositories(
        session: Session.Merchant,
        backend: Retrofit
    ): MerchantRepositories {
        return MerchantRepositories(
            offers = RemoteMerchantOfferRepository(backend.create(MerchantOffersApi::class.java)),
            reservations = FakeMerchantReservationRepository(emptyList()),
            profile = RemoteMerchantProfileRepository(
                backend.create(MerchantProfileApi::class.java),
                // What the session knows, shown until the backend answers
                MerchantProfile(
                    businessName = session.businessName,
                    description = "",
                    address = "",
                    city = session.city,
                    isVerified = false
                )
            )
        )
    }
}

/** Above the server's 30 s wait for the model, so a slow suggestion still arrives. */
private const val SUGGESTION_READ_TIMEOUT_SECONDS = 45L

/** A token closer than this to expiring is renewed before uploading a photo. */
private const val TOKEN_MARGIN_SECONDS = 60L
