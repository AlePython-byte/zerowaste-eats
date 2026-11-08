package com.zerowasteeats.app.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.reservations.CustomerImpact
import com.zerowasteeats.app.data.reservations.CustomerReservationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The impact figures on the profile, and how loading them went. */
data class ProfileImpactState(
    val impact: CustomerImpact? = null,
    val loadState: CatalogLoadState = CatalogLoadState.Idle
) {
    val loadError: CatalogError? = (loadState as? CatalogLoadState.Failed)?.error

    /** No figures yet and the first load is on its way. */
    val showSkeleton: Boolean = impact == null &&
        (loadState == CatalogLoadState.Idle || loadState == CatalogLoadState.Loading)

    /** No figures because the load failed: say so in their place, with a retry. */
    val showError: Boolean = impact == null && loadError != null
}

/** Follows the customer's impact, and loads it again each time the profile shows. */
class ProfileImpactViewModel(
    private val repository: CustomerReservationRepository
) : ViewModel() {

    val state: StateFlow<ProfileImpactState> = combine(
        repository.impact,
        repository.impactState,
        ::ProfileImpactState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProfileImpactState(repository.impact.value, repository.impactState.value)
    )

    /** A pickup may have been confirmed since: showing the profile is the moment to catch up. */
    fun refresh() {
        if (repository.impactState.value == CatalogLoadState.Loading) return
        viewModelScope.launch { repository.refreshImpact() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class ProfileImpactViewModelFactory(
    private val repository: CustomerReservationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == ProfileImpactViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ProfileImpactViewModel(repository) as T
    }
}
