package com.zerowasteeats.app.presentation.screens.merchant.offers

import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.merchant.FakeMerchantData
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferRepository
import com.zerowasteeats.app.data.merchant.MerchantOfferStatus
import com.zerowasteeats.app.data.session.Session
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantOfferListStateTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC)

    private val demo = Session.Merchant(
        userId = "demo-merchant",
        displayName = "Negocio Demo",
        email = FakeAuthRepository.DEMO_MERCHANT_EMAIL,
        businessName = "Panadería Demo"
    )

    // Demo offers: two active (1 and 2), one paused (3), one sold out (4)
    private fun repository(): MerchantOfferRepository =
        FakeMerchantData.repositoriesFor(demo, latencyMillis = 0, clock = clock).offers

    private fun stateOf(repository: MerchantOfferRepository, filter: OfferFilter) =
        MerchantOfferListState(repository.offers.value, filter)

    private fun ids(offers: List<MerchantOffer>) = offers.map { it.id }

    @Test
    fun counts_followTheGroupsOfTheOffers() {
        val state = stateOf(repository(), OfferFilter.ACTIVE)

        assertEquals(2, state.counts.getValue(OfferFilter.ACTIVE))
        assertEquals(1, state.counts.getValue(OfferFilter.PAUSED))
        assertEquals(1, state.counts.getValue(OfferFilter.SOLD_OUT))
    }

    @Test
    fun expiredOffers_countWithTheSoldOutOnes() = runBlocking {
        val repo = repository()
        val soldOut = repo.offers.value.first { it.status == MerchantOfferStatus.SOLD_OUT }
        val expired = soldOut.copy(
            id = "expired",
            status = MerchantOfferStatus.EXPIRED
        )

        val state = MerchantOfferListState(repo.offers.value + expired, OfferFilter.SOLD_OUT)

        assertEquals(2, state.counts.getValue(OfferFilter.SOLD_OUT))
        assertEquals(setOf("demo-offer-4", "expired"), ids(state.visibleOffers).toSet())
    }

    @Test
    fun visibleOffers_areOnlyThoseOfTheChosenGroup_inOrder() {
        val repo = repository()

        assertEquals(
            listOf("demo-offer-1", "demo-offer-2"),
            ids(stateOf(repo, OfferFilter.ACTIVE).visibleOffers)
        )
        assertEquals(listOf("demo-offer-3"), ids(stateOf(repo, OfferFilter.PAUSED).visibleOffers))
        assertEquals(listOf("demo-offer-4"), ids(stateOf(repo, OfferFilter.SOLD_OUT).visibleOffers))
    }

    @Test
    fun noOffers_meansEmptyGroupsAndZeroCounts() {
        val state = MerchantOfferListState(emptyList(), OfferFilter.ACTIVE)

        assertTrue(state.visibleOffers.isEmpty())
        assertTrue(state.counts.values.all { it == 0 })
    }

    @Test
    fun pausing_movesTheOfferFromActiveToPaused_andUpdatesTheCounters() = runBlocking {
        val repo = repository()
        val offer = repo.offers.value.first { it.id == "demo-offer-1" }

        assertEquals(OfferStatusChange.PAUSE, OfferStatusChange.offeredFor(offer))
        assertTrue(OfferStatusChange.PAUSE.applyTo(repo, offer.id))

        val active = stateOf(repo, OfferFilter.ACTIVE)
        val paused = stateOf(repo, OfferFilter.PAUSED)
        assertEquals(listOf("demo-offer-2"), ids(active.visibleOffers))
        assertEquals(setOf("demo-offer-1", "demo-offer-3"), ids(paused.visibleOffers).toSet())
        assertEquals(1, active.counts.getValue(OfferFilter.ACTIVE))
        assertEquals(2, active.counts.getValue(OfferFilter.PAUSED))
    }

    @Test
    fun undo_putsTheOfferBackWhereItWas() = runBlocking {
        val repo = repository()
        val before = repo.offers.value

        OfferStatusChange.PAUSE.applyTo(repo, "demo-offer-1")
        OfferStatusChange.PAUSE.inverse.applyTo(repo, "demo-offer-1")

        assertEquals(before, repo.offers.value)
        assertEquals(2, stateOf(repo, OfferFilter.ACTIVE).counts.getValue(OfferFilter.ACTIVE))
    }

    @Test
    fun resuming_movesAPausedOfferBackToActive() = runBlocking {
        val repo = repository()
        val offer = repo.offers.value.first { it.id == "demo-offer-3" }

        assertEquals(OfferStatusChange.RESUME, OfferStatusChange.offeredFor(offer))
        assertTrue(OfferStatusChange.RESUME.applyTo(repo, offer.id))

        val state = stateOf(repo, OfferFilter.ACTIVE)
        assertEquals(3, state.counts.getValue(OfferFilter.ACTIVE))
        assertEquals(0, state.counts.getValue(OfferFilter.PAUSED))
    }

    @Test
    fun undoingWhenTheOfferAlreadyChanged_doesNothing() = runBlocking {
        val repo = repository()
        OfferStatusChange.PAUSE.applyTo(repo, "demo-offer-1")
        OfferStatusChange.RESUME.applyTo(repo, "demo-offer-1")
        val before = repo.offers.value

        // The pause was already undone by hand, so undoing it again has nothing to resume
        assertFalse(OfferStatusChange.PAUSE.inverse.applyTo(repo, "demo-offer-1"))
        assertEquals(before, repo.offers.value)
    }

    @Test
    fun soldOutAndExpiredOffers_offerNoStatusChange() {
        val soldOut = repository().offers.value.first { it.status == MerchantOfferStatus.SOLD_OUT }

        assertNull(OfferStatusChange.offeredFor(soldOut))
        assertNull(OfferStatusChange.offeredFor(soldOut.copy(status = MerchantOfferStatus.EXPIRED)))
    }

    @Test
    fun filters_groupEveryStatusExactlyOnce() {
        MerchantOfferStatus.entries.forEach { status ->
            val offer = repository().offers.value.first().copy(status = status)

            assertEquals(1, OfferFilter.entries.count { it.matches(offer) })
        }
    }
}
