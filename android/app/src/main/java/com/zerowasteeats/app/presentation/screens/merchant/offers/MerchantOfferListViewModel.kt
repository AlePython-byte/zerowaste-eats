package com.zerowasteeats.app.presentation.screens.merchant.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Something the screen tells the person once: an offer was paused or resumed. */
data class OfferStatusChanged(val offerId: String, val change: OfferStatusChange)

class MerchantOfferListViewModel(
    private val repository: MerchantOfferRepository
) : ViewModel() {

    private val filter = MutableStateFlow(OfferFilter.ACTIVE)

    val state: StateFlow<MerchantOfferListState> = combine(
        repository.offers,
        filter,
        ::MerchantOfferListState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MerchantOfferListState(repository.offers.value, filter.value)
    )

    // A channel, not state: each change is announced once, and one that happens while the
    // screen is rotating waits for the screen to come back
    private val statusChanges = Channel<OfferStatusChanged>(Channel.BUFFERED)
    val statusChanged: Flow<OfferStatusChanged> = statusChanges.receiveAsFlow()

    fun selectFilter(value: OfferFilter) {
        filter.value = value
    }

    /** Pauses an active offer or resumes a paused one, right away. */
    fun toggleStatus(offer: MerchantOffer) {
        val change = OfferStatusChange.offeredFor(offer) ?: return
        viewModelScope.launch {
            if (change.applyTo(repository, offer.id)) {
                statusChanges.send(OfferStatusChanged(offer.id, change))
            }
        }
    }

    /** Puts the offer back as it was before [event]. */
    fun undo(event: OfferStatusChanged) {
        viewModelScope.launch { event.change.inverse.applyTo(repository, event.offerId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class MerchantOfferListViewModelFactory(
    private val repository: MerchantOfferRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MerchantOfferListViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MerchantOfferListViewModel(repository) as T
    }
}
