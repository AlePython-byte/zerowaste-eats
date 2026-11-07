package com.zerowasteeats.app.data.session

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.AuthUser
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.auth.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    /** Asked on every call, so switching between live and demo data takes effect at once. */
    private val authRepositoryProvider: () -> AuthRepository,
    private val sessionStore: SessionStore,
    private val tokenStore: TokenStore,
    /** Runs the server side of a sign out after the local one; outlives any screen. */
    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    constructor(
        authRepository: AuthRepository,
        sessionStore: SessionStore,
        tokenStore: TokenStore,
        backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) : this({ authRepository }, sessionStore, tokenStore, backgroundScope)

    private val authRepository: AuthRepository get() = authRepositoryProvider()

    /** True while a customer or a business is signed in. */
    val isSignedIn: Boolean
        get() = sessionState.value.let { it is Session.Customer || it is Session.Merchant }

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

    /** Retries a sign up that ended in [com.zerowasteeats.app.data.auth.AuthError.PROFILE_INCOMPLETE]. */
    suspend fun completeProfile(): AuthResult =
        authRepository.completeProfile().also(::startSessionIfSuccessful)

    /**
     * Forgets the session and its tokens right away, then tells the server in the background.
     * Without network the person is still signed out on this device.
     */
    fun signOut() {
        val accessToken = tokenStore.load()?.accessToken
        // The repository that holds this session, even if the mode changes right after
        val repository = authRepository
        tokenStore.clear()
        sessionStore.clear()
        sessionState.value = Session.SignedOut
        backgroundScope.launch {
            try {
                repository.signOut(accessToken)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best effort: the session is already gone here, and expires on the server
            }
        }
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
