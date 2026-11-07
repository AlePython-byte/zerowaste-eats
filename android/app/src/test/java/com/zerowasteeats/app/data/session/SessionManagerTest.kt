package com.zerowasteeats.app.data.session

import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.AuthTokens
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.auth.PrefsTokenStore
import com.zerowasteeats.app.data.auth.SignUpRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private val data = InMemoryKeyValueStore()
    private val repository = FakeAuthRepository(latencyMillis = 0)
    private val tokenData = InMemoryKeyValueStore()
    private val tokens = PrefsTokenStore(tokenData)
    private val someTokens = AuthTokens("access", "refresh", 1_900_000_000)

    private fun manager(restored: Boolean = true) = SessionManager(repository, PrefsSessionStore(data), tokens).also {
        if (restored) it.restore()
    }

    private val demoPassword = FakeAuthRepository.DEMO_PASSWORD

    @Test
    fun session_isLoadingUntilRestored() {
        val manager = manager(restored = false)

        assertEquals(Session.Loading, manager.session.value)

        manager.restore()

        assertEquals(Session.SignedOut, manager.session.value)
    }

    @Test
    fun restore_picksUpTheSavedSession() {
        PrefsSessionStore(data).save(Session.Merchant("u2", "Luis", "l@t.co", "Tienda Luis"))

        assertEquals(Session.Merchant("u2", "Luis", "l@t.co", "Tienda Luis"), manager().session.value)
    }

    @Test
    fun signIn_customerOpensTheCustomerSession() = runBlocking {
        val manager = manager()

        val result = manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)

        assertTrue(result is AuthResult.Success)
        assertEquals(
            Session.Customer("demo-customer", "Comprador Demo", "comprador@demo.co"),
            manager.session.value
        )
    }

    @Test
    fun signIn_merchantOpensTheMerchantSession() = runBlocking {
        val manager = manager()

        manager.signIn(FakeAuthRepository.DEMO_MERCHANT_EMAIL, demoPassword)

        assertEquals(
            Session.Merchant("demo-merchant", "Negocio Demo", "negocio@demo.co", "Panadería Demo", "Pasto"),
            manager.session.value
        )
    }

    @Test
    fun signIn_failureKeepsTheSessionAndSavesNothing() = runBlocking {
        val manager = manager()

        val result = manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, "mala")

        assertEquals(AuthResult.Failure(AuthError.INVALID_CREDENTIALS), result)
        assertEquals(Session.SignedOut, manager.session.value)
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun signIn_networkFailureKeepsTheSessionAndSavesNothing() = runBlocking {
        repository.simulateNetworkError = true
        val manager = manager()

        val result = manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)

        assertEquals(AuthResult.Failure(AuthError.NETWORK), result)
        assertEquals(Session.SignedOut, manager.session.value)
        assertTrue(data.contents.isEmpty())
    }

    @Test
    fun signIn_isRememberedByTheNextManager() = runBlocking {
        manager().signIn(FakeAuthRepository.DEMO_MERCHANT_EMAIL, demoPassword)

        // A new launch: new manager, same saved data
        assertTrue(manager().session.value is Session.Merchant)
    }

    @Test
    fun signUp_customerStartsASessionRightAway() = runBlocking {
        val manager = manager()

        manager.signUp(SignUpRequest("Ana", "ana@correo.co", "clave1234", UserRole.CUSTOMER))

        val session = manager.session.value as Session.Customer
        assertEquals("Ana", session.displayName)
        assertEquals("ana@correo.co", session.email)
    }

    @Test
    fun signUp_merchantStartsAMerchantSession() = runBlocking {
        val manager = manager()

        manager.signUp(
            SignUpRequest("Luis", "luis@tienda.co", "clave1234", UserRole.MERCHANT, "Tienda Luis", "Pasto")
        )

        assertEquals("Tienda Luis", (manager.session.value as Session.Merchant).businessName)
    }

    @Test
    fun signUp_merchantKeepsTheCityInTheSession_andAfterARestart() = runBlocking {
        val data = InMemoryKeyValueStore()
        val first = SessionManager(FakeAuthRepository(latencyMillis = 0), PrefsSessionStore(data), PrefsTokenStore(InMemoryKeyValueStore()))

        first.signUp(
            SignUpRequest("Luis", "luis@tienda.co", "clave1234", UserRole.MERCHANT, "Tienda Luis", " Pasto ")
        )

        assertEquals("Pasto", (first.session.value as Session.Merchant).city)

        val restarted = SessionManager(FakeAuthRepository(latencyMillis = 0), PrefsSessionStore(data), PrefsTokenStore(InMemoryKeyValueStore()))
        restarted.restore()
        assertEquals("Pasto", (restarted.session.value as Session.Merchant).city)
    }

    @Test
    fun signUp_repeatedEmailFailsWithoutChangingTheSession() = runBlocking {
        val manager = manager()

        val result = manager.signUp(
            SignUpRequest("Otro", FakeAuthRepository.DEMO_CUSTOMER_EMAIL, "clave1234", UserRole.CUSTOMER)
        )

        assertEquals(AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED), result)
        assertEquals(Session.SignedOut, manager.session.value)
    }

    @Test
    fun signOut_clearsTheSessionAndWhatWasSaved() = runBlocking {
        val manager = manager()
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)

        manager.signOut()

        assertEquals(Session.SignedOut, manager.session.value)
        assertTrue(data.contents.isEmpty())
        assertEquals(Session.SignedOut, manager().session.value)
    }

    @Test
    fun expireSession_dropsSessionAndTokens_andLeavesANotice() = runBlocking {
        val manager = manager()
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)
        tokens.save(someTokens)

        manager.expireSession()

        assertEquals(Session.SignedOut, manager.session.value)
        assertEquals(SessionNotice.SESSION_EXPIRED, manager.notice.value)
        assertNull(tokens.load())
        assertTrue(data.contents.isEmpty())
        // Nothing comes back on the next launch
        assertEquals(Session.SignedOut, manager().session.value)
    }

    @Test
    fun expireSession_whenAlreadySignedOut_leavesNoNotice() {
        val manager = manager()

        manager.expireSession()

        assertNull(manager.notice.value)
    }

    @Test
    fun theNotice_goesAwayWhenDismissed_orOnTheNextSignIn() = runBlocking {
        val manager = manager()
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)
        manager.expireSession()

        manager.dismissNotice()
        assertNull(manager.notice.value)

        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)
        manager.expireSession()
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)
        assertNull(manager.notice.value)
    }

    @Test
    fun restore_withoutASession_dropsLeftoverTokens() {
        tokens.save(someTokens)

        manager()

        assertNull(tokens.load())
    }

    @Test
    fun restore_withASession_keepsItsTokens() {
        PrefsSessionStore(data).save(Session.Customer("u1", "Ana", "a@b.co"))
        tokens.save(someTokens)

        manager()

        assertEquals(someTokens, tokens.load())
    }

    @Test
    fun signingInAsAnotherRole_replacesTheSession() = runBlocking {
        val manager = manager()
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)

        manager.signOut()
        manager.signIn(FakeAuthRepository.DEMO_MERCHANT_EMAIL, demoPassword)

        assertTrue(manager.session.value is Session.Merchant)
        assertEquals(
            Session.Merchant("demo-merchant", "Negocio Demo", "negocio@demo.co", "Panadería Demo", "Pasto"),
            manager().session.value
        )
    }
}
