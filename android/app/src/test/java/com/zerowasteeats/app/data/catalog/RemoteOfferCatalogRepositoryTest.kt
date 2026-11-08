package com.zerowasteeats.app.data.catalog

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteOfferCatalogRepositoryTest {

    private val server = MockWebServer()
    private val clock = TestClock()
    private lateinit var repository: RemoteOfferCatalogRepository

    private val here = GeoPoint(1.2136, -77.2811)

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(OfferCatalogApi::class.java)
        repository = RemoteOfferCatalogRepository(api, clock)
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
        category: String = "BAKERY",
        distanceKm: String = "0.8",
        isFavorite: Boolean = false,
        imageUrl: String? = "https://ref.supabase.co/storage/v1/object/public/offer-photos/u/pan.jpg",
        pickupEnd: String = "2026-11-08T23:00:00.000Z"
    ) = """
        {"id":"$id","title":"Pan $id","description":"Pan de hoy","category":"$category",
         "imageUrl":${imageUrl?.let { "\"$it\"" } ?: "null"},
         "originalPrice":12000,"discountedPrice":6000,"discountPercent":50,"quantityAvailable":3,
         "pickupStart":"2026-11-08T22:00:00.000Z","pickupEnd":"$pickupEnd",
         "distanceKm":$distanceKm,"isFavorite":$isFavorite,
         "business":{"name":"Panadería Sol","isVerified":true,"establishmentName":"Sede centro",
           "address":"Cra. 27 #18-45","city":"Pasto","latitude":1.2150,"longitude":-77.28}}
    """.trimIndent()

    @Test
    fun asksForTheOffersAroundThePerson_withTheRadiusAndLimit() = runBlocking {
        respond(200, "[]")

        repository.refresh(here)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/offers", request.url.encodedPath)
        assertEquals("1.2136", request.url.queryParameter("lat"))
        assertEquals("-77.2811", request.url.queryParameter("lng"))
        assertEquals("10.0", request.url.queryParameter("radiusKm"))
        assertEquals("50", request.url.queryParameter("limit"))
    }

    @Test
    fun readsEveryFieldTheScreensShow() = runBlocking {
        respond(200, "[${offerJson("a", distanceKm = "1.4", isFavorite = true)}]")

        repository.refresh(here)

        val offer = repository.nearby.value.single()
        assertEquals("a", offer.id)
        assertEquals("Pan a", offer.title)
        assertEquals("Pan de hoy", offer.description)
        assertEquals(OfferCategory.BAKERY, offer.category)
        assertEquals(
            OfferPhoto.Remote("https://ref.supabase.co/storage/v1/object/public/offer-photos/u/pan.jpg"),
            offer.photo
        )
        assertEquals(12000L, offer.originalPrice)
        assertEquals(6000L, offer.discountedPrice)
        assertEquals(50, offer.discountPercent)
        assertEquals(3, offer.quantityAvailable)
        assertEquals(Instant.parse("2026-11-08T22:00:00Z"), offer.pickupStart)
        assertEquals(Instant.parse("2026-11-08T23:00:00Z"), offer.pickupEnd)
        assertEquals(1.4, offer.distanceKm!!, 0.0)
        assertTrue(offer.isFavorite)
        assertEquals(
            CatalogBusiness("Panadería Sol", true, "Cra. 27 #18-45", "Pasto", 1.2150, -77.28),
            offer.business
        )
        assertEquals(CatalogLoadState.Loaded, repository.loadState.value)
    }

    @Test
    fun keepsTheServerOrder_andShowsUnknownCategoriesAsPreparedFood() = runBlocking {
        respond(200, "[${offerJson("near", category = "DAIRY")},${offerJson("far", category = "OTHER")}]")

        repository.refresh(here)

        assertEquals(listOf("near", "far"), repository.nearby.value.map { it.id })
        assertTrue(repository.nearby.value.all { it.category == OfferCategory.PREPARED_FOOD })
    }

    @Test
    fun anOfferWithoutPhoto_hasNone() = runBlocking {
        respond(200, "[${offerJson("a", imageUrl = null)}]")

        repository.refresh(here)

        assertNull(repository.nearby.value.single().photo)
    }

    @Test
    fun leavesOutAnOfferItCannotRead_andKeepsTheRest() = runBlocking {
        respond(200, "[${offerJson("bad", pickupEnd = "mañana")},${offerJson("good")}]")

        repository.refresh(here)

        assertEquals(listOf("good"), repository.nearby.value.map { it.id })
    }

    @Test
    fun aFailedLoad_keepsTheOffersAlreadyLoaded_andSaysWhy() = runBlocking {
        respond(200, "[${offerJson("a")}]")
        repository.refresh(here)

        mapOf(
            500 to CatalogError.NETWORK,
            429 to CatalogError.NETWORK,
            401 to CatalogError.SESSION,
            400 to CatalogError.UNEXPECTED
        ).forEach { (status, error) ->
            respond(status, """{"message":"x","statusCode":$status}""")
            repository.refresh(here)

            assertEquals(CatalogLoadState.Failed(error), repository.loadState.value)
            assertEquals(listOf("a"), repository.nearby.value.map { it.id })
        }
    }

    @Test
    fun noConnection_isANetworkError() = runBlocking {
        server.close()

        repository.refresh(here)

        assertEquals(CatalogLoadState.Failed(CatalogError.NETWORK), repository.loadState.value)
    }

    @Test
    fun refreshIfStale_loadsOnce_thenOnlyAfterAMinute() = runBlocking {
        respond(200, "[${offerJson("a")}]")
        repository.refreshIfStale { here }
        repository.refreshIfStale { here }
        assertEquals(1, server.requestCount)

        clock.now = clock.now.plus(Duration.ofSeconds(61))
        respond(200, "[${offerJson("a")},${offerJson("b")}]")
        repository.refreshIfStale { here }

        assertEquals(2, server.requestCount)
        assertEquals(listOf("a", "b"), repository.nearby.value.map { it.id })
    }

    @Test
    fun refreshIfStale_triesAgainRightAfterAFailure() = runBlocking {
        respond(500)
        repository.refreshIfStale { here }
        respond(200, "[${offerJson("a")}]")
        repository.refreshIfStale { here }

        assertEquals(2, server.requestCount)
        assertEquals(CatalogLoadState.Loaded, repository.loadState.value)
    }

    @Test
    fun lookUp_asksForTheOfferWithTheLocation_andUpdatesTheListToo() = runBlocking {
        respond(200, "[${offerJson("a", isFavorite = false)}]")
        repository.refresh(here)
        server.takeRequest()

        respond(200, offerJson("a", isFavorite = true))
        val result = repository.lookUp("a", here)

        val request = server.takeRequest()
        assertEquals("/api/v1/offers/a", request.url.encodedPath)
        assertEquals("1.2136", request.url.queryParameter("lat"))
        assertEquals("-77.2811", request.url.queryParameter("lng"))
        assertTrue((result as OfferLookup.Found).offer.isFavorite)
        assertTrue(repository.nearby.value.single().isFavorite)
        assertTrue(repository.observe("a").first()!!.isFavorite)
    }

    @Test
    fun lookUp_withoutLocation_sendsNone_andKeepsTheDistanceTheListHad() = runBlocking {
        respond(200, "[${offerJson("a", distanceKm = "2.3")}]")
        repository.refresh(here)
        server.takeRequest()

        respond(200, offerJson("a", distanceKm = "null"))
        val result = repository.lookUp("a", null)

        val request = server.takeRequest()
        assertNull(request.url.queryParameter("lat"))
        assertNull(request.url.queryParameter("lng"))
        assertEquals(2.3, (result as OfferLookup.Found).offer.distanceKm!!, 0.0)
    }

    @Test
    fun lookUp_ofAnOfferNoLongerOnSale_isNotAvailable_andLeavesTheList() = runBlocking {
        respond(200, "[${offerJson("a")},${offerJson("b")}]")
        repository.refresh(here)

        respond(404, """{"message":"Esta oferta ya no está disponible.","statusCode":404}""")
        val result = repository.lookUp("a", here)

        assertEquals(OfferLookup.NotAvailable, result)
        assertEquals(listOf("b"), repository.nearby.value.map { it.id })
        assertNull(repository.observe("a").first())
    }

    @Test
    fun lookUp_thatFails_saysWhy_andChangesNothing() = runBlocking {
        respond(200, "[${offerJson("a")}]")
        repository.refresh(here)

        respond(503)
        val result = repository.lookUp("a", here)

        assertEquals(OfferLookup.Failed(CatalogError.NETWORK), result)
        assertEquals(listOf("a"), repository.nearby.value.map { it.id })
        assertFalse(repository.observe("a").first() == null)
    }
}
