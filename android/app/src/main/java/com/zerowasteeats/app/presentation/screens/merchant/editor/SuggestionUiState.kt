package com.zerowasteeats.app.presentation.screens.merchant.editor

import com.zerowasteeats.app.data.ai.SuggestionError
import com.zerowasteeats.app.data.ai.SuggestionResult

/** Where asking for a suggestion stands, for the card under the photo. */
sealed interface SuggestionUiState {
    data object Idle : SuggestionUiState

    data object Loading : SuggestionUiState

    /** The form was filled; what to tell the merchant about it. */
    data class Applied(
        val isExample: Boolean,
        val warnings: List<String>,
        val discountPercent: Int,
        /** False when no original price was given, so only the percent is known. */
        val priceFilled: Boolean
    ) : SuggestionUiState

    data class Failed(val error: SuggestionError) : SuggestionUiState
}

/** The state a finished request leaves the editor in. */
fun suggestionStateAfter(result: SuggestionResult): SuggestionUiState = when (result) {
    is SuggestionResult.Success -> with(result.suggestion) {
        SuggestionUiState.Applied(
            isExample = isExample,
            warnings = warnings.map(String::trim).filter(String::isNotEmpty),
            discountPercent = discountPercent,
            priceFilled = discountedPrice != null
        )
    }
    is SuggestionResult.Failure -> SuggestionUiState.Failed(result.error)
}

/**
 * The original price to send with the photo, when the form has one; otherwise the
 * suggestion comes back with a percent and no price.
 */
fun originalPriceForSuggestion(form: OfferEditorFormState): Long? =
    form.values.originalPrice.toLongOrNull()?.takeIf { it > 0 }
