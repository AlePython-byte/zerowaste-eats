package com.zerowasteeats.app.data.catalog

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The heart changes before the server answers, and goes back if the answer is a failure. */
class OptimisticFavoriteTest {

    /** A catalog whose favorite writes wait for the test to answer them. */
    private class GatedCatalog : CachedOfferCatalog(TestClock()) {
        val answer = CompletableDeferred<Boolean>()
        private val offers = FakeOfferCatalogRepository.demoOffers(TestClock())

        override suspend fun fetchNearby(location: GeoPoint) = offers
        override suspend fun fetchOffer(id: String, location: GeoPoint?) = offers.firstOrNull { it.id == id }
        override suspend fun sendFavorite(id: String, favorite: Boolean): Boolean = answer.await()
    }

    private val id = "demo-catalog-1"

    private fun GatedCatalog.isFavorite() = nearby.value.first { it.id == id }.isFavorite

    @Test
    fun theHeartChangesAtOnce_andStaysWhenTheServerAgrees() = runBlocking {
        val catalog = GatedCatalog().apply { refresh(PASTO_CENTER) }

        val result = async { catalog.setFavorite(id, true) }
        yield()
        assertTrue(catalog.isFavorite())

        catalog.answer.complete(true)
        assertEquals(FavoriteResult.Saved, result.await())
        assertTrue(catalog.isFavorite())
    }

    @Test
    fun theHeartGoesBack_whenTheServerCannotBeReached() = runBlocking {
        val catalog = GatedCatalog().apply { refresh(PASTO_CENTER) }

        val result = async { catalog.setFavorite(id, true) }
        yield()
        assertTrue(catalog.isFavorite())

        catalog.answer.completeExceptionally(IOException("offline"))
        assertEquals(FavoriteResult.Failed(CatalogError.NETWORK), result.await())
        assertFalse(catalog.isFavorite())
    }

    @Test
    fun theHeartGoesBack_whenTheChangeIsCancelled() = runBlocking {
        val catalog = GatedCatalog().apply { refresh(PASTO_CENTER) }

        val result = async { catalog.setFavorite(id, true) }
        yield()
        result.cancel()
        yield()

        assertFalse(catalog.isFavorite())
    }
}
