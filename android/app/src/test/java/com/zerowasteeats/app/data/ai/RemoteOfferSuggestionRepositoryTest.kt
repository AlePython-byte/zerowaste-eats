package com.zerowasteeats.app.data.ai

import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.io.File
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemoteOfferSuggestionRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var repository: RemoteOfferSuggestionRepository
    private lateinit var photo: File

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(AiSuggestionApi::class.java)
        repository = RemoteOfferSuggestionRepository(api)
        photo = folder.newFile("pan.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01, 0x02))
        }
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(status: Int, body: String = "") = server.enqueue(
        MockResponse.Builder()
            .code(status)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    )

    private val suggestionJson = """
        {"title":"Bolsa de pan del día","category":"BAKERY",
         "description":"Pan blanco y mogollas de esta mañana.",
         "suggestedDiscountPercent":50,"suggestedDiscountedPrice":6000,
         "confidence":0.92,"warnings":[],"source":"claude"}
    """.trimIndent()

    @Test
    fun sendsThePhotoAndThePriceAsMultipart_toTheSuggestionEndpoint() = runBlocking {
        respond(200, suggestionJson)

        repository.suggest(photo, 12000)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/ai/offer-suggestions", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"foto\"; filename=\"pan.jpg\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
        assertTrue(body.contains("name=\"precioOriginal\""))
        assertTrue(body.contains("12000"))
    }

    @Test
    fun leavesThePriceOut_whenThereIsNone_orTheServerWouldRefuseIt() = runBlocking {
        listOf(null, 300L, 20_000_000L).forEach { price ->
            respond(200, suggestionJson)

            repository.suggest(photo, price)

            assertFalse(
                "price $price",
                server.takeRequest().body!!.utf8().contains("precioOriginal")
            )
        }
    }

    @Test
    fun aModelSuggestion_comesBackWhole() = runBlocking {
        respond(200, suggestionJson)

        assertEquals(
            SuggestionResult.Success(
                OfferSuggestion(
                    title = "Bolsa de pan del día",
                    category = OfferCategory.BAKERY,
                    description = "Pan blanco y mogollas de esta mañana.",
                    discountPercent = 50,
                    discountedPrice = 6000,
                    confidence = 0.92,
                    warnings = emptyList(),
                    isExample = false
                )
            ),
            repository.suggest(photo, 12000)
        )
    }

    @Test
    fun aSimulatedSuggestion_isMarkedAsAnExample_andKeepsTheServersWarnings() = runBlocking {
        respond(
            200,
            """{"title":"Bolsa sorpresa del día","category":"OTHER","description":"Productos variados.",
               "suggestedDiscountPercent":40,"suggestedDiscountedPrice":null,"confidence":0.5,
               "warnings":["La foto no parece mostrar comida."],"source":"mock","extra":1}"""
        )

        val suggestion = (repository.suggest(photo, null) as SuggestionResult.Success).suggestion

        assertTrue(suggestion.isExample)
        assertEquals(listOf("La foto no parece mostrar comida."), suggestion.warnings)
        // OTHER does not exist in the app: the merchant chooses
        assertEquals(null, suggestion.category)
        assertEquals(null, suggestion.discountedPrice)
    }

    @Test
    fun everyServerCategory_mapsToTheAppOrToNothing() {
        assertEquals(OfferCategory.BAKERY, categoryFromServer("BAKERY"))
        assertEquals(OfferCategory.PREPARED_FOOD, categoryFromServer("PREPARED_FOOD"))
        assertEquals(OfferCategory.DESSERTS, categoryFromServer("DESSERTS"))
        assertEquals(OfferCategory.PRODUCE, categoryFromServer("PRODUCE"))
        assertEquals(null, categoryFromServer("DAIRY"))
        assertEquals(null, categoryFromServer("OTHER"))
        assertEquals(null, categoryFromServer("SNACKS"))
    }

    private fun failureFor(status: Int): SuggestionResult = runBlocking {
        respond(status, """{"statusCode":$status,"error":"x","message":"Mensaje del servidor"}""")
        repository.suggest(photo, 12000)
    }

    @Test
    fun aPhotoTheServerRefuses_isAnInvalidPhoto() {
        listOf(400, 413, 415).forEach {
            assertEquals("status $it", SuggestionResult.Failure(SuggestionError.INVALID_PHOTO), failureFor(it))
        }
    }

    @Test
    fun aLimit_isReportedAsSuch() {
        assertEquals(SuggestionResult.Failure(SuggestionError.LIMIT_REACHED), failureFor(429))
    }

    @Test
    fun anUnavailableModel_letsTheMerchantGoOnByHand() {
        listOf(502, 503, 504).forEach {
            assertEquals("status $it", SuggestionResult.Failure(SuggestionError.UNAVAILABLE), failureFor(it))
        }
    }

    @Test
    fun anythingElse_isUnexpected() {
        listOf(401, 403, 500).forEach {
            assertEquals("status $it", SuggestionResult.Failure(SuggestionError.UNEXPECTED), failureFor(it))
        }
    }

    @Test
    fun anUnreadableAnswer_isUnexpected() = runBlocking {
        respond(200, """{"title":"incompleto"}""")

        assertEquals(SuggestionResult.Failure(SuggestionError.UNEXPECTED), repository.suggest(photo, null))
    }

    @Test
    fun noAnswer_isANetworkFailure() = runBlocking {
        server.close()

        assertEquals(SuggestionResult.Failure(SuggestionError.NETWORK), repository.suggest(photo, null))
    }
}
