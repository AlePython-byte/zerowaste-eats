package com.zerowasteeats.app

import android.content.Context
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.auth.PrefsTokenStore
import com.zerowasteeats.app.data.auth.SupabaseAuthRepository
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

    /** Null when local.properties names no Supabase project: then only demo data can work. */
    val supabaseAuth: SupabaseAuth? =
        if (apiConfig.supabaseUrl.isNotBlank() && apiConfig.supabasePublishableKey.isNotBlank()) {
            SupabaseAuthClient.create(
                apiConfig.supabaseUrl,
                apiConfig.supabasePublishableKey,
                httpClient
            )
        } else {
            null
        }

    /**
     * Retrofit for the backend, with the access token on every call and one renewal on a 401.
     * A renewal Supabase refuses ends the session. Null without a backend URL or a project.
     */
    val backendRetrofit: Retrofit? by lazy {
        val supabase = supabaseAuth
        if (apiConfig.apiBaseUrl.isBlank() || supabase == null) return@lazy null
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
        createRetrofit(apiConfig.apiBaseUrl, client)
    }

    /** Supabase plus the backend when both are configured; the demo accounts otherwise. */
    val authRepository: AuthRepository = run {
        val supabase = supabaseAuth
        val backend = backendRetrofit
        if (supabase != null && backend != null) {
            SupabaseAuthRepository(supabase, backend.create(UsersApi::class.java), tokenStore)
        } else {
            FakeAuthRepository()
        }
    }

    val sessionStore: SessionStore = PrefsSessionStore(
        SharedPreferencesKeyValueStore(context, PrefsSessionStore.FILE_NAME)
    )

    val sessionManager = SessionManager(authRepository, sessionStore, tokenStore)

    private val merchantRepositories = mutableMapOf<String, MerchantRepositories>()

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
