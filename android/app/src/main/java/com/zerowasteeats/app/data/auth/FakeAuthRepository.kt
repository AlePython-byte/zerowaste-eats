package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.session.UserRole
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * In-memory accounts for the prototype. Two accounts always exist, both with [DEMO_PASSWORD]:
 * [DEMO_CUSTOMER_EMAIL] and [DEMO_MERCHANT_EMAIL]. Accounts created with sign up are lost
 * when the process ends.
 *
 * Every call waits [latencyMillis] to look like a real request. While [simulateNetworkError]
 * is true, every call fails with [AuthError.NETWORK] after that wait.
 */
class FakeAuthRepository(
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS
) : AuthRepository {

    @Volatile
    var simulateNetworkError: Boolean = false

    private class Account(val user: AuthUser, val password: String)

    private val lock = Any()
    private val accounts = mutableMapOf<String, Account>()

    init {
        register(
            Account(
                AuthUser(
                    userId = "demo-customer",
                    role = UserRole.CUSTOMER,
                    displayName = "Comprador Demo",
                    email = DEMO_CUSTOMER_EMAIL
                ),
                DEMO_PASSWORD
            )
        )
        register(
            Account(
                AuthUser(
                    userId = "demo-merchant",
                    role = UserRole.MERCHANT,
                    displayName = "Negocio Demo",
                    email = DEMO_MERCHANT_EMAIL,
                    businessName = "Panadería Demo",
                    city = "Pasto"
                ),
                DEMO_PASSWORD
            )
        )
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        delay(latencyMillis)
        if (simulateNetworkError) return AuthResult.Failure(AuthError.NETWORK)

        val account = synchronized(lock) { accounts[normalize(email)] }
        return if (account != null && account.password == password) {
            AuthResult.Success(account.user)
        } else {
            // Same answer for an unknown email and a wrong password, as a real server gives
            AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        }
    }

    override suspend fun signUp(request: SignUpRequest): AuthResult {
        delay(latencyMillis)
        if (simulateNetworkError) return AuthResult.Failure(AuthError.NETWORK)

        val email = normalize(request.email)
        val user = AuthUser(
            userId = UUID.randomUUID().toString(),
            role = request.role,
            displayName = request.displayName.trim(),
            email = email,
            businessName = request.businessName?.trim()?.takeIf { request.role == UserRole.MERCHANT },
            city = request.city?.trim()?.takeIf { request.role == UserRole.MERCHANT }
        )
        // Check and insert together, so two sign ups with one email cannot both win
        val added = synchronized(lock) {
            if (accounts.containsKey(email)) false else {
                accounts[email] = Account(user, request.password)
                true
            }
        }
        return if (added) {
            AuthResult.Success(user)
        } else {
            AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
        }
    }

    private fun register(account: Account) {
        accounts[normalize(account.user.email)] = account
    }

    private fun normalize(email: String) = email.trim().lowercase()

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 600L
        const val DEMO_CUSTOMER_EMAIL = "comprador@demo.co"
        const val DEMO_MERCHANT_EMAIL = "negocio@demo.co"
        const val DEMO_PASSWORD = "Demo1234"
    }
}
