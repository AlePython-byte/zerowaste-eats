package com.zerowasteeats.app.data.remote

import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerWakeUpTest {

    private val server = MockWebServer()
    private var now = 1_000L

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    /** A client whose usual read timeout is one second, as short as the test can afford. */
    private fun client(wakeUp: ServerWakeUp) = OkHttpClient.Builder()
        .readTimeout(1, TimeUnit.SECONDS)
        .addInterceptor(wakeUp)
        .build()

    private fun get(client: OkHttpClient, path: String = "/api/v1/salud") =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().use { it.code }

    private fun answer(code: Int = 200, delayMillis: Long = 0) = server.enqueue(
        MockResponse.Builder()
            .code(code)
            .body("{}")
            .headersDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
    )

    @Test
    fun theFirstCall_getsTheLongTimeout_whileTheServerWakesUp() {
        val wakeUp = ServerWakeUp(server.hostName, firstCallTimeoutSeconds = 5) { now }
        answer(delayMillis = 2_000)

        // Twice the usual read timeout, and still answered
        assertEquals(200, get(client(wakeUp)))
    }

    @Test
    fun onceAwake_callsUseTheUsualTimeout() {
        val wakeUp = ServerWakeUp(server.hostName, firstCallTimeoutSeconds = 5) { now }
        val client = client(wakeUp)
        answer()
        get(client)

        answer(delayMillis = 2_000)
        val failed = runCatching { get(client) }.exceptionOrNull()

        assertTrue(failed.toString(), failed is InterruptedIOException)
    }

    @Test
    fun whileWaiting_itSaysSince_when_andClearsOnTheAnswer() {
        val wakeUp = ServerWakeUp(server.hostName, firstCallTimeoutSeconds = 5) { now }
        val client = client(wakeUp)
        answer(delayMillis = 1_500)
        val started = CountDownLatch(1)

        val call = thread {
            started.countDown()
            get(client)
        }
        started.await()
        // Give the call time to reach the interceptor
        var seen: Long? = null
        repeat(50) {
            seen = seen ?: wakeUp.waitingSince.value
            if (seen == null) Thread.sleep(20)
        }
        call.join()

        assertEquals(1_000L, seen)
        assertNull(wakeUp.waitingSince.value)
    }

    @Test
    fun anErrorAnswer_alsoMeansTheServerIsUp() {
        val wakeUp = ServerWakeUp(server.hostName, firstCallTimeoutSeconds = 5) { now }
        val client = client(wakeUp)
        answer(code = 401)
        get(client, "/api/v1/reservations")

        answer(delayMillis = 2_000)

        assertNotNull(runCatching { get(client) }.exceptionOrNull())
        assertNull(wakeUp.waitingSince.value)
    }

    @Test
    fun noAnswer_keepsWaitingForTheNextCall_withoutAStaleStart() {
        val wakeUp = ServerWakeUp(server.hostName, firstCallTimeoutSeconds = 5) { now }
        val client = client(wakeUp)
        server.close()

        runCatching { get(client) }

        // Not awake, but nothing in flight either
        assertNull(wakeUp.waitingSince.value)
    }

    @Test
    fun callsToOtherHosts_areNotWatched() {
        val wakeUp = ServerWakeUp("api.example.com", firstCallTimeoutSeconds = 5) { now }
        answer(delayMillis = 2_000)

        val failed = runCatching { get(client(wakeUp)) }.exceptionOrNull()

        assertTrue(failed.toString(), failed is InterruptedIOException)
    }
}
