package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.session.KeyValueStore

/** The Supabase tokens of the signed-in person. Kept apart from the session on purpose. */
class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    /** When [accessToken] stops being valid, in seconds since the epoch. */
    val expiresAtEpochSeconds: Long
) {
    override fun equals(other: Any?): Boolean =
        other is AuthTokens &&
            other.accessToken == accessToken &&
            other.refreshToken == refreshToken &&
            other.expiresAtEpochSeconds == expiresAtEpochSeconds

    override fun hashCode(): Int =
        31 * (31 * accessToken.hashCode() + refreshToken.hashCode()) +
            expiresAtEpochSeconds.hashCode()

    /** Never the tokens themselves, so printing one by mistake leaks nothing. */
    override fun toString(): String = "AuthTokens(expiresAt=$expiresAtEpochSeconds)"
}

/**
 * Keeps the tokens between launches, in their own file, apart from the [com.zerowasteeats.app
 * .data.session.SessionStore] (which never holds a token). The backup rules exclude this file
 * too, so a restored copy of the app carries no credentials.
 */
interface TokenStore {
    fun load(): AuthTokens?

    fun save(tokens: AuthTokens)

    fun clear()
}

class PrefsTokenStore(private val store: KeyValueStore) : TokenStore {

    // The authenticator reads and writes from OkHttp threads while the UI signs in or out
    private val lock = Any()

    override fun load(): AuthTokens? = synchronized(lock) {
        val access = store.getString(KEY_ACCESS_TOKEN)
        val refresh = store.getString(KEY_REFRESH_TOKEN)
        val expiresAt = store.getString(KEY_EXPIRES_AT)?.toLongOrNull()
        if (access.isNullOrEmpty() || refresh.isNullOrEmpty() || expiresAt == null) {
            // Half-written data is useless: drop it instead of sending a broken token
            if (access != null || refresh != null) store.replaceAll(emptyMap())
            null
        } else {
            AuthTokens(access, refresh, expiresAt)
        }
    }

    override fun save(tokens: AuthTokens) = synchronized(lock) {
        store.replaceAll(
            mapOf(
                KEY_ACCESS_TOKEN to tokens.accessToken,
                KEY_REFRESH_TOKEN to tokens.refreshToken,
                KEY_EXPIRES_AT to tokens.expiresAtEpochSeconds.toString()
            )
        )
    }

    override fun clear() = synchronized(lock) { store.replaceAll(emptyMap()) }

    companion object {
        /** SharedPreferences file name. The backup rules exclude this same file. */
        const val FILE_NAME = "auth_tokens"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
