package com.zerowasteeats.app.presentation.screens.offerdetail

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.FakeOfferCatalogRepository
import com.zerowasteeats.app.data.catalog.TestClock
import com.zerowasteeats.app.presentation.model.toUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferDetailStateTest {

    private val clock = TestClock()
    private val offer = FakeOfferCatalogRepository.demoOffers(clock).first().toUiModel(clock)

    @Test
    fun withNothingKnownYet_showsPlaceholders() {
        val state = OfferDetailState(offer = null, status = DetailStatus.Loading)

        assertTrue(state.showSkeleton)
        assertFalse(state.showNotAvailable)
    }

    @Test
    fun whatTheListKnew_showsAtOnce_whileTheServerIsAsked() {
        val state = OfferDetailState(offer = offer, status = DetailStatus.Loading)

        assertFalse(state.showSkeleton)
        assertNull(state.loadError)
    }

    @Test
    fun anOfferNoLongerOnSale_saysSo() {
        val state = OfferDetailState(offer = null, status = DetailStatus.NotAvailable)

        assertTrue(state.showNotAvailable)
        assertFalse(state.showSkeleton)
    }

    @Test
    fun aFailedLookup_saysWhy_andKeepsWhatWasKnown() {
        val state = OfferDetailState(offer = offer, status = DetailStatus.Failed(CatalogError.NETWORK))

        assertEquals(CatalogError.NETWORK, state.loadError)
        assertFalse(state.showSkeleton)
        assertEquals(offer, state.offer)
    }
}
