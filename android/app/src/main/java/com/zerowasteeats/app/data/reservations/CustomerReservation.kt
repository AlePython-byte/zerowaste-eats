package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogError
import java.time.Instant

/** Where a customer's reservation stands, as the customer sees it. */
enum class CustomerReservationStatus {
    /** Reserved: the code is waiting to be shown at pickup. */
    PENDING,
    READY_FOR_PICKUP,

    /** Picked up: the business typed the code. */
    COMPLETED,
    CANCELLED
}

/** A reservation the customer made. Prices are whole pesos. */
data class CustomerReservation(
    val id: String,
    /** The pickup code the customer shows, such as "ZW-7KQ3". */
    val code: String,
    val status: CustomerReservationStatus,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long,
    /** What one unit cost before the discount, to work out the saving. */
    val originalUnitPrice: Long,
    val createdAt: Instant,
    val offerId: String,
    val offerTitle: String,
    val businessName: String,
    val pickupStart: Instant,
    val pickupEnd: Instant
) {
    /** Still to be picked up: the code is what the customer needs now. */
    val isActive: Boolean
        get() = status == CustomerReservationStatus.PENDING ||
            status == CustomerReservationStatus.READY_FOR_PICKUP
}

/** What came of trying to reserve. */
sealed interface ReserveResult {
    data class Reserved(val reservation: CustomerReservation) : ReserveResult

    /** The offer no longer has the units asked for: someone else took them first. */
    data object NotEnoughUnits : ReserveResult

    /** The offer is gone: sold out, paused, over or removed. */
    data object NotAvailable : ReserveResult

    /** Nothing was reserved: offline, the session ended, or the server failed. */
    data class Failed(val error: CatalogError) : ReserveResult
}

/** The most units one person may take from an offer in one reservation, as the server allows. */
const val MAX_UNITS_PER_RESERVATION = 10
