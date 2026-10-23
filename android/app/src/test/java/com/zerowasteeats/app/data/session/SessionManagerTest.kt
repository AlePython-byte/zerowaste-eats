package com.zerowasteeats.app.data.session

import com.zerowasteeats.app.data.auth.AuthError
import com.zerowasteeats.app.data.auth.AuthResult
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.auth.SignUpRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private val data = InMemoryKeyValueStore()
    private val repository = FakeAuthRepository(latencyMillis = 0)

    private fun manager(restored: Boolean = true) = SessionManager(repository, PrefsSessionStore(data)).also {
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
            Session.Merchant("demo-merchant", "Negocio Demo", "negocio@demo.co", "Panadería Demo"),
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
    fun signingInAsAnotherRole_replacesTheSession() = runBlocking {
        val manager = manager()
        manager.signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, demoPassword)

        manager.signOut()
        manager.signIn(FakeAuthRepository.DEMO_MERCHANT_EMAIL, demoPassword)

        assertTrue(manager.session.value is Session.Merchant)
        assertEquals(Session.Merchant("demo-merchant", "Negocio Demo", "negocio@demo.co", "Panadería Demo"), manager().session.value)
    }
}
