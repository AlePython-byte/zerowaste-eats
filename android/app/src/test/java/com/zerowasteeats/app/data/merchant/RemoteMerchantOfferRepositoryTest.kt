package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteMerchantOfferRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: RemoteMerchantOfferRepository

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(MerchantOffersApi::class.java)
        repository = RemoteMerchantOfferRepository(api)
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

    private fun offerJson(
        id: String,
        status: String = "AVAILABLE",
        total: Int = 5,
        available: Int = 3,
        category: String = "BAKERY",
        imageUrl: String? = "https://ref.supabase.co/storage/v1/object/public/offer-photos/u/pan.jpg"
    ) = """
        {"id":"$id","establishmentId":"e1","title":"Pan $id","description":"Pan de hoy",
         "category":"$category","imageUrl":${imageUrl?.let { "\"$it\"" } ?: "null"},
         "originalPrice":12000,"discountedPrice":6000,"discountPercent":50,
         "quantityTotal":$total,"quantityAvailable":$available,
         "pickupStart":"2026-11-08T22:00:00.000Z","pickupEnd":"2026-11-08T23:00:00.000Z",
         "status":"$status","createdAt":"2026-11-08T12:00:00.000Z","updatedAt":"2026-11-08T12:00:00.000Z"}
    """.trimIndent()

    private val draft = MerchantOfferDraft(
        title = " Pan del día ",
        category = OfferCategory.BAKERY,
        originalPrice = 12000,
        discountedPrice = 6000,
        quantity = 4,
        pickupStart = Instant.parse("2026-11-08T22:00:00Z"),
        pickupEnd = Instant.parse("2026-11-08T23:00:00Z"),
        photo = OfferPhoto.Remote("https://ref.supabase.co/storage/v1/object/public/offer-photos/u/pan.jpg"),
        description = " Pan de hoy "
    )

    private fun bodyOf(request: mockwebserver3.RecordedRequest) =
        Json.parseToJsonElement(request.body!!.utf8()).jsonObject

    // --- Loading ---

    @Test
    fun loadsTheOffers_mappingTheServersStatusesAndKeepingReservedUnits() = runBlocking {
        respond(
            200,
            "[" + listOf(
                offerJson("a", "AVAILABLE", total = 5, available = 3),
                offerJson("b", "PAUSED"),
                offerJson("c", "SOLD_OUT", available = 0),
                offerJson("d", "EXPIRED", imageUrl = null),
                offerJson("e", "CANCELLED")
            ).joinToString(",") + "]"
        )

        repository.refresh()

        assertEquals(LoadState.Loaded, repository.loadState.value)
        val offers = repository.offers.value.associateBy { it.id }
        assertEquals(setOf("a", "b", "c", "d"), offers.keys)
        assertEquals(MerchantOfferStatus.ACTIVE, offers.getValue("a").status)
        assertEquals(MerchantOfferStatus.PAUSED, offers.getValue("b").status)
        assertEquals(MerchantOfferStatus.SOLD_OUT, offers.getValue("c").status)
        assertEquals(MerchantOfferStatus.EXPIRED, offers.getValue("d").status)
        with(offers.getValue("a")) {
            assertEquals(3, quantity)
            assertEquals(2, quantityReserved)
            assertEquals("Pan de hoy", description)
            assertEquals(Instant.parse("2026-11-08T23:00:00Z"), pickupEnd)
            assertTrue(photo is OfferPhoto.Remote)
        }
        assertNull(offers.getValue("d").photo)
        assertEquals("/api/v1/merchant/offers", server.takeRequest().url.encodedPath)
    }

    @Test
    fun aCategoryTheAppDoesNotHave_isStillShown() = runBlocking {
        respond(200, "[" + offerJson("a", category = "DAIRY") + "]")

        repository.refresh()

        assertEquals(OfferCategory.PREPARED_FOOD, repository.offers.value.single().category)
    }

    @Test
    fun aFailedLoad_keepsWhatWasLoaded_andSaysWhy() = runBlocking {
        respond(200, "[" + offerJson("a") + "]")
        repository.refresh()

        respond(503)
        repository.refresh()

        assertEquals(LoadState.Failed(MerchantDataError.NETWORK), repository.loadState.value)
        assertEquals(listOf("a"), repository.offers.value.map { it.id })
    }

    @Test
    fun loadErrors_areTold_apart() = runBlocking {
        respond(401)
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.SESSION), repository.loadState.value)

        respond(500)
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.NETWORK), repository.loadState.value)

        respond(200, "{not json")
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.UNEXPECTED), repository.loadState.value)

        server.close()
        repository.refresh()
        assertEquals(LoadState.Failed(MerchantDataError.NETWORK), repository.loadState.value)
    }

    @Test
    fun loadIfNeeded_loadsOnce_andRetriesAfterAFailure() = runBlocking {
        respond(500)
        repository.loadIfNeeded()
        respond(200, "[]")
        repository.loadIfNeeded()
        repository.loadIfNeeded()

        assertEquals(LoadState.Loaded, repository.loadState.value)
        assertEquals(2, server.requestCount)
    }

    // --- Writes ---

    @Test
    fun create_sendsTheOffer_andAddsTheServersAnswerFirst() = runBlocking {
        respond(200, "[" + offerJson("old") + "]")
        repository.refresh()
        server.takeRequest()
        respond(201, offerJson("new", total = 4, available = 4))

        val created = repository.createOffer(draft)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        val body = bodyOf(request)
        assertEquals("Pan del día", body["title"]!!.jsonPrimitive.content)
        assertEquals("Pan de hoy", body["description"]!!.jsonPrimitive.content)
        assertEquals("BAKERY", body["category"]!!.jsonPrimitive.content)
        assertEquals(12000L, body["originalPrice"]!!.jsonPrimitive.long)
        assertEquals(4, body["quantityTotal"]!!.jsonPrimitive.int)
        assertEquals("2026-11-08T22:00:00Z", body["pickupStart"]!!.jsonPrimitive.content)
        assertTrue(body["imageUrl"]!!.jsonPrimitive.content.endsWith("/u/pan.jpg"))
        assertEquals("new", created.id)
        assertEquals(listOf("new", "old"), repository.offers.value.map { it.id })
    }

    @Test
    fun update_sendsTheFreeUnitsPlusTheReservedOnes() = runBlocking {
        respond(200, "[" + offerJson("a", total = 5, available = 3) + "]")
        repository.refresh()
        server.takeRequest()
        respond(200, offerJson("a", total = 6, available = 4))

        val edited = repository.updateOffer("a", draft.copy(quantity = 4))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/merchant/offers/a", request.url.encodedPath)
        // 4 still free + 2 reserved
        assertEquals(6, bodyOf(request)["quantityTotal"]!!.jsonPrimitive.int)
        assertEquals(4, edited!!.quantity)
    }

    @Test
    fun update_ofAnOfferThatIsGone_isNull_andDropsIt() = runBlocking {
        respond(200, "[" + offerJson("a") + "]")
        repository.refresh()
        respond(404, """{"statusCode":404}""")

        assertNull(repository.updateOffer("a", draft))
        assertTrue(repository.offers.value.isEmpty())
    }

    @Test
    fun pauseAndResume_takeTheServersAnswer() = runBlocking {
        respond(200, "[" + offerJson("a") + "]")
        repository.refresh()
        respond(200, offerJson("a", status = "PAUSED"))

        assertTrue(repository.pauseOffer("a"))
        assertEquals(MerchantOfferStatus.PAUSED, repository.offers.value.single().status)

        respond(200, offerJson("a", status = "AVAILABLE"))
        assertTrue(repository.resumeOffer("a"))
        assertEquals(MerchantOfferStatus.ACTIVE, repository.offers.value.single().status)
    }

    @Test
    fun aChangeTheServerRefuses_isFalse_andShowsWhatTheServerHolds() = runBlocking {
        respond(200, "[" + offerJson("a", status = "PAUSED") + "]")
        repository.refresh()
        respond(409, """{"statusCode":409}""")
        respond(200, "[" + offerJson("a", status = "EXPIRED") + "]")

        assertFalse(repository.resumeOffer("a"))
        assertEquals(MerchantOfferStatus.EXPIRED, repository.offers.value.single().status)
    }

    @Test
    fun aWriteThatCannotReachTheServer_throwsWithItsReason() {
        respond(503)
        val failure = assertThrows(MerchantDataException::class.java) {
            runBlocking { repository.createOffer(draft) }
        }
        assertEquals(MerchantDataError.NETWORK, failure.error)

        server.close()
        val offline = assertThrows(MerchantDataException::class.java) {
            runBlocking { repository.pauseOffer("a") }
        }
        assertEquals(MerchantDataError.NETWORK, offline.error)
    }

    @Test
    fun delete_removesTheOffer_orKeepsOneWithReservations() = runBlocking {
        respond(200, "[" + offerJson("a") + "," + offerJson("b") + "]")
        repository.refresh()
        server.takeRequest()
        server.enqueue(MockResponse.Builder().code(204).build())

        assertTrue(repository.deleteOffer("a"))
        assertEquals("DELETE", server.takeRequest().method)
        assertEquals(listOf("b"), repository.offers.value.map { it.id })

        respond(409, """{"statusCode":409}""")
        assertFalse(repository.deleteOffer("b"))
        assertEquals(listOf("b"), repository.offers.value.map { it.id })
    }
}
