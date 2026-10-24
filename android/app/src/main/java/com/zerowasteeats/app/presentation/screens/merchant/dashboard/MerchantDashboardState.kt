package com.zerowasteeats.app.presentation.screens.merchant.dashboard

import com.zerowasteeats.app.data.merchant.MerchantMetrics
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantProfile
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus

/** Everything the dashboard shows, worked out from what the repositories hold. */
data class MerchantDashboardState(
    val businessName: String,
    val isVerified: Boolean,
    val metrics: MerchantMetrics,
    /** Pending reservations, the soonest pickup first. */
    val upcomingPickups: List<MerchantReservation>,
    /** False for a business with no offers and no reservations yet. */
    val hasData: Boolean
) {
    companion object {
        fun from(
            profile: MerchantProfile,
            offers: List<MerchantOffer>,
            reservations: List<MerchantReservation>
        ) = MerchantDashboardState(
            businessName = profile.businessName,
            isVerified = profile.isVerified,
            metrics = MerchantMetrics.from(offers, reservations),
            upcomingPickups = reservations
                .filter { it.status == MerchantReservationStatus.PENDING }
                .sortedBy { it.pickupTime },
            hasData = offers.isNotEmpty() || reservations.isNotEmpty()
        )
    }
}
