package com.zerowasteeats.app.data.remote

import com.zerowasteeats.app.data.auth.AuthTokens
import com.zerowasteeats.app.data.auth.PrefsTokenStore
import com.zerowasteeats.app.data.auth.supabase.ProfileMetadata
import com.zerowasteeats.app.data.auth.supabase.SupabaseAuth
import com.zerowasteeats.app.data.auth.supabase.SupabaseFailure
import com.zerowasteeats.app.data.auth.supabase.SupabaseResult
import com.zerowasteeats.app.data.auth.supabase.SupabaseSession
import com.zerowasteeats.app.data.auth.supabase.SupabaseUser
import com.zerowasteeats.app.data.session.InMemoryKeyValueStore
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccessTokensTest {

    private val server = MockWebServer()
    private val tokenStore = PrefsTokenStore(InMemoryKeyValueStore())
    private val oldTokens = AuthTokens("old-access", "old-refresh", 1_000)
    private val newTokens = AuthTokens("new-access", "new-refresh", 2_000)

    private val refreshCalls = AtomicInteger()
    private val expiredCalls = AtomicInteger()
    private var refreshDelayMillis = 0L
    private var outcome: RefreshOutcome = RefreshOutcome.Refreshed(newTokens)

    /** The backend: accepts only the renewed token, so the old one always gets a 401. */
    private val backend = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            if (request.headers["Authorization"] == "Bearer new-access") {
                MockResponse.Builder().code(200).body("ok").build()
            } else {
                MockResponse.Builder().code(401).body("""{"statusCode":401}""").build()
            }
    }

    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.dispatcher = backend
        server.start()
        tokenStore.save(oldTokens)
        client = createAuthenticatedHttpClient(
            base = createHttpClient(),
            interceptor = AuthTokenInterceptor(tokenStore),
            authenticator = TokenAuthenticator(
                tokenStore = tokenStore,
                refresh = { refreshToken ->
                    assertEquals("old-refresh", refreshToken)
                    refreshCalls.incrementAndGet()
                    delay(refreshDelayMillis)
                    outcome
                },
                onSessionExpired = { expiredCalls.incrementAndGet() }
            )
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun get(): Int =
        client.newCall(Request.Builder().url(server.url("/api/v1/users/me")).build())
            .execute()
            .use { it.code }

    @Test
    fun interceptor_addsTheSavedAccessToken() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                MockResponse.Builder().code(200).build()
        }

        get()

        assertEquals("Bearer old-access", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun interceptor_sendsNothingWhenSignedOut() {
        tokenStore.clear()

        assertEquals(401, get())
        assertNull(server.takeRequest().headers["Authorization"])
        assertEquals(0, refreshCalls.get())
    }

    @Test
    fun a401_renewsOnce_savesTheNewTokens_andRepeatsTheCall() {
        assertEquals(200, get())

        assertEquals(1, refreshCalls.get())
        assertEquals(newTokens, tokenStore.load())
        assertEquals(2, server.requestCount)
        assertEquals(0, expiredCalls.get())
    }

    @Test
    fun parallelCalls_shareASingleRenewal() {
        refreshDelayMillis = 300
        val calls = 8
        val pool = Executors.newFixedThreadPool(calls)
        try {
            val codes = pool.invokeAll(List(calls) { Callable { get() } })
                .map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(List(calls) { 200 }, codes)
            assertEquals(1, refreshCalls.get())
            assertEquals(newTokens, tokenStore.load())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun aRejectedRenewal_dropsTheTokens_andExpiresTheSessionOnce() {
        outcome = RefreshOutcome.Rejected
        refreshDelayMillis = 100
        val pool = Executors.newFixedThreadPool(4)
        try {
            val codes = pool.invokeAll(List(4) { Callable { get() } })
                .map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(List(4) { 401 }, codes)
            assertEquals(1, refreshCalls.get())
            assertEquals(1, expiredCalls.get())
            assertNull(tokenStore.load())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun anUnavailableRenewal_keepsTheSession() {
        outcome = RefreshOutcome.Unavailable

        assertEquals(401, get())

        assertEquals(oldTokens, tokenStore.load())
        assertEquals(0, expiredCalls.get())
    }

    @Test
    fun aRenewedTokenThatIsStillRefused_doesNotLoop() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                MockResponse.Builder().code(401).build()
        }

        assertEquals(401, get())

        assertEquals(1, refreshCalls.get())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun signingOutDuringTheRenewal_doesNotBringTheTokensBack() {
        refreshDelayMillis = 300
        val pool = Executors.newSingleThreadExecutor()
        try {
            val call = pool.submit(Callable { get() })
            Thread.sleep(150)
            tokenStore.clear()

            assertEquals(401, call.get(10, TimeUnit.SECONDS))
            assertNull(tokenStore.load())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun tokenRefresher_tellsARefusalFromATemporaryFailure() = runBlocking {
        fun supabase(result: SupabaseResult<SupabaseSession>) = object : SupabaseAuth {
            override suspend fun refresh(refreshToken: String) = result
            override suspend fun signUp(email: String, password: String, metadata: ProfileMetadata) = error("unused")
            override suspend fun signIn(email: String, password: String) = error("unused")
            override suspend fun signOut(accessToken: String) = error("unused")
            override suspend fun currentUser(accessToken: String) = error("unused")
        }
        val session = SupabaseSession(newTokens, SupabaseUser("u1", "a@b.co", null))

        assertEquals(
            RefreshOutcome.Refreshed(newTokens),
            supabase(SupabaseResult.Success(session)).tokenRefresher()("r")
        )
        assertEquals(
            RefreshOutcome.Rejected,
            supabase(SupabaseResult.Failure(SupabaseFailure.SESSION_REJECTED)).tokenRefresher()("r")
        )
        assertEquals(
            RefreshOutcome.Unavailable,
            supabase(SupabaseResult.Failure(SupabaseFailure.NETWORK)).tokenRefresher()("r")
        )
    }
}
