package com.zerowasteeats.app.presentation.screens.merchant.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.MerchantRepositories
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Follows the business's repositories, so the dashboard changes when its data does. */
class MerchantDashboardViewModel(repositories: MerchantRepositories) : ViewModel() {

    val state: StateFlow<MerchantDashboardState> = combine(
        repositories.profile.profile,
        repositories.offers.offers,
        repositories.reservations.reservations,
        MerchantDashboardState::from
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        // The repositories already hold their data, so the first frame is not empty
        initialValue = MerchantDashboardState.from(
            repositories.profile.profile.value,
            repositories.offers.offers.value,
            repositories.reservations.reservations.value
        )
    )

    private companion object {
        // Keeps the state through a rotation without staying subscribed in the background
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class MerchantDashboardViewModelFactory(
    private val repositories: MerchantRepositories
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MerchantDashboardViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MerchantDashboardViewModel(repositories) as T
    }
}
