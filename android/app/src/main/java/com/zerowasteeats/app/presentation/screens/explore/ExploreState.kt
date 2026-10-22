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

enum class ExploreViewMode { LIST, GRID, MAP }

/**
 * Everything Explore remembers: the search text, the applied filters, the view mode and
 * the offer selected on the map.
 *
 * The filter sheet edits [draftFilters], a copy that only becomes [appliedFilters] on
 * [applyDraft]. Closing the sheet any other way calls [discardDraft].
 *
 * The map selection always points at a visible offer: whenever the search or the filters
 * hide it, it is cleared.
 */
@Stable
class ExploreState(
    private val offers: List<OfferUiModel>,
    query: String = "",
    appliedFilters: OfferFilters = OfferFilters(),
    viewMode: ExploreViewMode = ExploreViewMode.GRID,
    draftFilters: OfferFilters = appliedFilters,
    isFilterSheetOpen: Boolean = false,
    selectedMapOfferId: String? = null
) {
    private var queryState by mutableStateOf(query)

    var query: String
        get() = queryState
        set(value) {
            queryState = value
            dropHiddenMapSelection()
        }

    var viewMode by mutableStateOf(viewMode)

    var appliedFilters by mutableStateOf(appliedFilters)
        private set

    var draftFilters by mutableStateOf(draftFilters)
        private set

    var isFilterSheetOpen by mutableStateOf(isFilterSheetOpen)
        private set

    var selectedMapOfferId by mutableStateOf(selectedMapOfferId)
        private set

    val activeFilterCount: Int get() = appliedFilters.activeCount

    val filteredOffers: List<OfferUiModel> by derivedStateOf {
        offers.filterAndSort(this.query, this.appliedFilters)
    }

    /** Selects an offer on the map; ids that are not among the results are ignored. */
    fun selectMapOffer(offerId: String?) {
        selectedMapOfferId = offerId?.takeIf { id -> filteredOffers.any { it.id == id } }
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
        dropHiddenMapSelection()
    }

    /** Sets the applied filters directly, without going through the sheet. */
    fun applyFilters(filters: OfferFilters) {
        appliedFilters = filters
        draftFilters = filters
        dropHiddenMapSelection()
    }

    fun removeCategoryFilter() = applyFilters(appliedFilters.copy(category = OfferCategory.ALL))

    fun removeSortFilter() = applyFilters(appliedFilters.copy(sort = OfferSort.DISTANCE))

    fun clearAppliedFilters() = applyFilters(OfferFilters())

    /** Empty search and no filters. */
    fun clearAll() {
        query = ""
        clearAppliedFilters()
    }

    private fun dropHiddenMapSelection() {
        val selected = selectedMapOfferId ?: return
        if (filteredOffers.none { it.id == selected }) selectedMapOfferId = null
    }

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
                    it.isFilterSheetOpen,
                    it.selectedMapOfferId.orEmpty()
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
                    isFilterSheetOpen = it[6] as Boolean,
                    selectedMapOfferId = (it[7] as String).ifEmpty { null }
                )
            }
        )
    }
}

@Composable
fun rememberExploreState(offers: List<OfferUiModel>): ExploreState =
    rememberSaveable(saver = ExploreState.saver(offers)) { ExploreState(offers) }
