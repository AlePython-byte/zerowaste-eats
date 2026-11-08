package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.auth.supabase.ProfileMetadata
import com.zerowasteeats.app.data.auth.supabase.SupabaseAuth
import com.zerowasteeats.app.data.auth.supabase.SupabaseFailure
import com.zerowasteeats.app.data.auth.supabase.SupabaseResult
import com.zerowasteeats.app.data.session.UserRole
import com.zerowasteeats.app.data.users.CreateProfileBody
import com.zerowasteeats.app.data.users.UserProfileResponse
import com.zerowasteeats.app.data.users.UsersApi
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Real accounts: Supabase Auth for the credentials, the backend for the profile.
 *
 * Sign up creates the Supabase account with the profile data in its metadata, then the backend
 * profile. If that second step fails the account still exists, so the result is
 * [AuthError.PROFILE_INCOMPLETE] and [completeProfile] finishes it later. Sign in reads the
 * backend profile and, if there is none (a sign up that never finished), creates it from the
 * metadata saved at sign up.
 *
 * Tokens are saved before any backend call, which reads them from [tokenStore]. Nothing here
 * logs a password, a token or a response.
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseAuth,
    private val users: UsersApi,
    private val tokenStore: TokenStore
) : AuthRepository {

    /** The profile of a sign up waiting for [completeProfile]; null after a restart. */
    @Volatile
    private var pendingProfile: ProfileMetadata? = null

    override suspend fun signUp(request: SignUpRequest): AuthResult {
        val email = request.email.trim()
        val metadata = ProfileMetadata(
            displayName = request.displayName.trim(),
            role = request.role.name,
            businessName = request.businessName?.trim()?.takeIf { request.role == UserRole.MERCHANT },
            city = request.city?.trim()?.takeIf { request.role == UserRole.MERCHANT }
        )
        val session = when (val result = supabase.signUp(email, request.password, metadata)) {
            is SupabaseResult.Success -> result.value
            is SupabaseResult.Failure -> return AuthResult.Failure(result.reason.toAuthError())
        }
        tokenStore.save(session.tokens)
        pendingProfile = metadata

        return when (val created = createProfile(metadata, session.user.email ?: email)) {
            is AuthResult.Success -> created.also { pendingProfile = null }
            // The account exists: retrying must finish it, not sign up again
            is AuthResult.Failure -> AuthResult.Failure(AuthError.PROFILE_INCOMPLETE)
        }
    }

    override suspend fun completeProfile(): AuthResult {
        val tokens = tokenStore.load() ?: return AuthResult.Failure(AuthError.UNEXPECTED)
        val pending = pendingProfile
        val (metadata, email) = if (pending != null) {
            pending to null
        } else {
            when (val user = supabase.currentUser(tokens.accessToken)) {
                is SupabaseResult.Success -> (user.value.metadata ?: ProfileMetadata()) to user.value.email
                is SupabaseResult.Failure ->
                    return AuthResult.Failure(AuthError.PROFILE_INCOMPLETE)
            }
        }
        return when (val created = createProfile(metadata, email)) {
            is AuthResult.Success -> created.also { pendingProfile = null }
            is AuthResult.Failure -> AuthResult.Failure(AuthError.PROFILE_INCOMPLETE)
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        val session = when (val result = supabase.signIn(email.trim(), password)) {
            is SupabaseResult.Success -> result.value
            is SupabaseResult.Failure -> return AuthResult.Failure(result.reason.toAuthError())
        }
        tokenStore.save(session.tokens)

        val result = try {
            AuthResult.Success(users.me().toAuthUser(session.user.metadata))
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // A sign up whose profile never reached the backend: finish it now
                createProfile(session.user.metadata ?: ProfileMetadata(), session.user.email ?: email)
            } else {
                AuthResult.Failure(failureOf(e))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(failureOf(e))
        }
        // Without a profile there is no session: the next attempt starts clean
        if (result is AuthResult.Failure) tokenStore.clear()
        return result
    }

    override suspend fun signOut(accessToken: String?) {
        pendingProfile = null
        if (accessToken != null) supabase.signOut(accessToken)
    }

    /** POST users/me; the backend answers the existing profile if it was already created. */
    private suspend fun createProfile(metadata: ProfileMetadata, email: String?): AuthResult {
        val role = metadata.role?.let { name -> UserRole.entries.firstOrNull { it.name == name } }
            ?: UserRole.CUSTOMER
        val body = CreateProfileBody(
            displayName = metadata.displayName?.takeIf { it.length >= 2 }
                ?: fallbackName(email),
            role = role.name,
            city = metadata.city?.takeIf { role == UserRole.MERCHANT && it.length >= 2 },
            businessName = metadata.businessName?.takeIf { role == UserRole.MERCHANT && it.length >= 2 }
        )
        return try {
            AuthResult.Success(users.create(body).toAuthUser(metadata))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(failureOf(e))
        }
    }

    private fun UserProfileResponse.toAuthUser(metadata: ProfileMetadata?): AuthUser {
        val profileRole = UserRole.entries.firstOrNull { it.name == role }
            ?: throw SerializationException("Unknown role")
        val isMerchant = profileRole == UserRole.MERCHANT
        return AuthUser(
            userId = id,
            role = profileRole,
            displayName = displayName,
            email = email,
            // The backend keeps the business on its own; the metadata has the name chosen
            businessName = if (isMerchant) metadata?.businessName ?: displayName else null,
            city = if (isMerchant) city ?: metadata?.city else null
        )
    }

    private fun failureOf(e: Exception): AuthError = when (e) {
        is IOException -> AuthError.NETWORK
        is HttpException -> if (e.code() == 429 || e.code() >= 500) AuthError.NETWORK else AuthError.UNEXPECTED
        else -> AuthError.UNEXPECTED
    }

    private fun SupabaseFailure.toAuthError(): AuthError = when (this) {
        SupabaseFailure.INVALID_CREDENTIALS -> AuthError.INVALID_CREDENTIALS
        SupabaseFailure.EMAIL_ALREADY_REGISTERED -> AuthError.EMAIL_ALREADY_REGISTERED
        SupabaseFailure.WEAK_PASSWORD -> AuthError.WEAK_PASSWORD
        SupabaseFailure.NETWORK -> AuthError.NETWORK
        SupabaseFailure.SESSION_REJECTED,
        SupabaseFailure.NO_SESSION,
        SupabaseFailure.UNEXPECTED -> AuthError.UNEXPECTED
    }

    /** The backend needs 2 to 80 characters; a profile with no saved name uses the email. */
    private fun fallbackName(email: String?): String =
        email?.substringBefore('@')?.take(80)?.takeIf { it.length >= 2 } ?: "Usuario"
}
