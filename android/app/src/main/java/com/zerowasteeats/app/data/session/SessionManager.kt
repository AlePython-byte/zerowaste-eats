package com.zerowasteeats.app.data.session

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.AuthUser
import com.zerowasteeats.app.data.auth.SignUpRequest

/**
 * Holds the current [Session] for the whole app. Call [restore] once at startup; until then
 * the session is [Session.Loading]. A successful [signIn] or [signUp] saves the session, and
 * [signOut] erases it, so what the person sees on the next launch matches the last state.
 */
class SessionManager(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore
) {
    private val sessionState = mutableStateOf<Session>(Session.Loading)

    val session: State<Session> get() = sessionState

    /** Reads the saved session. Synchronous, so it can run before the first frame. */
    fun restore() {
        sessionState.value = sessionStore.load()
    }

    suspend fun signIn(email: String, password: String): AuthResult =
        authRepository.signIn(email, password).also(::startSessionIfSuccessful)

    suspend fun signUp(request: SignUpRequest): AuthResult =
        authRepository.signUp(request).also(::startSessionIfSuccessful)

    fun signOut() {
        sessionStore.clear()
        sessionState.value = Session.SignedOut
    }

    private fun startSessionIfSuccessful(result: AuthResult) {
        if (result !is AuthResult.Success) return
        val session = result.user.toSession()
        sessionStore.save(session)
        sessionState.value = session
    }

    private fun AuthUser.toSession(): Session = when (role) {
        UserRole.CUSTOMER -> Session.Customer(userId, displayName, email)
        UserRole.MERCHANT -> Session.Merchant(
            userId = userId,
            displayName = displayName,
            email = email,
            businessName = businessName.orEmpty()
        )
    }
}
