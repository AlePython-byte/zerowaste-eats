package com.zerowasteeats.app.data.auth.supabase

import com.zerowasteeats.app.data.auth.AuthTokens
import com.zerowasteeats.app.data.remote.createHttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SupabaseAuthClientTest {

    private val server = MockWebServer()
    private lateinit var client: SupabaseAuthClient

    @Before
    fun setUp() {
        server.start()
        // The project URL as configured, without the auth path; the client adds /auth/v1/
        client = SupabaseAuthClient.create(
            supabaseUrl = server.url("/").toString().trimEnd('/'),
            publishableKey = "publishable-test-key",
            baseClient = createHttpClient()
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(status: Int, body: String) = server.enqueue(
        MockResponse.Builder()
            .code(status)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    )

    private val sessionJson = """
        {"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600,
         "expires_at":1900000000,"token_type":"bearer",
         "user":{"id":"b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13","email":"ana@correo.co",
                 "user_metadata":{"display_name":"Ana","role":"MERCHANT","business_name":"Tienda Ana","city":"Pasto"}}}
    """.trimIndent()

    private val expectedSession = SupabaseSession(
        AuthTokens("new-access", "new-refresh", 1_900_000_000),
        SupabaseUser(
            id = "b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13",
            email = "ana@correo.co",
            metadata = ProfileMetadata("Ana", "MERCHANT", "Tienda Ana", "Pasto")
        )
    )

    @Test
    fun signUp_sendsTheMetadataAndThePublishableKey_andReturnsTheSession() = runBlocking {
        respond(200, sessionJson)

        val result = client.signUp(
            "ana@correo.co",
            "clave1234",
            ProfileMetadata("Ana", "MERCHANT", "Tienda Ana", "Pasto")
        )

        assertEquals(SupabaseResult.Success(expectedSession), result)
        val request = server.takeRequest()
        assertEquals("/auth/v1/signup", request.url.encodedPath)
        assertEquals("publishable-test-key", request.headers["apikey"])
        assertNull(request.headers["Authorization"])
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("ana@correo.co", body["email"]!!.jsonPrimitive.content)
        val data = body["data"]!!.jsonObject
        assertEquals("Ana", data["display_name"]!!.jsonPrimitive.content)
        assertEquals("MERCHANT", data["role"]!!.jsonPrimitive.content)
        assertEquals("Tienda Ana", data["business_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun signUp_omitsMissingMetadataInsteadOfSendingNulls() = runBlocking {
        respond(200, sessionJson)

        client.signUp("ana@correo.co", "clave1234", ProfileMetadata("Ana", "CUSTOMER"))

        val data = Json.parseToJsonElement(server.takeRequest().body!!.utf8())
            .jsonObject["data"]!!.jsonObject
        assertEquals(setOf("display_name", "role"), data.keys)
    }

    @Test
    fun signUp_existingEmail_isReportedAsSuch() = runBlocking {
        respond(422, """{"code":422,"error_code":"user_already_exists","msg":"User already registered"}""")

        val result = client.signUp("ana@correo.co", "clave1234", ProfileMetadata("Ana", "CUSTOMER"))

        assertEquals(SupabaseResult.Failure(SupabaseFailure.EMAIL_ALREADY_REGISTERED), result)
    }

    @Test
    fun signUp_weakPassword_isReportedAsSuch() = runBlocking {
        respond(422, """{"code":422,"error_code":"weak_password","msg":"Password is too weak"}""")

        val result = client.signUp("ana@correo.co", "clave1234", ProfileMetadata("Ana", "CUSTOMER"))

        assertEquals(SupabaseResult.Failure(SupabaseFailure.WEAK_PASSWORD), result)
    }

    @Test
    fun signUp_withoutASession_meansTheEmailMustBeConfirmed() = runBlocking {
        // With "Confirm email" on, Supabase answers with the user only
        respond(200, """{"id":"b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13","email":"ana@correo.co"}""")

        val result = client.signUp("ana@correo.co", "clave1234", ProfileMetadata("Ana", "CUSTOMER"))

        assertEquals(SupabaseResult.Failure(SupabaseFailure.NO_SESSION), result)
    }

    @Test
    fun signIn_usesThePasswordGrant() = runBlocking {
        respond(200, sessionJson)

        val result = client.signIn("ana@correo.co", "clave1234")

        assertEquals(SupabaseResult.Success(expectedSession), result)
        val request = server.takeRequest()
        assertEquals("/auth/v1/token", request.url.encodedPath)
        assertEquals("password", request.url.queryParameter("grant_type"))
    }

    @Test
    fun signIn_wrongPassword_isInvalidCredentials() = runBlocking {
        respond(
            400,
            """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""
        )

        assertEquals(
            SupabaseResult.Failure(SupabaseFailure.INVALID_CREDENTIALS),
            client.signIn("ana@correo.co", "mala")
        )
    }

    @Test
    fun refresh_sendsTheRefreshToken_andReturnsNewTokens() = runBlocking {
        respond(200, sessionJson)

        val result = client.refresh("old-refresh")

        assertEquals(SupabaseResult.Success(expectedSession), result)
        val request = server.takeRequest()
        assertEquals("refresh_token", request.url.queryParameter("grant_type"))
        assertEquals(
            "old-refresh",
            Json.parseToJsonElement(request.body!!.utf8()).jsonObject["refresh_token"]!!
                .jsonPrimitive.content
        )
    }

    @Test
    fun refresh_rejectedToken_meansSignInAgain() = runBlocking {
        respond(400, """{"code":400,"error_code":"refresh_token_already_used","msg":"Invalid Refresh Token"}""")

        assertEquals(
            SupabaseResult.Failure(SupabaseFailure.SESSION_REJECTED),
            client.refresh("old-refresh")
        )
    }

    @Test
    fun refresh_serverTrouble_isTemporary_notARejection() = runBlocking {
        respond(503, "<html>maintenance</html>")

        assertEquals(SupabaseResult.Failure(SupabaseFailure.NETWORK), client.refresh("old-refresh"))
    }

    @Test
    fun noAnswer_isANetworkFailure() = runBlocking {
        server.close()

        assertEquals(
            SupabaseResult.Failure(SupabaseFailure.NETWORK),
            client.signIn("ana@correo.co", "clave1234")
        )
    }

    @Test
    fun signOut_sendsTheAccessTokenAsBearer() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())

        assertEquals(SupabaseResult.Success(Unit), client.signOut("current-access"))
        val request = server.takeRequest()
        assertEquals("/auth/v1/logout", request.url.encodedPath)
        assertEquals("Bearer current-access", request.headers["Authorization"])
    }

    @Test
    fun signOut_ofAnAlreadyEndedSession_stillSucceeds() = runBlocking {
        respond(401, """{"code":401,"error_code":"session_not_found"}""")

        assertEquals(SupabaseResult.Success(Unit), client.signOut("expired-access"))
    }

    @Test
    fun currentUser_readsTheSavedMetadata() = runBlocking {
        respond(
            200,
            """{"id":"u1","email":"ana@correo.co","user_metadata":{"display_name":"Ana","role":"CUSTOMER"}}"""
        )

        assertEquals(
            SupabaseResult.Success(SupabaseUser("u1", "ana@correo.co", ProfileMetadata("Ana", "CUSTOMER"))),
            client.currentUser("current-access")
        )
        assertEquals("Bearer current-access", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun authBaseUrl_toleratesATrailingSlash() {
        assertEquals(
            "https://x.supabase.co/auth/v1/",
            SupabaseAuthClient.authBaseUrl("https://x.supabase.co/")
        )
    }
}
