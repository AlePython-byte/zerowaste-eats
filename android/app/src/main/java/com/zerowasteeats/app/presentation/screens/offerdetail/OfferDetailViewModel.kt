package com.zerowasteeats.app.presentation.screens.offerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CustomerLocationSource
import com.zerowasteeats.app.data.catalog.OfferCatalogRepository
import com.zerowasteeats.app.data.catalog.OfferLookup
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.model.toUiModel
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where asking the server for the offer stands. */
sealed interface DetailStatus {
    data object Loading : DetailStatus
    data object Loaded : DetailStatus
    data object NotAvailable : DetailStatus
    data class Failed(val error: CatalogError) : DetailStatus
}

/**
 * The offer on the detail screen. What the list already knew shows at once, while the server
 * is asked for the latest; only with nothing known yet are there placeholders.
 */
data class OfferDetailState(
    val offer: OfferUiModel? = null,
    val status: DetailStatus = DetailStatus.Loading
) {
    val showSkeleton: Boolean = offer == null && status == DetailStatus.Loading

    /** The server says it can no longer be reserved. */
    val showNotAvailable: Boolean = status == DetailStatus.NotAvailable

    val loadError: CatalogError? = (status as? DetailStatus.Failed)?.error
}

class OfferDetailViewModel(
    private val offerId: String,
    private val catalog: OfferCatalogRepository,
    private val location: CustomerLocationSource,
    private val clock: Clock
) : ViewModel() {

    private val status = MutableStateFlow<DetailStatus>(DetailStatus.Loading)

    val state: StateFlow<OfferDetailState> = combine(catalog.observe(offerId), status) { offer, status ->
        OfferDetailState(offer?.toUiModel(clock), status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OfferDetailState()
    )

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        status.value = DetailStatus.Loading
        viewModelScope.launch {
            status.value = when (val result = catalog.lookUp(offerId, location.current())) {
                is OfferLookup.Found -> DetailStatus.Loaded
                OfferLookup.NotAvailable -> DetailStatus.NotAvailable
                is OfferLookup.Failed -> DetailStatus.Failed(result.error)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class OfferDetailViewModelFactory(
    private val offerId: String,
    private val catalog: OfferCatalogRepository,
    private val location: CustomerLocationSource,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OfferDetailViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OfferDetailViewModel(offerId, catalog, location, clock) as T
    }
}
