package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.session.Session
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMerchantDataTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC)

    private val demo = Session.Merchant(
        userId = "demo-merchant",
        displayName = "Negocio Demo",
        email = FakeAuthRepository.DEMO_MERCHANT_EMAIL,
        businessName = "Panadería Demo"
    )

    private val newBusiness = Session.Merchant(
        userId = "m-2",
        displayName = "Luis Gómez",
        email = "luis@sol.co",
        businessName = "Panadería Sol"
    )

    private fun repositoriesFor(session: Session.Merchant) =
        FakeMerchantData.repositoriesFor(session, latencyMillis = 0, clock = clock)

    @Test
    fun demoAccount_hasFourOffersIncludingOnePausedAndOneSoldOut() {
        val offers = repositoriesFor(demo).offers.offers.value

        assertEquals(4, offers.size)
        assertEquals(1, offers.count { it.status == MerchantOfferStatus.PAUSED })
        assertEquals(1, offers.count { it.status == MerchantOfferStatus.SOLD_OUT })
        assertEquals(2, offers.count { it.status == MerchantOfferStatus.ACTIVE })
        assertTrue(offers.filter { it.status == MerchantOfferStatus.SOLD_OUT }.all { it.quantity == 0 })
    }

    @Test
    fun demoAccount_hasThreeReservationsTwoPending() {
        val reservations = repositoriesFor(demo).reservations.reservations.value

        assertEquals(3, reservations.size)
        assertEquals(2, reservations.count { it.status == MerchantReservationStatus.PENDING })
        assertEquals(1, reservations.count { it.status == MerchantReservationStatus.COMPLETED })
    }

    @Test
    fun demoAccount_isVerified_withItsBusinessName() {
        val profile = repositoriesFor(demo).profile.profile.value

        assertEquals("Panadería Demo", profile.businessName)
        assertTrue(profile.isVerified)
    }

    @Test
    fun newBusiness_startsEmptyAndUnverified() {
        val repositories = repositoriesFor(newBusiness)

        assertTrue(repositories.offers.offers.value.isEmpty())
        assertTrue(repositories.reservations.reservations.value.isEmpty())
        assertFalse(repositories.profile.profile.value.isVerified)
        assertEquals("Panadería Sol", repositories.profile.profile.value.businessName)
    }

    @Test
    fun demoMetrics_matchTheSeedData() {
        val repositories = repositoriesFor(demo)

        val metrics = MerchantMetrics.from(
            repositories.offers.offers.value,
            repositories.reservations.reservations.value
        )

        assertEquals(MerchantMetrics(activeOffers = 2, pendingReservations = 2, rescuedMeals = 3), metrics)
    }

    @Test
    fun metrics_ofAnEmptyBusiness_areZero() {
        assertEquals(MerchantMetrics(0, 0, 0), MerchantMetrics.from(emptyList(), emptyList()))
    }

    @Test
    fun metrics_followARedeem() = kotlinx.coroutines.runBlocking {
        val repositories = repositoriesFor(demo)

        repositories.reservations.redeem("PAN-4821")

        val metrics = MerchantMetrics.from(
            repositories.offers.offers.value,
            repositories.reservations.reservations.value
        )
        // The redeemed reservation was for 2 meals
        assertEquals(1, metrics.pendingReservations)
        assertEquals(5, metrics.rescuedMeals)
    }

    @Test
    fun metrics_countOnlyActiveOffers_afterPausing() = kotlinx.coroutines.runBlocking {
        val repositories = repositoriesFor(demo)

        repositories.offers.pauseOffer("demo-offer-1")

        val metrics = MerchantMetrics.from(
            repositories.offers.offers.value,
            repositories.reservations.reservations.value
        )
        assertEquals(1, metrics.activeOffers)
    }
}
