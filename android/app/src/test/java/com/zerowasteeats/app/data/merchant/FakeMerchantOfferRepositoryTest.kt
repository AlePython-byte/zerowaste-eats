package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMerchantOfferRepositoryTest {

    private val now = Instant.parse("2026-10-24T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun repository(initial: List<MerchantOffer> = emptyList()) =
        FakeMerchantOfferRepository(initial, latencyMillis = 0, clock = clock)

    private fun draft(
        title: String = "Croissants",
        quantity: Int = 5,
        pickupEnd: Instant = now.plusSeconds(3 * 3600)
    ) = MerchantOfferDraft(
        title = title,
        category = OfferCategory.BAKERY,
        originalPrice = 15_000,
        discountedPrice = 9_000,
        quantity = quantity,
        pickupStart = now.plusSeconds(3600),
        pickupEnd = pickupEnd,
        photo = OfferPhoto.CROISSANT
    )

    @Test
    fun create_addsAnActiveOfferAtTheTop() = runBlocking {
        val repo = repository()
        val first = repo.createOffer(draft(title = "Primera"))
        val second = repo.createOffer(draft(title = "Segunda"))

        assertEquals(MerchantOfferStatus.ACTIVE, first.status)
        assertEquals(listOf(second.id, first.id), repo.offers.value.map { it.id })
    }

    @Test
    fun create_withNoStock_isSoldOutFromTheStart() = runBlocking {
        val offer = repository().createOffer(draft(quantity = 0))

        assertEquals(MerchantOfferStatus.SOLD_OUT, offer.status)
    }

    @Test
    fun create_withAPastPickupWindow_isExpired() = runBlocking {
        val past = MerchantOfferDraft(
            title = "Croissants",
            category = OfferCategory.BAKERY,
            originalPrice = 15_000,
            discountedPrice = 9_000,
            quantity = 5,
            pickupStart = now.minusSeconds(7200),
            pickupEnd = now.minusSeconds(3600),
            photo = OfferPhoto.CROISSANT
        )

        assertEquals(MerchantOfferStatus.EXPIRED, repository().createOffer(past).status)
    }

    @Test
    fun pauseAndResume_toggleAnActiveOffer() = runBlocking {
        val repo = repository()
        val offer = repo.createOffer(draft())

        assertTrue(repo.pauseOffer(offer.id))
        assertEquals(MerchantOfferStatus.PAUSED, repo.offers.value.single().status)

        assertTrue(repo.resumeOffer(offer.id))
        assertEquals(MerchantOfferStatus.ACTIVE, repo.offers.value.single().status)
    }

    @Test
    fun pause_onlyWorksOnActiveOffers_andResumeOnlyOnPausedOnes() = runBlocking {
        val repo = repository()
        val soldOut = repo.createOffer(draft(quantity = 0))
        val active = repo.createOffer(draft())

        assertFalse(repo.pauseOffer(soldOut.id))
        assertFalse(repo.resumeOffer(active.id))
        assertFalse(repo.pauseOffer("no-such-offer"))
        assertEquals(MerchantOfferStatus.SOLD_OUT, repo.offers.value.first { it.id == soldOut.id }.status)
    }

    @Test
    fun edit_toZeroQuantity_makesTheOfferSoldOut() = runBlocking {
        val repo = repository()
        val offer = repo.createOffer(draft(quantity = 3))

        val edited = repo.updateOffer(offer.id, draft(quantity = 0))

        assertEquals(MerchantOfferStatus.SOLD_OUT, edited?.status)
        assertEquals(0, repo.offers.value.single().quantity)
    }

    @Test
    fun edit_restockingASoldOutOffer_makesItActiveAgain() = runBlocking {
        val repo = repository()
        val offer = repo.createOffer(draft(quantity = 0))

        val edited = repo.updateOffer(offer.id, draft(quantity = 4))

        assertEquals(MerchantOfferStatus.ACTIVE, edited?.status)
    }

    @Test
    fun edit_keepsAPausedOfferPaused() = runBlocking {
        val repo = repository()
        val offer = repo.createOffer(draft())
        repo.pauseOffer(offer.id)

        val edited = repo.updateOffer(offer.id, draft(title = "Nuevo título"))

        assertEquals(MerchantOfferStatus.PAUSED, edited?.status)
        assertEquals("Nuevo título", repo.offers.value.single().title)
    }

    @Test
    fun edit_ofAnUnknownOffer_changesNothing() = runBlocking {
        val repo = repository()
        repo.createOffer(draft())

        assertNull(repo.updateOffer("no-such-offer", draft(title = "Otro")))
        assertEquals("Croissants", repo.offers.value.single().title)
    }

    @Test
    fun draft_rejectsInvalidValues() {
        assertThrows(IllegalArgumentException::class.java) { draft(title = "  ") }
        assertThrows(IllegalArgumentException::class.java) { draft(quantity = -1) }
        assertThrows(IllegalArgumentException::class.java) {
            draft().copy(discountedPrice = 20_000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            draft().copy(pickupEnd = now)
        }
    }

    @Test
    fun discountPercent_isComputedFromThePrices() = runBlocking {
        val offer = repository().createOffer(draft())

        assertEquals(40, offer.discountPercent)
        assertNotNull(offer.id)
    }
}
