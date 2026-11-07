package com.zerowasteeats.app.data.auth

import com.zerowasteeats.app.data.auth.supabase.ProfileMetadata
import com.zerowasteeats.app.data.auth.supabase.SupabaseAuth
import com.zerowasteeats.app.data.auth.supabase.SupabaseFailure
import com.zerowasteeats.app.data.auth.supabase.SupabaseResult
import com.zerowasteeats.app.data.auth.supabase.SupabaseSession
import com.zerowasteeats.app.data.auth.supabase.SupabaseUser
import com.zerowasteeats.app.data.session.InMemoryKeyValueStore
import com.zerowasteeats.app.data.session.UserRole
import com.zerowasteeats.app.data.users.CreateProfileBody
import com.zerowasteeats.app.data.users.UserProfileResponse
import com.zerowasteeats.app.data.users.UsersApi
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SupabaseAuthRepositoryTest {

    private val userId = "b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13"
    private val tokens = AuthTokens("access", "refresh", 1_900_000_000)

    private class FakeSupabase : SupabaseAuth {
        var signUpResult: SupabaseResult<SupabaseSession>? = null
        var signInResult: SupabaseResult<SupabaseSession>? = null
        var currentUserResult: SupabaseResult<SupabaseUser> =
            SupabaseResult.Failure(SupabaseFailure.NETWORK)
        var signUpMetadata: ProfileMetadata? = null
        var signUpCalls = 0
        val signedOutWith = mutableListOf<String>()

        override suspend fun signUp(email: String, password: String, metadata: ProfileMetadata) =
            signUpResult!!.also {
                signUpCalls++
                signUpMetadata = metadata
            }

        override suspend fun signIn(email: String, password: String) = signInResult!!

        override suspend fun refresh(refreshToken: String) = error("unused")

        override suspend fun signOut(accessToken: String): SupabaseResult<Unit> {
            signedOutWith += accessToken
            return SupabaseResult.Failure(SupabaseFailure.NETWORK)
        }

        override suspend fun currentUser(accessToken: String) = currentUserResult
    }

    /** The backend's users/me, with a profile per run and switchable failures. */
    private class FakeUsers : UsersApi {
        var profile: UserProfileResponse? = null
        var createFailure: Exception? = null
        var meFailure: Exception? = null
        val created = mutableListOf<CreateProfileBody>()

        override suspend fun me(): UserProfileResponse {
            meFailure?.let { throw it }
            return profile ?: throw httpError(404)
        }

        override suspend fun create(body: CreateProfileBody): UserProfileResponse {
            created += body
            createFailure?.let { throw it }
            return profile ?: UserProfileResponse(
                id = "b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13",
                email = "ana@correo.co",
                displayName = body.displayName,
                role = body.role,
                city = body.city
            ).also { profile = it }
        }
    }

    private val supabase = FakeSupabase()
    private val users = FakeUsers()
    private val tokenData = InMemoryKeyValueStore()
    private val tokenStore = PrefsTokenStore(tokenData)
    private val repository = SupabaseAuthRepository(supabase, users, tokenStore)

    private fun session(metadata: ProfileMetadata?) = SupabaseResult.Success(
        SupabaseSession(tokens, SupabaseUser(userId, "ana@correo.co", metadata))
    )

    private val merchantRequest = SignUpRequest(
        displayName = " Ana ",
        email = " ana@correo.co ",
        password = "clave1234",
        role = UserRole.MERCHANT,
        businessName = " Tienda Ana ",
        city = "Pasto"
    )

    @Test
    fun signUp_customer_createsTheAccountThenTheProfile() = runBlocking {
        supabase.signUpResult = session(null)

        val result = repository.signUp(
            SignUpRequest("Ana", "ana@correo.co", "clave1234", UserRole.CUSTOMER)
        )

        assertEquals(
            AuthResult.Success(AuthUser(userId, UserRole.CUSTOMER, "Ana", "ana@correo.co")),
            result
        )
        assertEquals(ProfileMetadata("Ana", "CUSTOMER"), supabase.signUpMetadata)
        // No business fields for a customer: the backend would reject them
        assertEquals(listOf(CreateProfileBody("Ana", "CUSTOMER")), users.created)
        assertEquals(tokens, tokenStore.load())
    }

    @Test
    fun signUp_merchant_keepsTheBusinessInTheMetadata_andSendsItToTheBackend() = runBlocking {
        supabase.signUpResult = session(null)

        val result = repository.signUp(merchantRequest) as AuthResult.Success

        assertEquals(ProfileMetadata("Ana", "MERCHANT", "Tienda Ana", "Pasto"), supabase.signUpMetadata)
        assertEquals(
            listOf(CreateProfileBody("Ana", "MERCHANT", city = "Pasto", businessName = "Tienda Ana")),
            users.created
        )
        assertEquals("Tienda Ana", result.user.businessName)
        assertEquals("Pasto", result.user.city)
    }

    @Test
    fun signUp_existingEmail_savesNothing() = runBlocking {
        supabase.signUpResult = SupabaseResult.Failure(SupabaseFailure.EMAIL_ALREADY_REGISTERED)

        val result = repository.signUp(merchantRequest)

        assertEquals(AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED), result)
        assertNull(tokenStore.load())
        assertTrue(users.created.isEmpty())
    }

    @Test
    fun signUp_weakPassword_isReported() = runBlocking {
        supabase.signUpResult = SupabaseResult.Failure(SupabaseFailure.WEAK_PASSWORD)

        assertEquals(AuthResult.Failure(AuthError.WEAK_PASSWORD), repository.signUp(merchantRequest))
    }

    @Test
    fun signUp_profileFailure_leavesTheProfileIncomplete_andTheRetryFinishesIt() = runBlocking {
        supabase.signUpResult = session(null)
        users.createFailure = IOException("offline")

        assertEquals(AuthResult.Failure(AuthError.PROFILE_INCOMPLETE), repository.signUp(merchantRequest))
        // The account exists: its tokens stay so the profile can still be created
        assertEquals(tokens, tokenStore.load())

        users.createFailure = null
        val retried = repository.completeProfile() as AuthResult.Success

        assertEquals(UserRole.MERCHANT, retried.user.role)
        assertEquals("Tienda Ana", retried.user.businessName)
        assertEquals(1, supabase.signUpCalls)
        assertEquals(users.created[0], users.created[1])
    }

    @Test
    fun completeProfile_afterARestart_usesTheMetadataSavedInSupabase() = runBlocking {
        tokenStore.save(tokens)
        supabase.currentUserResult = SupabaseResult.Success(
            SupabaseUser(userId, "ana@correo.co", ProfileMetadata("Ana", "MERCHANT", "Tienda Ana", "Pasto"))
        )

        val result = SupabaseAuthRepository(supabase, users, tokenStore).completeProfile()

        assertTrue(result is AuthResult.Success)
        assertEquals(
            CreateProfileBody("Ana", "MERCHANT", city = "Pasto", businessName = "Tienda Ana"),
            users.created.single()
        )
    }

    @Test
    fun completeProfile_stillFailing_staysIncomplete() = runBlocking {
        supabase.signUpResult = session(null)
        users.createFailure = HttpException(Response.error<Any>(503, "".toResponseBody()))
        repository.signUp(merchantRequest)

        assertEquals(AuthResult.Failure(AuthError.PROFILE_INCOMPLETE), repository.completeProfile())
    }

    @Test
    fun signIn_withAProfile_returnsTheBackendIdentity() = runBlocking {
        supabase.signInResult = session(ProfileMetadata("Ana", "MERCHANT", "Tienda Ana", "Pasto"))
        users.profile = UserProfileResponse(userId, "ana@correo.co", "Ana", "MERCHANT", "Pasto")

        val result = repository.signIn(" ana@correo.co ", "clave1234")

        assertEquals(
            AuthResult.Success(
                AuthUser(userId, UserRole.MERCHANT, "Ana", "ana@correo.co", "Tienda Ana", "Pasto")
            ),
            result
        )
        assertTrue(users.created.isEmpty())
        assertEquals(tokens, tokenStore.load())
    }

    @Test
    fun signIn_trustsTheBackendRole_overTheMetadata() = runBlocking {
        supabase.signInResult = session(ProfileMetadata("Ana", "MERCHANT", "Tienda Ana"))
        users.profile = UserProfileResponse(userId, "ana@correo.co", "Ana", "CUSTOMER")

        val result = repository.signIn("ana@correo.co", "clave1234") as AuthResult.Success

        assertEquals(UserRole.CUSTOMER, result.user.role)
        assertNull(result.user.businessName)
    }

    @Test
    fun signIn_withoutAProfile_createsItFromTheSavedMetadata() = runBlocking {
        supabase.signInResult = session(ProfileMetadata("Ana", "MERCHANT", "Tienda Ana", "Pasto"))

        val result = repository.signIn("ana@correo.co", "clave1234") as AuthResult.Success

        assertEquals(
            listOf(CreateProfileBody("Ana", "MERCHANT", city = "Pasto", businessName = "Tienda Ana")),
            users.created
        )
        assertEquals(UserRole.MERCHANT, result.user.role)
    }

    @Test
    fun signIn_withoutProfileOrMetadata_createsACustomerNamedAfterTheEmail() = runBlocking {
        supabase.signInResult = session(null)

        repository.signIn("ana@correo.co", "clave1234")

        assertEquals(listOf(CreateProfileBody("ana", "CUSTOMER")), users.created)
    }

    @Test
    fun signIn_whenTheBackendIsUnreachable_dropsTheTokens() = runBlocking {
        supabase.signInResult = session(null)
        users.meFailure = IOException("offline")

        assertEquals(AuthResult.Failure(AuthError.NETWORK), repository.signIn("ana@correo.co", "x"))
        assertNull(tokenStore.load())
    }

    @Test
    fun signIn_whenCreatingTheMissingProfileFails_dropsTheTokens() = runBlocking {
        supabase.signInResult = session(ProfileMetadata("Ana", "CUSTOMER"))
        users.createFailure = HttpException(Response.error<Any>(409, "".toResponseBody()))

        assertEquals(AuthResult.Failure(AuthError.UNEXPECTED), repository.signIn("ana@correo.co", "x"))
        assertNull(tokenStore.load())
    }

    @Test
    fun signIn_wrongPassword_isReported_andSavesNothing() = runBlocking {
        supabase.signInResult = SupabaseResult.Failure(SupabaseFailure.INVALID_CREDENTIALS)

        assertEquals(
            AuthResult.Failure(AuthError.INVALID_CREDENTIALS),
            repository.signIn("ana@correo.co", "mala")
        )
        assertNull(tokenStore.load())
    }

    @Test
    fun signOut_endsTheServerSession_andToleratesAFailure() = runBlocking {
        repository.signOut("access")

        assertEquals(listOf("access"), supabase.signedOutWith)
    }

    @Test
    fun signOut_withoutAToken_callsNothing() = runBlocking {
        repository.signOut(null)

        assertTrue(supabase.signedOutWith.isEmpty())
    }

    private companion object {
        fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
    }
}
