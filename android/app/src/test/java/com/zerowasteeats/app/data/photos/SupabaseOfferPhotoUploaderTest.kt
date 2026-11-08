package com.zerowasteeats.app.data.photos

import com.zerowasteeats.app.data.auth.AuthTokens
import com.zerowasteeats.app.data.auth.PrefsTokenStore
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.merchant.PickedPhoto
import com.zerowasteeats.app.data.remote.AuthTokenInterceptor
import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.session.InMemoryKeyValueStore
import java.io.File
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SupabaseOfferPhotoUploaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private val userId = "3f1e8a2b-0c4d-4e5f-8a9b-0c1d2e3f4a5b"
    private val tokens = PrefsTokenStore(InMemoryKeyValueStore()).apply {
        save(AuthTokens("user-access-token", "refresh", 1_900_000_000))
    }
    private lateinit var photo: PickedPhoto
    private val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)
    private var beforeUploadCalls = 0

    private lateinit var projectUrl: String

    private fun uploader() = SupabaseOfferPhotoUploader(
        // Like the app: the user's token is added by the interceptor of the shared client
        client = createHttpClient().newBuilder().addInterceptor(AuthTokenInterceptor(tokens)).build(),
        supabaseUrl = "$projectUrl/",
        publishableKey = "publishable-test-key",
        userId = userId,
        beforeUpload = { beforeUploadCalls++ },
        newFileName = { "7b0e6c1a-2f3d-4e5a-9b8c-1d2e3f4a5b6c" }
    )

    @Before
    fun setUp() {
        server.start()
        projectUrl = server.url("").toString().trimEnd('/')
        photo = PickedPhoto(folder.newFile("pan.jpg").apply { writeBytes(photoBytes) }.absolutePath)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int) = server.enqueue(
        MockResponse.Builder().code(code).body("""{"Key":"offer-photos/x.jpg"}""").build()
    )

    @Test
    fun putsTheJpegInTheBusinessFolder_withTheUserTokenAndTheKey() = runBlocking {
        respond(200)

        uploader().upload(photo)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals(
            "/storage/v1/object/offer-photos/$userId/7b0e6c1a-2f3d-4e5a-9b8c-1d2e3f4a5b6c.jpg",
            request.url.encodedPath
        )
        assertEquals("Bearer user-access-token", request.headers["Authorization"])
        assertEquals("publishable-test-key", request.headers["apikey"])
        assertEquals("image/jpeg", request.headers["Content-Type"])
        assertArrayEquals(photoBytes, request.body!!.toByteArray())
        assertEquals(1, beforeUploadCalls)
    }

    @Test
    fun answersWithThePublicUrl_theBackendAcceptsAsAnOfferPhoto() = runBlocking {
        respond(200)

        val result = uploader().upload(photo)

        val expected = "$projectUrl/storage/v1/object/public/offer-photos/$userId/" +
            "7b0e6c1a-2f3d-4e5a-9b8c-1d2e3f4a5b6c.jpg"
        assertEquals(PhotoUpload.Uploaded(OfferPhoto.Remote(expected)), result)
        // The same shape the server checks: its own folder, a plain file name, jpg
        val name = expected.removePrefix("$projectUrl/storage/v1/object/public/offer-photos/$userId/")
        assertTrue(Regex("^[A-Za-z0-9_-]{1,100}\\.(jpg|jpeg|webp)$").matches(name))
    }

    @Test
    fun aRefusal_isRejected() = runBlocking {
        listOf(400, 401, 403, 413).forEach { code ->
            respond(code)
            assertEquals(
                "status $code",
                PhotoUpload.Failed(PhotoUploadError.REJECTED),
                uploader().upload(photo)
            )
        }
    }

    @Test
    fun serverTrouble_isTemporary() = runBlocking {
        listOf(429, 500, 503).forEach { code ->
            respond(code)
            assertEquals(
                "status $code",
                PhotoUpload.Failed(PhotoUploadError.NETWORK),
                uploader().upload(photo)
            )
        }
    }

    @Test
    fun noAnswer_isANetworkFailure() = runBlocking {
        server.close()

        assertEquals(PhotoUpload.Failed(PhotoUploadError.NETWORK), uploader().upload(photo))
    }

    @Test
    fun aPhotoWhoseFileIsGone_isNotSent() = runBlocking {
        File(photo.path).delete()

        assertEquals(PhotoUpload.Failed(PhotoUploadError.UNEXPECTED), uploader().upload(photo))
        assertEquals(0, server.requestCount)
    }
}
