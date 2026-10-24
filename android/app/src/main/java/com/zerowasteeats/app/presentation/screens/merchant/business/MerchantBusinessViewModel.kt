package com.zerowasteeats.app.presentation.screens.merchant.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.MerchantProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Follows the business's profile, so the tab changes when the profile does. */
class MerchantBusinessViewModel(repository: MerchantProfileRepository) : ViewModel() {

    val state: StateFlow<MerchantBusinessState> = repository.profile
        .map(MerchantBusinessState::from)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MerchantBusinessState.from(repository.profile.value)
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class MerchantBusinessViewModelFactory(
    private val repository: MerchantProfileRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MerchantBusinessViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MerchantBusinessViewModel(repository) as T
    }
}
