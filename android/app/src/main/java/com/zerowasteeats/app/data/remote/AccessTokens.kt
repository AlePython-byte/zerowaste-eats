package com.zerowasteeats.app.data.remote

import com.zerowasteeats.app.data.auth.AuthTokens
import com.zerowasteeats.app.data.auth.TokenStore
import com.zerowasteeats.app.data.auth.supabase.SupabaseAuth
import com.zerowasteeats.app.data.auth.supabase.SupabaseFailure
import com.zerowasteeats.app.data.auth.supabase.SupabaseResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val AUTHORIZATION = "Authorization"
private const val BEARER = "Bearer "

/** Adds the saved access token to every backend call that does not carry one already. */
class AuthTokenInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(AUTHORIZATION) != null) return chain.proceed(request)
        val tokens = tokenStore.load() ?: return chain.proceed(request)
        return chain.proceed(request.withToken(tokens.accessToken))
    }
}

/** What a refresh attempt ended in. */
sealed interface RefreshOutcome {
    data class Refreshed(val tokens: AuthTokens) : RefreshOutcome

    /** The refresh token is no longer accepted: only signing in again helps. */
    data object Rejected : RefreshOutcome

    /** No answer (offline, server trouble): keep the session, the next call can try again. */
    data object Unavailable : RefreshOutcome
}

/**
 * Answers a backend 401 by renewing the access token once and repeating the call with it.
 *
 * Renewals are serialized by a [Mutex]: when several calls fail at the same time, the first
 * one renews and the others find the new token already saved and just repeat with it, so a
 * refresh token (single use in Supabase) is never spent twice. If the renewal is rejected the
 * tokens are dropped and [onSessionExpired] runs once; if it is merely unavailable the 401 is
 * returned and nothing is dropped, so being offline never signs anyone out.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refresh: suspend (refreshToken: String) -> RefreshOutcome,
    private val onSessionExpired: () -> Unit
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sentToken = response.request.header(AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER) }
            ?.removePrefix(BEARER)
            ?: return null
        // A repeated call that still gets a 401 is not fixed by renewing again
        if (response.priorResponse != null) return null

        // OkHttp calls this on its own background thread, where blocking is expected
        return runBlocking {
            mutex.withLock {
                val current = tokenStore.load() ?: return@withLock null
                if (current.accessToken != sentToken) {
                    // Another call renewed while this one waited
                    return@withLock response.request.withToken(current.accessToken)
                }
                when (val outcome = refresh(current.refreshToken)) {
                    is RefreshOutcome.Refreshed -> {
                        // Signed out during the renewal: do not bring the tokens back
                        if (tokenStore.load()?.refreshToken != current.refreshToken) {
                            return@withLock null
                        }
                        tokenStore.save(outcome.tokens)
                        response.request.withToken(outcome.tokens.accessToken)
                    }
                    RefreshOutcome.Rejected -> {
                        tokenStore.clear()
                        onSessionExpired()
                        null
                    }
                    RefreshOutcome.Unavailable -> null
                }
            }
        }
    }
}

/** Renews with Supabase, telling a refused refresh token apart from a temporary failure. */
fun SupabaseAuth.tokenRefresher(): suspend (String) -> RefreshOutcome = { refreshToken ->
    when (val result = refresh(refreshToken)) {
        is SupabaseResult.Success -> RefreshOutcome.Refreshed(result.value.tokens)
        is SupabaseResult.Failure ->
            if (result.reason == SupabaseFailure.SESSION_REJECTED) {
                RefreshOutcome.Rejected
            } else {
                RefreshOutcome.Unavailable
            }
    }
}

private fun Request.withToken(accessToken: String): Request =
    newBuilder().header(AUTHORIZATION, BEARER + accessToken).build()
