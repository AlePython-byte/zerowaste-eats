package com.zerowasteeats.app.data.catalog

import com.zerowasteeats.app.data.remote.createHttpClient
import com.zerowasteeats.app.data.remote.createRetrofit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Saving and removing favorites through the backend, against a server the test controls. */
class RemoteFavoritesTest {

    private val server = MockWebServer()
    private lateinit var repository: RemoteOfferCatalogRepository

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/api/v1/").toString(), createHttpClient())
            .create(OfferCatalogApi::class.java)
        repository = RemoteOfferCatalogRepository(api, TestClock())
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

    private fun offerJson(id: String, isFavorite: Boolean) = """
        {"id":"$id","title":"Pan $id","category":"BAKERY",
         "originalPrice":12000,"discountedPrice":6000,"discountPercent":50,"quantityAvailable":3,
         "pickupStart":"2026-11-08T22:00:00.000Z","pickupEnd":"2026-11-08T23:00:00.000Z",
         "distanceKm":0.8,"isFavorite":$isFavorite,
         "business":{"name":"Panadería Sol","address":"Cra. 27","city":"Pasto",
           "latitude":1.215,"longitude":-77.28}}
    """.trimIndent()

    /** Loads one offer, [id], and forgets that request. */
    private fun loaded(id: String = "a", isFavorite: Boolean = false) = runBlocking {
        respond(200, "[${offerJson(id, isFavorite)}]")
        repository.refresh(PASTO_CENTER)
        server.takeRequest()
    }

    private fun offer(id: String = "a") = repository.nearby.value.first { it.id == id }

    @Test
    fun saving_putsTheOfferInTheFavorites() = runBlocking {
        loaded()
        respond(204)

        val result = repository.setFavorite("a", true)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/favorites/a", request.url.encodedPath)
        assertEquals(FavoriteResult.Saved, result)
        assertTrue(offer().isFavorite)
        assertTrue(repository.observe("a").first()!!.isFavorite)
    }

    @Test
    fun removing_deletesItFromTheFavorites() = runBlocking {
        loaded(isFavorite = true)
        respond(204)

        val result = repository.setFavorite("a", false)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/favorites/a", request.url.encodedPath)
        assertEquals(FavoriteResult.Saved, result)
        assertFalse(offer().isFavorite)
    }

    @Test
    fun aFailedSave_putsTheHeartBack_andSaysWhy() = runBlocking {
        loaded()

        mapOf(
            500 to CatalogError.NETWORK,
            401 to CatalogError.SESSION,
            403 to CatalogError.UNEXPECTED
        ).forEach { (status, error) ->
            respond(status, """{"message":"x","statusCode":$status}""")

            val result = repository.setFavorite("a", true)

            assertEquals(FavoriteResult.Failed(error), result)
            assertFalse(offer().isFavorite)
        }
    }

    @Test
    fun aFailedRemoval_keepsItSaved() = runBlocking {
        loaded(isFavorite = true)
        respond(503)

        val result = repository.setFavorite("a", false)

        assertEquals(FavoriteResult.Failed(CatalogError.NETWORK), result)
        assertTrue(offer().isFavorite)
    }

    @Test
    fun noConnection_putsTheHeartBack() = runBlocking {
        loaded()
        server.close()

        val result = repository.setFavorite("a", true)

        assertEquals(FavoriteResult.Failed(CatalogError.NETWORK), result)
        assertFalse(offer().isFavorite)
    }

    @Test
    fun savingAnOfferNoLongerOnSale_marksItNotAvailable() = runBlocking {
        loaded()
        respond(404, """{"message":"Esta oferta ya no está disponible.","statusCode":404}""")

        val result = repository.setFavorite("a", true)

        assertEquals(FavoriteResult.NotAvailable, result)
        assertFalse(offer().isFavorite)
        assertFalse(offer().isAvailable)
    }

    @Test
    fun removingAnOfferNoLongerOnSale_stillWorks() = runBlocking {
        loaded(isFavorite = true)
        respond(404)

        val result = repository.setFavorite("a", false)

        assertEquals(FavoriteResult.Saved, result)
        assertFalse(offer().isFavorite)
        assertTrue(offer().isAvailable)
    }

    @Test
    fun theNextLoad_bringsTheServersWordBack() = runBlocking {
        loaded()
        respond(404)
        repository.setFavorite("a", true)

        // The server no longer lists it
        respond(200, "[${offerJson("b", isFavorite = false)}]")
        repository.refresh(PASTO_CENTER)

        assertEquals(listOf("b"), repository.nearby.value.map { it.id })
    }
}
