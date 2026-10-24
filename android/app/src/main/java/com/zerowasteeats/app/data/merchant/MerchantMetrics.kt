package com.zerowasteeats.app.data.merchant

/** The numbers on the merchant dashboard. */
data class MerchantMetrics(
    val activeOffers: Int,
    val pendingReservations: Int,
    /** Meals handed over: the sum of the quantities of completed reservations. */
    val rescuedMeals: Int
) {
    companion object {
        fun from(
            offers: List<MerchantOffer>,
            reservations: List<MerchantReservation>
        ) = MerchantMetrics(
            activeOffers = offers.count { it.status == MerchantOfferStatus.ACTIVE },
            pendingReservations = reservations.count {
                it.status == MerchantReservationStatus.PENDING
            },
            rescuedMeals = reservations
                .filter { it.status == MerchantReservationStatus.COMPLETED }
                .sumOf { it.quantity }
        )
    }
}
