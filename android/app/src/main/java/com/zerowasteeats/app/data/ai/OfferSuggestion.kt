package com.zerowasteeats.app.data.ai

import com.zerowasteeats.app.presentation.model.OfferCategory
import java.io.File

/** Offer details proposed from a photo, ready to fill the editor. */
data class OfferSuggestion(
    val title: String,
    /** Null when the suggestion is a category the app does not offer (dairy, other). */
    val category: OfferCategory?,
    val description: String,
    val discountPercent: Int,
    /** Null when no original price was sent, so there was nothing to discount. */
    val discountedPrice: Long?,
    /** From 0 to 1. */
    val confidence: Double,
    /** Advice for the merchant, as the server wrote it (a photo that is not food, say). */
    val warnings: List<String>,
    /** True for a simulated suggestion: the server without a model key, or demo mode. */
    val isExample: Boolean
)

/** Why no suggestion came back, reduced to what the editor says differently. */
enum class SuggestionError {
    /** The photo was refused (too big, not JPEG or WebP). */
    INVALID_PHOTO,

    /** The per-minute or daily limit of suggestions was reached. */
    LIMIT_REACHED,

    /** The model is not available: the offer can still be completed by hand. */
    UNAVAILABLE,
    NETWORK,
    UNEXPECTED
}

sealed interface SuggestionResult {
    data class Success(val suggestion: OfferSuggestion) : SuggestionResult
    data class Failure(val error: SuggestionError) : SuggestionResult
}

/** Proposes offer details from a prepared photo. Live (the backend) or demo (local). */
fun interface OfferSuggestionRepository {
    suspend fun suggest(photo: File, originalPrice: Long?): SuggestionResult
}

/** The server's categories that exist in the app; the rest are left for the merchant to choose. */
internal fun categoryFromServer(name: String): OfferCategory? = when (name) {
    "BAKERY" -> OfferCategory.BAKERY
    "PREPARED_FOOD" -> OfferCategory.PREPARED_FOOD
    "DESSERTS" -> OfferCategory.DESSERTS
    "PRODUCE" -> OfferCategory.PRODUCE
    else -> null
}
