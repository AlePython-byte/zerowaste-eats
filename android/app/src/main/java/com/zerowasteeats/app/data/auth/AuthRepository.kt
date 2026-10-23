package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.session.UserRole

/** The account behind a successful sign in or sign up. Never carries the password. */
data class AuthUser(
    val userId: String,
    val role: UserRole,
    val displayName: String,
    val email: String,
    /** Only set for [UserRole.MERCHANT]. */
    val businessName: String? = null
)

/** Data for a new account. A merchant also needs a business name and a city. */
data class SignUpRequest(
    val displayName: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val businessName: String? = null,
    val city: String? = null
) {
    init {
        require(role != UserRole.MERCHANT || !businessName.isNullOrBlank()) {
            "A merchant account needs a business name"
        }
    }
}

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_REGISTERED,
    NETWORK
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/**
 * Where accounts come from. Today a fake in memory; later Supabase plus the backend, with
 * the screens unchanged. Failures that are expected (wrong password, no network) come back
 * as [AuthResult.Failure] instead of exceptions.
 */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signUp(request: SignUpRequest): AuthResult
}
