package com.zerowasteeats.app.presentation.screens.feed

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.catalog.FakeOfferCatalogRepository
import com.zerowasteeats.app.data.catalog.TestClock
import com.zerowasteeats.app.presentation.model.toUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferFeedStateTest {

    private val clock = TestClock()
    private val offers = FakeOfferCatalogRepository.demoOffers(clock).map { it.toUiModel(clock) }

    @Test
    fun beforeAndDuringTheFirstLoad_showsPlaceholders_andNoEmptyMessage() {
        listOf(CatalogLoadState.Idle, CatalogLoadState.Loading).forEach { load ->
            val state = OfferFeedState(emptyList(), load)

            assertTrue(state.showSkeletons)
            assertFalse(state.showEmptyState)
        }
    }

    @Test
    fun reloadingWithOffersOnScreen_keepsThemInsteadOfPlaceholders() {
        val state = OfferFeedState(offers, CatalogLoadState.Loading)

        assertFalse(state.showSkeletons)
    }

    @Test
    fun loadedWithNothingNear_showsTheEmptyMessage() {
        val state = OfferFeedState(emptyList(), CatalogLoadState.Loaded)

        assertFalse(state.showSkeletons)
        assertTrue(state.showEmptyState)
    }

    @Test
    fun aFailedFirstLoad_isNotPresentedAsNoOffers() {
        val state = OfferFeedState(emptyList(), CatalogLoadState.Failed(CatalogError.NETWORK))

        assertFalse(state.showSkeletons)
        assertFalse(state.showEmptyState)
        assertEquals(CatalogError.NETWORK, state.loadError)
    }

    @Test
    fun aFailedReload_keepsTheOffers_andFiltersCanStillEmptyThem() {
        val state = OfferFeedState(offers, CatalogLoadState.Failed(CatalogError.SESSION))

        assertTrue(state.showEmptyState)
        assertEquals(CatalogError.SESSION, state.loadError)
    }

    @Test
    fun noErrorOnceLoaded() {
        assertNull(OfferFeedState(offers, CatalogLoadState.Loaded).loadError)
    }
}
