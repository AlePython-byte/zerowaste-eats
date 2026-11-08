package com.zerowasteeats.app.presentation.screens.merchant.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.LoadState
import com.zerowasteeats.app.data.merchant.MerchantDataError
import com.zerowasteeats.app.data.merchant.MerchantDataException
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferRepository
import com.zerowasteeats.app.data.merchant.loadIfNeeded
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Something the screen tells the person once: an offer was paused or resumed. */
data class OfferStatusChanged(val offerId: String, val change: OfferStatusChange)

/** Something that went wrong, said once with a way to try again. */
sealed interface OfferListFailure {
    val error: MerchantDataError

    data class Load(override val error: MerchantDataError) : OfferListFailure

    data class Change(
        override val error: MerchantDataError,
        val offer: MerchantOffer
    ) : OfferListFailure
}

class MerchantOfferListViewModel(
    private val repository: MerchantOfferRepository
) : ViewModel() {

    private val filter = MutableStateFlow(OfferFilter.ACTIVE)

    val state: StateFlow<MerchantOfferListState> = combine(
        repository.offers,
        filter,
        repository.loadState,
        ::MerchantOfferListState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MerchantOfferListState(
            repository.offers.value,
            filter.value,
            repository.loadState.value
        )
    )

    // A channel, not state: each change is announced once, and one that happens while the
    // screen is rotating waits for the screen to come back
    private val statusChanges = Channel<OfferStatusChanged>(Channel.BUFFERED)
    val statusChanged: Flow<OfferStatusChanged> = statusChanges.receiveAsFlow()

    private val failureEvents = Channel<OfferListFailure>(Channel.BUFFERED)
    val failures: Flow<OfferListFailure> = failureEvents.receiveAsFlow()

    init {
        // Every failed load is announced, including later refreshes
        viewModelScope.launch {
            repository.loadState
                .drop(1)
                .filterIsInstance<LoadState.Failed>()
                .collect { failureEvents.send(OfferListFailure.Load(it.error)) }
        }
        // Started after the collector above, so its outcome is always seen
        viewModelScope.launch { repository.loadIfNeeded() }
    }

    fun selectFilter(value: OfferFilter) {
        filter.value = value
    }

    fun retryLoad() {
        viewModelScope.launch { repository.refresh() }
    }

    /** Pauses an active offer or resumes a paused one, right away. */
    fun toggleStatus(offer: MerchantOffer) {
        val change = OfferStatusChange.offeredFor(offer) ?: return
        viewModelScope.launch {
            try {
                if (change.applyTo(repository, offer.id)) {
                    statusChanges.send(OfferStatusChanged(offer.id, change))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: MerchantDataException) {
                failureEvents.send(OfferListFailure.Change(e.error, offer))
            }
        }
    }

    /** Puts the offer back as it was before [event]. */
    fun undo(event: OfferStatusChanged) {
        viewModelScope.launch {
            try {
                event.change.inverse.applyTo(repository, event.offerId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: MerchantDataException) {
                repository.offers.value.firstOrNull { it.id == event.offerId }?.let { offer ->
                    failureEvents.send(OfferListFailure.Change(e.error, offer))
                }
            }
        }
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
