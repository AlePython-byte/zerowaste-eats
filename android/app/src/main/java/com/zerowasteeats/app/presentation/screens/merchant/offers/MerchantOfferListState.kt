package com.zerowasteeats.app.presentation.screens.merchant.offers

import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferRepository
import com.zerowasteeats.app.data.merchant.MerchantOfferStatus

/** The groups of the offer list. Expired offers share the sold out group: both are over. */
enum class OfferFilter(private val statuses: Set<MerchantOfferStatus>) {
    ACTIVE(setOf(MerchantOfferStatus.ACTIVE)),
    PAUSED(setOf(MerchantOfferStatus.PAUSED)),
    SOLD_OUT(setOf(MerchantOfferStatus.SOLD_OUT, MerchantOfferStatus.EXPIRED));

    fun matches(offer: MerchantOffer) = offer.status in statuses
}

/** What the three-dot menu can do to the status of an offer. */
enum class OfferStatusChange {
    PAUSE,
    RESUME;

    val inverse: OfferStatusChange
        get() = when (this) {
            PAUSE -> RESUME
            RESUME -> PAUSE
        }

    /** Returns whether the offer changed: it did not if it was no longer in the right status. */
    suspend fun applyTo(repository: MerchantOfferRepository, offerId: String): Boolean =
        when (this) {
            PAUSE -> repository.pauseOffer(offerId)
            RESUME -> repository.resumeOffer(offerId)
        }

    companion object {
        /** The change the menu offers for [offer], or null if it offers none (sold out, expired). */
        fun offeredFor(offer: MerchantOffer): OfferStatusChange? = when (offer.status) {
            MerchantOfferStatus.ACTIVE -> PAUSE
            MerchantOfferStatus.PAUSED -> RESUME
            MerchantOfferStatus.SOLD_OUT, MerchantOfferStatus.EXPIRED -> null
        }
    }
}

/** The offers, the chosen group, and what the list shows from them. */
data class MerchantOfferListState(
    val offers: List<MerchantOffer>,
    val filter: OfferFilter
) {
    /** How many offers each chip stands for. */
    val counts: Map<OfferFilter, Int> =
        OfferFilter.entries.associateWith { filter -> offers.count { filter.matches(it) } }

    /** The offers of the chosen group, in the order the repository gives them. */
    val visibleOffers: List<MerchantOffer> = offers.filter { filter.matches(it) }
}
