package com.zerowasteeats.app.presentation.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.catalog.CustomerLocationSource
import com.zerowasteeats.app.data.catalog.OfferCatalogRepository
import com.zerowasteeats.app.presentation.model.toUiModel
import java.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The offers near the customer, for Home and for Explore: each screen has its own instance,
 * and both read the same catalog, so they always agree. Search and filters stay on the screen.
 */
class OfferFeedViewModel(
    private val catalog: OfferCatalogRepository,
    private val location: CustomerLocationSource,
    private val clock: Clock
) : ViewModel() {

    val state: StateFlow<OfferFeedState> = combine(catalog.nearby, catalog.loadState) { offers, load ->
        OfferFeedState(offers.map { it.toUiModel(clock) }, load)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OfferFeedState(
            catalog.nearby.value.map { it.toUiModel(clock) },
            catalog.loadState.value
        )
    )

    // A channel, not state: each problem is said once, and one that happens while the screen
    // is rotating waits for it to come back
    private val favoriteProblems = Channel<FavoriteProblem>(Channel.BUFFERED)
    val favoriteFailures: Flow<FavoriteProblem> = favoriteProblems.receiveAsFlow()

    /** The screen is showing: loads the offers when there are none yet or they got old. */
    fun onShown() {
        viewModelScope.launch { catalog.refreshIfStale(location::current) }
    }

    fun retryLoad() {
        viewModelScope.launch { catalog.refresh(location.current()) }
    }

    /** Saves the offer as a favorite, or removes it if it was one. */
    fun toggleFavorite(offerId: String) {
        val offer = catalog.nearby.value.firstOrNull { it.id == offerId } ?: return
        setFavorite(offerId, !offer.isFavorite)
    }

    fun setFavorite(offerId: String, favorite: Boolean) {
        viewModelScope.launch {
            catalog.setFavorite(offerId, favorite).problemFor(offerId, favorite)
                ?.let { favoriteProblems.send(it) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class OfferFeedViewModelFactory(
    private val catalog: OfferCatalogRepository,
    private val location: CustomerLocationSource,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OfferFeedViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OfferFeedViewModel(catalog, location, clock) as T
    }
}
