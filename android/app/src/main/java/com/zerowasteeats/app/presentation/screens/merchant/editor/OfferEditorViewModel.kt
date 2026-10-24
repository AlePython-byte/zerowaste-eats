package com.zerowasteeats.app.presentation.screens.merchant.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferDraft
import com.zerowasteeats.app.data.merchant.MerchantOfferRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Saves the offer, so a save in flight is not cancelled by a rotation. The form itself is
 * kept by the screen (see [OfferEditorFormState.Saver]).
 */
class OfferEditorViewModel(
    private val repository: MerchantOfferRepository,
    /** Null when creating a new offer. */
    val offerId: String?
) : ViewModel() {

    /** The offer being edited as it is saved now, or null when creating or if it is gone. */
    val existingOffer: MerchantOffer? =
        offerId?.let { id -> repository.offers.value.firstOrNull { it.id == id } }

    var isSaving by mutableStateOf(false)
        private set

    /** Set when the offer disappeared while it was being edited. */
    var notFound by mutableStateOf(false)
        private set

    // Sent once, so the screen leaves only after a save, and a save that finishes during a
    // rotation is still seen when the screen comes back
    private val savedResults = Channel<OfferSaveResult>(Channel.BUFFERED)
    val saved: Flow<OfferSaveResult> = savedResults.receiveAsFlow()

    fun save(draft: MerchantOfferDraft) {
        if (isSaving) return
        isSaving = true
        notFound = false
        viewModelScope.launch {
            val result = saveOffer(repository, offerId, draft)
            isSaving = false
            if (result == OfferSaveResult.NOT_FOUND) notFound = true else savedResults.send(result)
        }
    }
}

class OfferEditorViewModelFactory(
    private val repository: MerchantOfferRepository,
    private val offerId: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OfferEditorViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OfferEditorViewModel(repository, offerId) as T
    }
}
