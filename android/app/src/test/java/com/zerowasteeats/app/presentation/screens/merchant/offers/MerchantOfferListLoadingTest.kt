package com.zerowasteeats.app.presentation.screens.merchant.offers

import com.zerowasteeats.app.data.merchant.FakeMerchantData
import com.zerowasteeats.app.data.merchant.LoadState
import com.zerowasteeats.app.data.merchant.MerchantDataError
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the list shows while offers load from the backend, and after a failure. */
class MerchantOfferListLoadingTest {

    private val someOffers = FakeMerchantData.demoOffers(
        Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC)
    )

    private fun state(offers: List<com.zerowasteeats.app.data.merchant.MerchantOffer>, load: LoadState) =
        MerchantOfferListState(offers, OfferFilter.ACTIVE, load)

    @Test
    fun theFirstLoad_showsPlaceholders_notAnEmptyMessage() {
        listOf(LoadState.Idle, LoadState.Loading).forEach { load ->
            val state = state(emptyList(), load)
            assertTrue(state.showSkeletons)
            assertFalse(state.showEmptyState)
        }
    }

    @Test
    fun aRefresh_keepsShowingTheOffersAlreadyThere() {
        val state = state(someOffers, LoadState.Loading)

        assertFalse(state.showSkeletons)
        assertTrue(state.visibleOffers.isNotEmpty())
    }

    @Test
    fun aFailedFirstLoad_doesNotClaimThereAreNoOffers() {
        val state = state(emptyList(), LoadState.Failed(MerchantDataError.NETWORK))

        assertFalse(state.showSkeletons)
        assertFalse(state.showEmptyState)
    }

    @Test
    fun onceLoaded_anEmptyGroupSaysSo() {
        assertTrue(state(emptyList(), LoadState.Loaded).showEmptyState)
    }

    @Test
    fun theDemo_isLoadedFromTheStart() {
        // The fake repository's state, which the demo keeps
        assertTrue(MerchantOfferListState(someOffers, OfferFilter.ACTIVE).showEmptyState)
        assertFalse(MerchantOfferListState(someOffers, OfferFilter.ACTIVE).showSkeletons)
    }
}
