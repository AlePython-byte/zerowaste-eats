package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Instant

enum class MerchantOfferStatus { ACTIVE, PAUSED, SOLD_OUT, EXPIRED }

/** Prices are whole pesos. */
data class MerchantOffer(
    val id: String,
    val title: String,
    val category: OfferCategory,
    val originalPrice: Long,
    val discountedPrice: Long,
    val quantity: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    val status: MerchantOfferStatus,
    val photo: OfferPhoto
) {
    /** Whole percent off, rounded down. */
    val discountPercent: Int
        get() = if (originalPrice <= 0) {
            0
        } else {
            ((originalPrice - discountedPrice) * 100 / originalPrice).toInt()
        }
}

/** What a merchant fills in to create or edit an offer. The status is not theirs to set. */
data class MerchantOfferDraft(
    val title: String,
    val category: OfferCategory,
    val originalPrice: Long,
    val discountedPrice: Long,
    val quantity: Int,
    val pickupStart: Instant,
    val pickupEnd: Instant,
    val photo: OfferPhoto
) {
    init {
        require(title.isNotBlank()) { "An offer needs a title" }
        require(category != OfferCategory.ALL) { "ALL is a filter, not a category" }
        require(originalPrice > 0) { "The original price must be above zero" }
        require(discountedPrice in 0..originalPrice) {
            "The discounted price cannot be negative or above the original"
        }
        require(quantity >= 0) { "The quantity cannot be negative" }
        require(pickupEnd.isAfter(pickupStart)) { "The pickup window must end after it starts" }
    }
}

/**
 * The status an offer has after it was created or edited. A past pickup window wins, then an
 * empty stock; otherwise a paused offer stays paused and anything else is active. Nothing
 * re-checks this as time passes: it is decided each time the offer is written.
 */
internal fun resolveOfferStatus(
    quantity: Int,
    pickupEnd: Instant,
    now: Instant,
    previous: MerchantOfferStatus?
): MerchantOfferStatus = when {
    !pickupEnd.isAfter(now) -> MerchantOfferStatus.EXPIRED
    quantity == 0 -> MerchantOfferStatus.SOLD_OUT
    previous == MerchantOfferStatus.PAUSED -> MerchantOfferStatus.PAUSED
    else -> MerchantOfferStatus.ACTIVE
}
