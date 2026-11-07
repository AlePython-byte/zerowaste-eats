package com.zerowasteeats.app.presentation.screens.merchant.editor

import android.net.Uri
import com.zerowasteeats.app.data.ai.OfferSuggestion
import com.zerowasteeats.app.data.ai.OfferSuggestionRepository
import com.zerowasteeats.app.data.ai.SuggestionResult
import java.io.File
import androidx.compose.runtime.getValue
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.photos.OfferPhotoPreparer
import com.zerowasteeats.app.data.photos.PhotoPreparation
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
    val offerId: String?,
    private val photoPreparer: OfferPhotoPreparer,
    private val suggestions: OfferSuggestionRepository
) : ViewModel() {

    var suggestion by mutableStateOf<SuggestionUiState>(SuggestionUiState.Idle)
        private set

    // A suggestion that arrives during a rotation still fills the form when it comes back
    private val suggestionsToApply = Channel<OfferSuggestion>(Channel.BUFFERED)
    val suggested: Flow<OfferSuggestion> = suggestionsToApply.receiveAsFlow()

    /**
     * Asks for offer details from [photo]. On success the form is filled through [suggested];
     * on failure the form is left exactly as it was, so the merchant can retry or go on by hand.
     */
    fun suggest(photo: OfferPhoto.Device, originalPrice: Long?) {
        if (suggestion == SuggestionUiState.Loading) return
        suggestion = SuggestionUiState.Loading
        viewModelScope.launch {
            val result = suggestions.suggest(File(photo.path), originalPrice)
            if (result is SuggestionResult.Success) suggestionsToApply.send(result.suggestion)
            suggestion = suggestionStateAfter(result)
        }
    }

    var isPreparingPhoto by mutableStateOf(false)
        private set

    /** Set when the last picked photo could not be used. */
    var photoFailed by mutableStateOf(false)
        private set

    // Like [saved]: a photo that finishes during a rotation still reaches the form
    private val preparedPhotos = Channel<OfferPhoto.Device>(Channel.BUFFERED)
    val prepared: Flow<OfferPhoto.Device> = preparedPhotos.receiveAsFlow()

    /** Decodes, turns upright and compresses the picked photo, off the main thread. */
    fun preparePhoto(uri: Uri) {
        if (isPreparingPhoto) return
        isPreparingPhoto = true
        photoFailed = false
        viewModelScope.launch {
            when (val result = photoPreparer.prepare(uri)) {
                is PhotoPreparation.Ready -> preparedPhotos.send(result.photo)
                PhotoPreparation.Unreadable -> photoFailed = true
            }
            isPreparingPhoto = false
        }
    }

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
    private val offerId: String?,
    private val photoPreparer: OfferPhotoPreparer,
    private val suggestions: OfferSuggestionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OfferEditorViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OfferEditorViewModel(repository, offerId, photoPreparer, suggestions) as T
    }
}
