package com.zerowasteeats.app.presentation.screens.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferFilters
import com.zerowasteeats.app.presentation.model.OfferSort
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.model.filterAndSort

enum class ExploreViewMode { LIST, GRID }

/**
 * Everything Explore remembers: the search text, the applied filters and the view mode.
 *
 * The filter sheet edits [draftFilters], a copy that only becomes [appliedFilters] on
 * [applyDraft]. Closing the sheet any other way calls [discardDraft].
 */
@Stable
class ExploreState(
    private val offers: List<OfferUiModel>,
    query: String = "",
    appliedFilters: OfferFilters = OfferFilters(),
    viewMode: ExploreViewMode = ExploreViewMode.GRID,
    draftFilters: OfferFilters = appliedFilters,
    isFilterSheetOpen: Boolean = false
) {
    var query by mutableStateOf(query)

    var viewMode by mutableStateOf(viewMode)

    var appliedFilters by mutableStateOf(appliedFilters)
        private set

    var draftFilters by mutableStateOf(draftFilters)
        private set

    var isFilterSheetOpen by mutableStateOf(isFilterSheetOpen)
        private set

    val activeFilterCount: Int get() = appliedFilters.activeCount

    val filteredOffers: List<OfferUiModel> by derivedStateOf {
        offers.filterAndSort(this.query, this.appliedFilters)
    }

    fun openFilterSheet() {
        draftFilters = appliedFilters
        isFilterSheetOpen = true
    }

    /** The sheet was closed without confirming: the draft is thrown away. */
    fun discardDraft() {
        draftFilters = appliedFilters
        isFilterSheetOpen = false
    }

    fun updateDraft(filters: OfferFilters) {
        draftFilters = filters
    }

    /** Resets the draft only; nothing changes until [applyDraft]. */
    fun clearDraft() {
        draftFilters = OfferFilters()
    }

    fun applyDraft() {
        appliedFilters = draftFilters
        isFilterSheetOpen = false
    }

    /** Sets the applied filters directly, without going through the sheet. */
    fun applyFilters(filters: OfferFilters) {
        appliedFilters = filters
        draftFilters = filters
    }

    fun removeCategoryFilter() = applyFilters(appliedFilters.copy(category = OfferCategory.ALL))

    fun removeSortFilter() = applyFilters(appliedFilters.copy(sort = OfferSort.DISTANCE))

    fun clearAppliedFilters() = applyFilters(OfferFilters())

    companion object {
        fun saver(offers: List<OfferUiModel>) = listSaver<ExploreState, Any>(
            save = {
                listOf(
                    it.query,
                    it.viewMode.name,
                    it.appliedFilters.category.name,
                    it.appliedFilters.sort.name,
                    it.draftFilters.category.name,
                    it.draftFilters.sort.name,
                    it.isFilterSheetOpen
                )
            },
            restore = {
                ExploreState(
                    offers = offers,
                    query = it[0] as String,
                    viewMode = ExploreViewMode.valueOf(it[1] as String),
                    appliedFilters = OfferFilters(
                        OfferCategory.valueOf(it[2] as String),
                        OfferSort.valueOf(it[3] as String)
                    ),
                    draftFilters = OfferFilters(
                        OfferCategory.valueOf(it[4] as String),
                        OfferSort.valueOf(it[5] as String)
                    ),
                    isFilterSheetOpen = it[6] as Boolean
                )
            }
        )
    }
}

@Composable
fun rememberExploreState(offers: List<OfferUiModel>): ExploreState =
    rememberSaveable(saver = ExploreState.saver(offers)) { ExploreState(offers) }
