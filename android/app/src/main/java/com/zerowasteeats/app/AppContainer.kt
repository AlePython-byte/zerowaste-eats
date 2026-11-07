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
    }

    /**
     * The repositories of a signed-in business. Asking needs a [Session.Merchant], so the
     * customer space has no way to reach them. They are kept per account while the process
     * lives, so every screen of the business sees the same data.
     */
    fun merchantRepositories(session: Session.Merchant): MerchantRepositories =
        synchronized(merchantRepositories) {
            merchantRepositories.getOrPut(session.userId) {
                FakeMerchantData.repositoriesFor(session)
            }
        }
}
