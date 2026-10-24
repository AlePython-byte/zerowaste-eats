package com.zerowasteeats.app.presentation.screens.merchant.dashboard

import com.zerowasteeats.app.data.merchant.MerchantMetrics
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferStatus
import com.zerowasteeats.app.data.merchant.MerchantProfile
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantDashboardStateTest {

    private val base = Instant.parse("2026-10-24T20:00:00Z")

    private val profile = MerchantProfile("Panadería Demo", "", "", "", isVerified = true)

    private fun offer(status: MerchantOfferStatus) = MerchantOffer(
        id = "o-$status",
        title = "Oferta",
        category = OfferCategory.BAKERY,
        originalPrice = 10_000,
        discountedPrice = 5_000,
        quantity = 1,
        pickupStart = base,
        pickupEnd = base.plusSeconds(3600),
        status = status,
        photo = OfferPhoto.CROISSANT
    )

    private fun reservation(
        id: String,
        minutes: Long,
        quantity: Int = 1,
        status: MerchantReservationStatus = MerchantReservationStatus.PENDING
    ) = MerchantReservation(
        id = id,
        offerTitle = "Oferta",
        customerName = "Cliente",
        quantity = quantity,
        pickupTime = base.plusSeconds(minutes * 60),
        code = "PAN-$id",
        status = status
    )

    @Test
    fun upcomingPickups_areThePendingReservations_soonestFirst() {
        val state = MerchantDashboardState.from(
            profile,
            emptyList(),
            listOf(
                reservation("late", minutes = 90),
                reservation("done", minutes = 10, status = MerchantReservationStatus.COMPLETED),
                reservation("soon", minutes = 15)
            )
        )

        assertEquals(listOf("soon", "late"), state.upcomingPickups.map { it.id })
    }

    @Test
    fun metrics_areTakenFromOffersAndReservations() {
        val state = MerchantDashboardState.from(
            profile,
            listOf(
                offer(MerchantOfferStatus.ACTIVE),
                offer(MerchantOfferStatus.PAUSED),
                offer(MerchantOfferStatus.SOLD_OUT)
            ),
            listOf(
                reservation("a", 10),
                reservation("b", 20, quantity = 3, status = MerchantReservationStatus.COMPLETED)
            )
        )

        assertEquals(MerchantMetrics(activeOffers = 1, pendingReservations = 1, rescuedMeals = 3), state.metrics)
    }

    @Test
    fun hasData_isFalseOnlyWhenThereAreNoOffersAndNoReservations() {
        assertFalse(MerchantDashboardState.from(profile, emptyList(), emptyList()).hasData)
        assertTrue(
            MerchantDashboardState.from(profile, listOf(offer(MerchantOfferStatus.PAUSED)), emptyList()).hasData
        )
        assertTrue(MerchantDashboardState.from(profile, emptyList(), listOf(reservation("a", 5))).hasData)
    }

    @Test
    fun businessNameAndVerification_comeFromTheProfile() {
        val unverified = profile.copy(businessName = "Panadería Sol", isVerified = false)

        val state = MerchantDashboardState.from(unverified, emptyList(), emptyList())

        assertEquals("Panadería Sol", state.businessName)
        assertFalse(state.isVerified)
    }
}
