package com.zerowasteeats.app.data.session

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.AuthUser
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.auth.TokenStore

/** Something to tell the person on the welcome screen after they were signed out. */
enum class SessionNotice {
    /** The session could not be renewed: they have to sign in again. */
    SESSION_EXPIRED
}

/**
 * Holds the current [Session] for the whole app. Call [restore] once at startup; until then
 * the session is [Session.Loading]. A successful [signIn] or [signUp] saves the session, and
 * [signOut] erases it, so what the person sees on the next launch matches the last state.
 */
class SessionManager(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val tokenStore: TokenStore
) {
    private val sessionState = mutableStateOf<Session>(Session.Loading)
    private val noticeState = mutableStateOf<SessionNotice?>(null)

    val session: State<Session> get() = sessionState

    /** Shown once on the welcome screen, then [dismissNotice]. */
    val notice: State<SessionNotice?> get() = noticeState

    /** Reads the saved session. Synchronous, so it can run before the first frame. */
    fun restore() {
        val saved = sessionStore.load()
        // Tokens without a session (a sign up left halfway, say) must not outlive it
        if (saved == Session.SignedOut) tokenStore.clear()
        sessionState.value = saved
    }

    suspend fun signIn(email: String, password: String): AuthResult =
        authRepository.signIn(email, password).also(::startSessionIfSuccessful)

    suspend fun signUp(request: SignUpRequest): AuthResult =
        authRepository.signUp(request).also(::startSessionIfSuccessful)

    fun signOut() {
        sessionStore.clear()
        sessionState.value = Session.SignedOut
    }

    /**
     * The tokens can no longer be renewed: drop everything and send the person back to the
     * welcome screen with [SessionNotice.SESSION_EXPIRED]. Safe to call from any thread, and
     * more than once (only a signed-in session is ended).
     */
    fun expireSession() {
        synchronized(this) {
            tokenStore.clear()
            if (sessionState.value !is Session.Customer && sessionState.value !is Session.Merchant) {
                return
            }
            sessionStore.clear()
            noticeState.value = SessionNotice.SESSION_EXPIRED
            sessionState.value = Session.SignedOut
        }
    }

    fun dismissNotice() {
        noticeState.value = null
    }

    private fun startSessionIfSuccessful(result: AuthResult) {
        if (result !is AuthResult.Success) return
        val session = result.user.toSession()
        sessionStore.save(session)
        noticeState.value = null
        sessionState.value = session
    }

    private fun AuthUser.toSession(): Session = when (role) {
        UserRole.CUSTOMER -> Session.Customer(userId, displayName, email)
        UserRole.MERCHANT -> Session.Merchant(
            userId = userId,
            displayName = displayName,
            email = email,
            businessName = businessName.orEmpty(),
            city = city.orEmpty()
        )
    }
}
