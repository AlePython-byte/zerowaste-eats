package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.session.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepositoryTest {

    private fun repository() = FakeAuthRepository(latencyMillis = 0)

    private fun success(result: AuthResult) = (result as AuthResult.Success).user

    private fun failure(result: AuthResult) = (result as AuthResult.Failure).error

    @Test
    fun signIn_demoCustomer() = runBlocking {
        val user = success(
            repository().signIn(FakeAuthRepository.DEMO_CUSTOMER_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        )

        assertEquals(UserRole.CUSTOMER, user.role)
        assertEquals("comprador@demo.co", user.email)
        assertNull(user.businessName)
    }

    @Test
    fun signIn_demoMerchantHasBusinessName() = runBlocking {
        val user = success(
            repository().signIn(FakeAuthRepository.DEMO_MERCHANT_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        )

        assertEquals(UserRole.MERCHANT, user.role)
        assertEquals("Panadería Demo", user.businessName)
    }

    @Test
    fun signIn_ignoresEmailCaseAndSpaces() = runBlocking {
        val result = repository().signIn("  Comprador@DEMO.co ", "Demo1234")

        assertEquals("comprador@demo.co", success(result).email)
    }

    @Test
    fun signIn_wrongPassword() = runBlocking {
        val result = repository().signIn("comprador@demo.co", "demo1234")

        assertEquals(AuthError.INVALID_CREDENTIALS, failure(result))
    }

    @Test
    fun signIn_unknownEmailLooksLikeWrongPassword() = runBlocking {
        val result = repository().signIn("nadie@demo.co", "Demo1234")

        assertEquals(AuthError.INVALID_CREDENTIALS, failure(result))
    }

    @Test
    fun signUp_customerCanThenSignIn() = runBlocking {
        val repository = repository()

        val created = success(
            repository.signUp(
                SignUpRequest("  Ana Pérez ", "Ana@Correo.co", "clave1234", UserRole.CUSTOMER)
            )
        )
        val signedIn = success(repository.signIn("ana@correo.co", "clave1234"))

        assertEquals("Ana Pérez", created.displayName)
        assertEquals("ana@correo.co", created.email)
        assertEquals(UserRole.CUSTOMER, created.role)
        assertEquals(created, signedIn)
    }

    @Test
    fun signUp_merchantKeepsBusinessName() = runBlocking {
        val user = success(
            repository().signUp(
                SignUpRequest(
                    "Luis", "luis@tienda.co", "clave1234", UserRole.MERCHANT,
                    businessName = " Tienda Luis ", city = "Pasto"
                )
            )
        )

        assertEquals(UserRole.MERCHANT, user.role)
        assertEquals("Tienda Luis", user.businessName)
    }

    @Test
    fun signUp_customerDropsBusinessName() = runBlocking {
        val user = success(
            repository().signUp(
                SignUpRequest("Ana", "ana@correo.co", "clave1234", UserRole.CUSTOMER, businessName = "Algo")
            )
        )

        assertNull(user.businessName)
    }

    @Test
    fun signUp_newAccountsGetDistinctIds() = runBlocking {
        val repository = repository()

        val first = success(repository.signUp(SignUpRequest("Ana", "a@correo.co", "clave1234", UserRole.CUSTOMER)))
        val second = success(repository.signUp(SignUpRequest("Bea", "b@correo.co", "clave1234", UserRole.CUSTOMER)))

        assertNotEquals(first.userId, second.userId)
    }

    @Test
    fun signUp_rejectsRegisteredEmail() = runBlocking {
        val repository = repository()

        val demo = repository.signUp(
            SignUpRequest("Otro", "COMPRADOR@demo.co", "clave1234", UserRole.CUSTOMER)
        )
        repository.signUp(SignUpRequest("Ana", "ana@correo.co", "clave1234", UserRole.CUSTOMER))
        val repeated = repository.signUp(
            SignUpRequest("Ana 2", "ana@correo.co", "otraClave1", UserRole.CUSTOMER)
        )

        assertEquals(AuthError.EMAIL_ALREADY_REGISTERED, failure(demo))
        assertEquals(AuthError.EMAIL_ALREADY_REGISTERED, failure(repeated))
        // The first password still works: the failed sign up changed nothing
        assertTrue(repository.signIn("ana@correo.co", "clave1234") is AuthResult.Success)
        assertEquals(AuthError.INVALID_CREDENTIALS, failure(repository.signIn("ana@correo.co", "otraClave1")))
    }

    @Test
    fun networkError_failsSignInAndSignUp() = runBlocking {
        val repository = repository()
        repository.simulateNetworkError = true

        val signIn = repository.signIn("comprador@demo.co", "Demo1234")
        val signUp = repository.signUp(SignUpRequest("Ana", "ana@correo.co", "clave1234", UserRole.CUSTOMER))

        assertEquals(AuthError.NETWORK, failure(signIn))
        assertEquals(AuthError.NETWORK, failure(signUp))
    }

    @Test
    fun networkError_signUpDoesNotCreateTheAccount() = runBlocking {
        val repository = repository()
        repository.simulateNetworkError = true
        repository.signUp(SignUpRequest("Ana", "ana@correo.co", "clave1234", UserRole.CUSTOMER))
        repository.simulateNetworkError = false

        assertEquals(AuthError.INVALID_CREDENTIALS, failure(repository.signIn("ana@correo.co", "clave1234")))
    }

    @Test
    fun networkError_canBeTurnedOffAgain() = runBlocking {
        val repository = repository()
        repository.simulateNetworkError = true
        assertFalse(repository.signIn("comprador@demo.co", "Demo1234") is AuthResult.Success)

        repository.simulateNetworkError = false

        assertTrue(repository.signIn("comprador@demo.co", "Demo1234") is AuthResult.Success)
    }

    @Test
    fun latency_isAboutSixHundredMillisecondsByDefault() = runBlocking {
        val start = System.nanoTime()

        FakeAuthRepository().signIn("comprador@demo.co", "Demo1234")

        val elapsedMillis = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $elapsedMillis ms", elapsedMillis in 550..1500)
    }

    @Test(expected = IllegalArgumentException::class)
    fun signUpRequest_merchantWithoutBusinessNameIsRejected() {
        SignUpRequest("Luis", "luis@tienda.co", "clave1234", UserRole.MERCHANT, businessName = "  ")
    }
}
