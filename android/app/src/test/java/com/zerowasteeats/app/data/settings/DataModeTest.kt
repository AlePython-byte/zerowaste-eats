package com.zerowasteeats.app.data.settings

import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.auth.PrefsTokenStore
import com.zerowasteeats.app.data.auth.SignUpRequest
import com.zerowasteeats.app.data.session.InMemoryKeyValueStore
import com.zerowasteeats.app.data.session.PrefsSessionStore
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.data.session.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModeTest {

    private val prefs = InMemoryKeyValueStore()
    private var signedIn = false
    private val changes = mutableListOf<DataMode>()

    private fun controller(liveAvailable: Boolean = true) = DataModeController(
        store = DataModeStore(prefs),
        liveAvailable = liveAvailable,
        isSignedIn = { signedIn },
        onChanged = { changes += it }
    )

    @Test
    fun startsLive_whenTheEndpointsExist_andNothingWasChosen() {
        assertEquals(DataMode.LIVE, controller().mode.value)
    }

    @Test
    fun withoutEndpoints_onlyTheDemoExists_andTheSwitchIsLocked() {
        DataModeStore(prefs).save(DataMode.LIVE)
        val controller = controller(liveAvailable = false)

        assertEquals(DataMode.DEMO, controller.mode.value)
        assertFalse(controller.canChange)
        assertFalse(controller.setMode(DataMode.LIVE))
        assertEquals(DataMode.DEMO, controller.mode.value)
    }

    @Test
    fun switching_isRemembered_andRebuildsOnce() {
        assertTrue(controller().setMode(DataMode.DEMO))

        assertEquals(listOf(DataMode.DEMO), changes)
        // The next launch starts in the chosen mode
        assertEquals(DataMode.DEMO, controller().mode.value)
    }

    @Test
    fun switching_isRefusedWhileSignedIn() {
        val controller = controller()
        signedIn = true

        assertFalse(controller.canChange)
        assertFalse(controller.setMode(DataMode.DEMO))
        assertEquals(DataMode.LIVE, controller.mode.value)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun choosingTheCurrentMode_changesNothing() {
        assertTrue(controller().setMode(DataMode.LIVE))

        assertTrue(changes.isEmpty())
    }

    @Test
    fun anUnreadableSavedMode_fallsBackToLive() {
        val garbage = InMemoryKeyValueStore(mapOf("mode" to "SOMETHING"))

        assertEquals(null, DataModeStore(garbage).load())
    }

    /** A live repository that is unreachable, to tell it apart from the demo one. */
    private class UnreachableLive : AuthRepository {
        override suspend fun signIn(email: String, password: String) =
            AuthResult.Failure(AuthError.NETWORK)

        override suspend fun signUp(request: SignUpRequest) = AuthResult.Failure(AuthError.NETWORK)
    }

    @Test
    fun theDemoAccounts_workInDemoMode_throughTheSameSessionManager() = runBlocking {
        val live = UnreachableLive()
        val demo = FakeAuthRepository(latencyMillis = 0)
        var current: AuthRepository = live
        val sessionData = InMemoryKeyValueStore()
        val manager = SessionManager(
            { current },
            PrefsSessionStore(sessionData),
            PrefsTokenStore(InMemoryKeyValueStore())
        ).also { it.restore() }
        val controller = DataModeController(
            store = DataModeStore(prefs),
            liveAvailable = true,
            isSignedIn = { manager.isSignedIn },
            onChanged = { mode -> current = if (mode == DataMode.DEMO) demo else live }
        )

        assertEquals(
            AuthResult.Failure(AuthError.NETWORK),
            manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        )

        controller.setMode(DataMode.DEMO)
        manager.signIn(FakeAuthRepository.DEMO_MERCHANT_EMAIL, FakeAuthRepository.DEMO_PASSWORD)

        assertTrue(manager.session.value is Session.Merchant)
        // Signed in: the mode is locked until signing out
        assertFalse(controller.setMode(DataMode.LIVE))

        manager.signOut()
        assertTrue(controller.setMode(DataMode.LIVE))
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        assertEquals(Session.SignedOut, manager.session.value)
    }
}
