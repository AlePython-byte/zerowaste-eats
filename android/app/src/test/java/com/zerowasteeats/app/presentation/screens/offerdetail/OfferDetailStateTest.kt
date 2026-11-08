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

    @Test
    fun theQuantity_startsAtOne_andCostsOneUnit() {
        val state = OfferDetailState(offer = offer.copy(remainingQuantity = 4), status = DetailStatus.Loaded)

        assertEquals(1, state.quantity)
        assertEquals(offer.discountedPrice, state.totalPrice)
        assertFalse(state.canDecrease)
        assertTrue(state.canIncrease)
        assertTrue(state.canReserve)
    }

    @Test
    fun theQuantity_neverGoesAboveWhatIsLeft() {
        val state = OfferDetailState(
            offer = offer.copy(remainingQuantity = 3),
            status = DetailStatus.Loaded,
            reserve = ReserveState(requestedQuantity = 7)
        )

        assertEquals(3, state.quantity)
        assertEquals(offer.discountedPrice * 3, state.totalPrice)
        assertFalse(state.canIncrease)
        assertTrue(state.canDecrease)
    }

    @Test
    fun theQuantity_neverGoesAboveTen_evenWithMoreLeft() {
        val state = OfferDetailState(
            offer = offer.copy(remainingQuantity = 25),
            status = DetailStatus.Loaded,
            reserve = ReserveState(requestedQuantity = 12)
        )

        assertEquals(10, state.maxQuantity)
        assertEquals(10, state.quantity)
        assertFalse(state.canIncrease)
    }

    @Test
    fun nothingLeft_orNoLongerOnSale_cannotBeReserved() {
        val soldOut = OfferDetailState(offer = offer.copy(remainingQuantity = 0), status = DetailStatus.Loaded)
        val gone = OfferDetailState(offer = offer.copy(isAvailable = false), status = DetailStatus.Loaded)

        assertFalse(soldOut.canReserve)
        assertFalse(soldOut.canIncrease)
        assertFalse(gone.canReserve)
    }

    @Test
    fun whileReserving_theControlsWait() {
        val state = OfferDetailState(
            offer = offer.copy(remainingQuantity = 5),
            status = DetailStatus.Loaded,
            reserve = ReserveState(requestedQuantity = 2, isReserving = true)
        )

        assertFalse(state.canReserve)
        assertFalse(state.canIncrease)
        assertFalse(state.canDecrease)
        assertEquals(2, state.quantity)
    }
}
