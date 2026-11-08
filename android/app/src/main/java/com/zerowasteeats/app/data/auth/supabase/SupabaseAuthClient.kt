package com.zerowasteeats.app.data.auth.supabase

import com.zerowasteeats.app.data.auth.AuthTokens
import com.zerowasteeats.app.data.remote.ApiJson
import com.zerowasteeats.app.data.remote.createRetrofit
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import retrofit2.HttpException

/** The Supabase user behind a session. [metadata] is what was saved at sign up. */
data class SupabaseUser(
    val id: String,
    val email: String?,
    val metadata: ProfileMetadata?
)

data class SupabaseSession(val tokens: AuthTokens, val user: SupabaseUser)

/** Why a Supabase Auth call failed, reduced to what the app reacts to differently. */
enum class SupabaseFailure {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_REGISTERED,
    WEAK_PASSWORD,
    /** The refresh token (or the session behind it) is no longer accepted: sign in again. */
    SESSION_REJECTED,
    /** The account was created but no session came back (email confirmation is on). */
    NO_SESSION,
    /** No answer, or a temporary one (rate limit, server error): trying again may work. */
    NETWORK,
    UNEXPECTED
}

sealed interface SupabaseResult<out T> {
    data class Success<T>(val value: T) : SupabaseResult<T>
    data class Failure(val reason: SupabaseFailure) : SupabaseResult<Nothing>
}

/** The Supabase Auth operations the app uses. An interface so the rest can be tested without HTTP. */
interface SupabaseAuth {
    suspend fun signUp(
        email: String,
        password: String,
        metadata: ProfileMetadata
    ): SupabaseResult<SupabaseSession>

    suspend fun signIn(email: String, password: String): SupabaseResult<SupabaseSession>

    suspend fun refresh(refreshToken: String): SupabaseResult<SupabaseSession>

    /** Ends the session on the server. Best effort: the caller forgets the tokens anyway. */
    suspend fun signOut(accessToken: String): SupabaseResult<Unit>

    suspend fun currentUser(accessToken: String): SupabaseResult<SupabaseUser>
}

/**
 * Talks to Supabase Auth over REST. Expected failures come back as [SupabaseResult.Failure];
 * nothing here logs a password, a token or a response body.
 */
class SupabaseAuthClient(
    private val api: SupabaseAuthApi,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) : SupabaseAuth {

    override suspend fun signUp(
        email: String,
        password: String,
        metadata: ProfileMetadata
    ): SupabaseResult<SupabaseSession> = call(Operation.SIGN_UP) {
        api.signUp(SignUpBody(email, password, metadata)).toSession()
    }

    override suspend fun signIn(email: String, password: String): SupabaseResult<SupabaseSession> =
        call(Operation.SIGN_IN) {
            api.signInWithPassword(PasswordGrantBody(email, password)).toSession()
        }

    override suspend fun refresh(refreshToken: String): SupabaseResult<SupabaseSession> =
        call(Operation.REFRESH) { api.refresh(RefreshGrantBody(refreshToken)).toSession() }

    override suspend fun signOut(accessToken: String): SupabaseResult<Unit> =
        call(Operation.OTHER) {
            val response = api.signOut(bearer(accessToken))
            // 401/404: the session is already gone on the server, which is what was wanted
            if (response.isSuccessful || response.code() == 401 || response.code() == 404) {
                SupabaseResult.Success(Unit)
            } else {
                throw HttpException(response)
            }
        }

    override suspend fun currentUser(accessToken: String): SupabaseResult<SupabaseUser> =
        call(Operation.OTHER) { SupabaseResult.Success(api.user(bearer(accessToken)).toUser()) }

    private fun SessionResponse.toSession(): SupabaseResult<SupabaseSession> {
        val access = accessToken
        val refresh = refreshToken
        val sessionUser = user
        if (access.isNullOrEmpty() || refresh.isNullOrEmpty() || sessionUser == null) {
            return SupabaseResult.Failure(SupabaseFailure.NO_SESSION)
        }
        val expiresAtSeconds = expiresAt ?: (nowEpochSeconds() + (expiresIn ?: 0L))
        return SupabaseResult.Success(
            SupabaseSession(AuthTokens(access, refresh, expiresAtSeconds), sessionUser.toUser())
        )
    }

    private fun UserResponse.toUser() = SupabaseUser(id, email, userMetadata)

    private enum class Operation { SIGN_UP, SIGN_IN, REFRESH, OTHER }

    private suspend fun <T> call(
        operation: Operation,
        block: suspend () -> SupabaseResult<T>
    ): SupabaseResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        SupabaseResult.Failure(SupabaseFailure.NETWORK)
    } catch (e: HttpException) {
        SupabaseResult.Failure(failureFor(operation, e))
    } catch (_: SerializationException) {
        SupabaseResult.Failure(SupabaseFailure.UNEXPECTED)
    } catch (_: IllegalArgumentException) {
        SupabaseResult.Failure(SupabaseFailure.UNEXPECTED)
    }

    private fun failureFor(operation: Operation, e: HttpException): SupabaseFailure {
        val status = e.code()
        if (status == 429 || status >= 500) return SupabaseFailure.NETWORK

        val body = readErrorBody(e)
        val code = body?.errorCode
        return when {
            code == "weak_password" -> SupabaseFailure.WEAK_PASSWORD
            code == "user_already_exists" || code == "email_exists" ->
                SupabaseFailure.EMAIL_ALREADY_REGISTERED
            operation == Operation.REFRESH -> SupabaseFailure.SESSION_REJECTED
            operation == Operation.SIGN_IN &&
                (code == "invalid_credentials" || body?.error == "invalid_grant" || status == 400) ->
                SupabaseFailure.INVALID_CREDENTIALS
            else -> SupabaseFailure.UNEXPECTED
        }
    }

    private fun readErrorBody(e: HttpException): SupabaseErrorBody? = try {
        e.response()?.errorBody()?.string()?.let { ApiJson.decodeFromString<SupabaseErrorBody>(it) }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IOException) {
        null
    }

    companion object {
        private fun bearer(token: String) = "Bearer $token"

        /** The Auth base URL of a project: `https://<ref>.supabase.co/auth/v1/`. */
        fun authBaseUrl(supabaseUrl: String): String = "${supabaseUrl.trimEnd('/')}/auth/v1/"

        /**
         * A client for the project at [supabaseUrl]. [baseClient] is shared with the rest of
         * the app; this one only adds the publishable key, never a user's token by itself.
         */
        fun create(
            supabaseUrl: String,
            publishableKey: String,
            baseClient: OkHttpClient
        ): SupabaseAuthClient {
            val client = baseClient.newBuilder()
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("apikey", publishableKey).build())
                }
                .build()
            val api = createRetrofit(authBaseUrl(supabaseUrl), client)
                .create(SupabaseAuthApi::class.java)
            return SupabaseAuthClient(api)
        }
    }
}
