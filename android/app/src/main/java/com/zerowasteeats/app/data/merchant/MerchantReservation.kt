package com.zerowasteeats.app.data.merchant

import java.time.Instant

enum class MerchantReservationStatus { PENDING, COMPLETED }

data class MerchantReservation(
    val id: String,
    val offerTitle: String,
    val customerName: String,
    val quantity: Int,
    val pickupTime: Instant,
    /**
     * Short code the customer shows at pickup, for example "PAN-4821". Empty for reservations
     * from the server, which never sends it: the business types it in, and that is the proof.
     */
    val code: String,
    val status: MerchantReservationStatus
)

sealed interface RedeemResult {
    data class Success(val reservation: MerchantReservation) : RedeemResult

    /** A wrong code, with no word on how many tries are left: the app keeps its own count. */
    data object InvalidCode : RedeemResult

    /** A wrong code; the server says how many tries are left before it locks the reservation. */
    data class WrongCode(val attemptsLeft: Int) : RedeemResult

    data object AlreadyRedeemed : RedeemResult

    /** Too many wrong codes: the server takes none for this reservation for a while. */
    data class Locked(val retryAfterMillis: Long) : RedeemResult

    /** The code did not reach the server, or the server failed. */
    data class Failed(val error: MerchantDataError) : RedeemResult
}
