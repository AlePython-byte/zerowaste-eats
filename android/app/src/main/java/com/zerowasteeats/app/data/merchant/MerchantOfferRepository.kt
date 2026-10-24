package com.zerowasteeats.app.data.merchant

import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The merchant's own offers. Today a fake in memory; later the backend, screens unchanged. */
interface MerchantOfferRepository {
    /** Newest first. */
    val offers: StateFlow<List<MerchantOffer>>

    suspend fun createOffer(draft: MerchantOfferDraft): MerchantOffer

    /** Returns the edited offer, or null if there is no offer with that [id]. */
    suspend fun updateOffer(id: String, draft: MerchantOfferDraft): MerchantOffer?

    /** Only an active offer can be paused. Returns whether it was. */
    suspend fun pauseOffer(id: String): Boolean

    /** Only a paused offer can be resumed. Returns whether it was. */
    suspend fun resumeOffer(id: String): Boolean
}

/** In-memory offers. Every call waits [latencyMillis] to look like a real request. */
class FakeMerchantOfferRepository(
    initialOffers: List<MerchantOffer> = emptyList(),
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS,
    private val clock: Clock = Clock.systemDefaultZone()
) : MerchantOfferRepository {

    private val state = MutableStateFlow(initialOffers)

    override val offers: StateFlow<List<MerchantOffer>> = state

    override suspend fun createOffer(draft: MerchantOfferDraft): MerchantOffer {
        delay(latencyMillis)
        val offer = MerchantOffer(
            id = UUID.randomUUID().toString(),
            title = draft.title.trim(),
            category = draft.category,
            originalPrice = draft.originalPrice,
            discountedPrice = draft.discountedPrice,
            quantity = draft.quantity,
            pickupStart = draft.pickupStart,
            pickupEnd = draft.pickupEnd,
            status = resolveOfferStatus(draft.quantity, draft.pickupEnd, clock.instant(), null),
            photo = draft.photo
        )
        state.update { listOf(offer) + it }
        return offer
    }

    override suspend fun updateOffer(id: String, draft: MerchantOfferDraft): MerchantOffer? {
        delay(latencyMillis)
        var edited: MerchantOffer? = null
        state.update { offers ->
            // The block can run again if another write got in first
            edited = null
            offers.map { offer ->
                if (offer.id != id) {
                    offer
                } else {
                    offer.copy(
                        title = draft.title.trim(),
                        category = draft.category,
                        originalPrice = draft.originalPrice,
                        discountedPrice = draft.discountedPrice,
                        quantity = draft.quantity,
                        pickupStart = draft.pickupStart,
                        pickupEnd = draft.pickupEnd,
                        photo = draft.photo,
                        status = resolveOfferStatus(
                            draft.quantity,
                            draft.pickupEnd,
                            clock.instant(),
                            offer.status
                        )
                    ).also { edited = it }
                }
            }
        }
        return edited
    }

    override suspend fun pauseOffer(id: String): Boolean =
        changeStatus(id, from = MerchantOfferStatus.ACTIVE, to = MerchantOfferStatus.PAUSED)

    override suspend fun resumeOffer(id: String): Boolean =
        changeStatus(id, from = MerchantOfferStatus.PAUSED, to = MerchantOfferStatus.ACTIVE)

    private suspend fun changeStatus(
        id: String,
        from: MerchantOfferStatus,
        to: MerchantOfferStatus
    ): Boolean {
        delay(latencyMillis)
        var changed = false
        state.update { offers ->
            changed = false
            offers.map { offer ->
                if (offer.id == id && offer.status == from) {
                    changed = true
                    offer.copy(status = to)
                } else {
                    offer
                }
            }
        }
        return changed
    }

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 300L
    }
}
