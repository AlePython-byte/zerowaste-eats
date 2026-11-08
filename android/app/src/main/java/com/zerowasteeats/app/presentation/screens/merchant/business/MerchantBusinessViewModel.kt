package com.zerowasteeats.app.presentation.screens.merchant.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.MerchantProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.zerowasteeats.app.data.merchant.loadIfNeeded
import kotlinx.coroutines.flow.stateIn

/**
 * Follows the business's profile, so the tab changes when the profile does. In live mode it
 * loads the profile on the way in; a failed load is shown with a way to try again.
 */
class MerchantBusinessViewModel(
    private val repository: MerchantProfileRepository
) : ViewModel() {

    val state: StateFlow<MerchantBusinessState> = combine(
        repository.profile,
        repository.loadState
    ) { profile, load -> MerchantBusinessState.from(profile, load) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MerchantBusinessState.from(
                repository.profile.value,
                repository.loadState.value
            )
        )

    init {
        viewModelScope.launch { repository.loadIfNeeded() }
    }

    fun retryLoad() {
        viewModelScope.launch { repository.refresh() }
    }

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
