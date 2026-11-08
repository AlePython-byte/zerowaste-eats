package com.zerowasteeats.app.data.catalog

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeOfferCatalogRepositoryTest {

    private val repository = FakeOfferCatalogRepository(TestClock(), latencyMillis = 0)

    @Test
    fun servesTheSampleOffers_nearestFirst_withTheirDistanceFromThePerson() = runBlocking {
        repository.refresh(PASTO_CENTER)

        val offers = repository.nearby.value
        assertEquals(5, offers.size)
        val distances = offers.map { it.distanceKm!! }
        assertEquals(distances.sorted(), distances)
        assertTrue(distances.all { it in 0.0..2.0 })
        assertEquals(CatalogLoadState.Loaded, repository.loadState.value)
    }

    @Test
    fun everySampleOffer_hasAPhotoAndAPlaceOnTheMap() {
        FakeOfferCatalogRepository.demoOffers(TestClock()).forEach { offer ->
            assertNotNull(offer.photo)
            assertTrue(offer.business.latitude in 1.20..1.23)
            assertTrue(offer.business.longitude in -77.29..-77.27)
        }
    }

    @Test
    fun lookUp_findsASampleOffer_andAnUnknownOneIsNotAvailable() = runBlocking {
        val found = repository.lookUp("demo-catalog-3", PASTO_CENTER)
        val missing = repository.lookUp("nope", PASTO_CENTER)

        assertEquals("Caja de postres surtidos", (found as OfferLookup.Found).offer.title)
        assertEquals(OfferLookup.NotAvailable, missing)
    }

    @Test
    fun favorites_areRememberedAcrossLoads_withoutAnyNetwork() = runBlocking {
        repository.refresh(PASTO_CENTER)

        assertEquals(FavoriteResult.Saved, repository.setFavorite("demo-catalog-1", true))
        assertEquals(FavoriteResult.Saved, repository.setFavorite("demo-catalog-2", false))
        repository.refresh(PASTO_CENTER)

        val saved = repository.nearby.value.filter { it.isFavorite }.map { it.id }
        assertEquals(listOf("demo-catalog-1"), saved)
    }

    @Test
    fun savingAnUnknownOffer_isNotAvailable() = runBlocking {
        assertEquals(FavoriteResult.NotAvailable, repository.setFavorite("nope", true))
    }

    @Test
    fun distance_matchesTheServersHaversine() {
        // About 1.1 km per hundredth of a degree of latitude
        val km = FakeOfferCatalogRepository.distanceKm(GeoPoint(1.20, -77.28), GeoPoint(1.21, -77.28))

        assertEquals(1.11, km, 0.01)
    }
}
