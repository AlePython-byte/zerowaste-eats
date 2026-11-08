package com.zerowasteeats.app.data.remote

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpClientsTest {

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun callsTheBackendAndDecodesItsJson() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""{"estado":"ok","servicio":"ZeroWaste Eats API","version":"1.0.0","extra":1}""")
                .build()
        )
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(HealthApi::class.java)

        val health = api.health()

        assertEquals(HealthResponse("ok", "ZeroWaste Eats API", "1.0.0"), health)
        assertEquals("/api/v1/salud", server.takeRequest().url.encodedPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsABaseUrlWithoutTheTrailingSlash() {
        createRetrofit("http://localhost:3000/api/v1", createHttpClient())
    }

    @Test
    fun aConfigIsCompleteOnlyWithEveryValue() {
        val complete = ApiConfig("http://10.0.2.2:3000/api/v1/", "https://x.supabase.co", "key")

        assertTrue(complete.isComplete)
        assertFalse(complete.copy(apiBaseUrl = "").isComplete)
        assertFalse(complete.copy(supabaseUrl = " ").isComplete)
        assertFalse(complete.copy(supabasePublishableKey = "").isComplete)
    }
}
