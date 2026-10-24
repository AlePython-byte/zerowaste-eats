package com.zerowasteeats.app.data.merchant

import java.time.Instant

enum class MerchantReservationStatus { PENDING, COMPLETED }

data class MerchantReservation(
    val id: String,
    val offerTitle: String,
    val customerName: String,
    val quantity: Int,
    val pickupTime: Instant,
    /** Short code the customer shows at pickup, for example "PAN-4821". */
    val code: String,
    val status: MerchantReservationStatus
)

sealed interface RedeemResult {
    data class Success(val reservation: MerchantReservation) : RedeemResult
    data object InvalidCode : RedeemResult
    data object AlreadyRedeemed : RedeemResult
}
