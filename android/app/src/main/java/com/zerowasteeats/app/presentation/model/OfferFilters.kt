package com.zerowasteeats.app.presentation.model

import androidx.annotation.StringRes
import com.zerowasteeats.app.R
import java.text.Normalizer

enum class OfferSort(@StringRes val labelRes: Int) {
    DISTANCE(R.string.explore_sort_distance),
    PRICE(R.string.explore_sort_price),
    DISCOUNT(R.string.explore_sort_discount)
}

data class OfferFilters(
    val category: OfferCategory = OfferCategory.ALL,
    val sort: OfferSort = OfferSort.DISTANCE
) {
    val activeCount: Int
        get() = (if (category != OfferCategory.ALL) 1 else 0) +
                (if (sort != OfferSort.DISTANCE) 1 else 0)
}

/**
 * Filters by [query] (title or merchant, ignoring case and accents) and by category,
 * then sorts. Ties fall back to the id so the order is stable between recompositions.
 */
fun List<OfferUiModel>.filterAndSort(query: String, filters: OfferFilters): List<OfferUiModel> {
    val needle = query.normalizedForSearch()
    val comparator: Comparator<OfferUiModel> = when (filters.sort) {
        OfferSort.DISTANCE -> compareBy<OfferUiModel> { it.distanceKm() }
        OfferSort.PRICE -> compareBy<OfferUiModel> { it.discountedPriceAmount() }
        OfferSort.DISCOUNT -> compareByDescending<OfferUiModel> { it.discountPercent }
    }.thenBy { it.id }

    return filter { offer ->
        val matchesSearch = offer.title.normalizedForSearch().contains(needle) ||
                offer.merchantName.normalizedForSearch().contains(needle)
        val matchesCategory = filters.category == OfferCategory.ALL || offer.category == filters.category
        matchesSearch && matchesCategory
    }.sortedWith(comparator)
}

private val DIACRITICS = """\p{Mn}+""".toRegex()
private val NON_DISTANCE_CHARS = "[^0-9.]".toRegex()
private val NON_DIGITS = "[^0-9]".toRegex()

private fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

// The mock models still carry display strings ("0.5 km", "$8.900"); parse them in one place.
private fun OfferUiModel.distanceKm(): Float =
    distance.replace(NON_DISTANCE_CHARS, "").toFloatOrNull() ?: 0f

private fun OfferUiModel.discountedPriceAmount(): Int =
    discountedPrice.replace(NON_DIGITS, "").toIntOrNull() ?: 0
