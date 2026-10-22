package com.zerowasteeats.app.presentation.screens.explore

import androidx.compose.runtime.saveable.SaverScope
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferFilters
import com.zerowasteeats.app.presentation.model.OfferSort
import com.zerowasteeats.app.presentation.model.OfferUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreStateTest {

    private fun offer(id: String, category: OfferCategory, price: String = "$10.000") = OfferUiModel(
        id = id,
        title = "Oferta $id",
        merchantName = "Comercio $id",
        imageUrl = "",
        originalPrice = "$20.000",
        discountedPrice = price,
        discountPercent = 40,
        distance = "1.0 km",
        remainingQuantity = 1,
        pickupTime = "",
        isFavorite = false,
        category = category
    )

    private val offers = listOf(
        offer("1", OfferCategory.BAKERY, price = "$9.000"),
        offer("2", OfferCategory.DESSERTS, price = "$5.000"),
        offer("3", OfferCategory.BAKERY, price = "$7.000")
    )

    private fun state() = ExploreState(offers)

    @Test
    fun defaults_showEverythingInGridWithNoFilters() {
        val state = state()

        assertEquals(ExploreViewMode.GRID, state.viewMode)
        assertEquals(0, state.activeFilterCount)
        assertEquals(listOf("1", "2", "3"), state.filteredOffers.map { it.id })
    }

    @Test
    fun editingDraft_doesNotChangeAppliedFiltersOrResults() {
        val state = state()
        state.openFilterSheet()

        state.updateDraft(OfferFilters(category = OfferCategory.DESSERTS))

        assertEquals(OfferFilters(), state.appliedFilters)
        assertEquals(0, state.activeFilterCount)
        assertEquals(3, state.filteredOffers.size)
    }

    @Test
    fun applyDraft_copiesDraftToAppliedAndClosesSheet() {
        val state = state()
        state.openFilterSheet()
        state.updateDraft(OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE))

        state.applyDraft()

        assertEquals(OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE), state.appliedFilters)
        assertEquals(2, state.activeFilterCount)
        assertFalse(state.isFilterSheetOpen)
        assertEquals(listOf("3", "1"), state.filteredOffers.map { it.id })
    }

    @Test
    fun clearDraft_resetsOnlyTheDraft() {
        val state = state()
        state.applyFilters(OfferFilters(OfferCategory.BAKERY))
        state.openFilterSheet()

        state.clearDraft()

        assertEquals(OfferFilters(), state.draftFilters)
        assertEquals(OfferFilters(OfferCategory.BAKERY), state.appliedFilters)
        assertTrue(state.isFilterSheetOpen)
    }

    @Test
    fun clearDraftThenApply_removesAllFilters() {
        val state = state()
        state.applyFilters(OfferFilters(OfferCategory.BAKERY, OfferSort.DISCOUNT))
        state.openFilterSheet()
        state.clearDraft()

        state.applyDraft()

        assertEquals(0, state.activeFilterCount)
    }

    @Test
    fun discardDraft_restoresDraftFromAppliedAndClosesSheet() {
        val state = state()
        state.applyFilters(OfferFilters(OfferCategory.BAKERY))
        state.openFilterSheet()
        state.updateDraft(OfferFilters(OfferCategory.DESSERTS, OfferSort.PRICE))

        state.discardDraft()

        assertEquals(OfferFilters(OfferCategory.BAKERY), state.draftFilters)
        assertEquals(OfferFilters(OfferCategory.BAKERY), state.appliedFilters)
        assertFalse(state.isFilterSheetOpen)
    }

    @Test
    fun openFilterSheet_startsFromTheAppliedFilters() {
        val state = state()
        state.applyFilters(OfferFilters(OfferCategory.DESSERTS))

        state.openFilterSheet()

        assertEquals(OfferFilters(OfferCategory.DESSERTS), state.draftFilters)
        assertTrue(state.isFilterSheetOpen)
    }

    @Test
    fun removingSingleFilters_keepsTheOthers() {
        val state = state()
        state.applyFilters(OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE))

        state.removeCategoryFilter()
        assertEquals(OfferFilters(OfferCategory.ALL, OfferSort.PRICE), state.appliedFilters)
        assertEquals(1, state.activeFilterCount)

        state.removeSortFilter()
        assertEquals(0, state.activeFilterCount)
    }

    @Test
    fun clearAll_emptiesSearchAndFilters() {
        val state = state()
        state.query = "zzz"
        state.applyFilters(OfferFilters(OfferCategory.DESSERTS, OfferSort.PRICE))

        state.clearAll()

        assertEquals("", state.query)
        assertEquals(0, state.activeFilterCount)
        assertEquals(3, state.filteredOffers.size)
    }

    @Test
    fun clearAppliedFilters_keepsTheSearchText() {
        val state = state()
        state.query = "oferta 1"
        state.applyFilters(OfferFilters(OfferCategory.DESSERTS))
        assertTrue(state.filteredOffers.isEmpty())

        state.clearAppliedFilters()

        assertEquals("oferta 1", state.query)
        assertEquals(listOf("1"), state.filteredOffers.map { it.id })
    }

    @Test
    fun mapSelection_isClearedWhenAFilterHidesIt() {
        val state = state()
        state.selectMapOffer("2") // DESSERTS

        state.applyFilters(OfferFilters(OfferCategory.BAKERY))

        assertNull(state.selectedMapOfferId)
    }

    @Test
    fun mapSelection_isClearedWhenTheSheetAppliesAFilterThatHidesIt() {
        val state = state()
        state.selectMapOffer("2")
        state.openFilterSheet()
        state.updateDraft(OfferFilters(OfferCategory.BAKERY))

        state.applyDraft()

        assertNull(state.selectedMapOfferId)
    }

    @Test
    fun mapSelection_isClearedWhenTheSearchHidesIt() {
        val state = state()
        state.selectMapOffer("2")

        state.query = "oferta 1"

        assertNull(state.selectedMapOfferId)
    }

    @Test
    fun mapSelection_staysWhenTheOfferIsStillVisible() {
        val state = state()
        state.selectMapOffer("1") // BAKERY

        state.applyFilters(OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE))
        state.query = "oferta"

        assertEquals("1", state.selectedMapOfferId)
    }

    @Test
    fun mapSelection_doesNotComeBackWhenTheFilterIsRemoved() {
        val state = state()
        state.selectMapOffer("2")
        state.applyFilters(OfferFilters(OfferCategory.BAKERY))

        state.clearAppliedFilters()

        assertNull(state.selectedMapOfferId)
    }

    @Test
    fun selectMapOffer_ignoresOffersThatAreNotVisible() {
        val state = state()
        state.applyFilters(OfferFilters(OfferCategory.BAKERY))

        state.selectMapOffer("2")

        assertNull(state.selectedMapOfferId)
    }

    @Test
    fun switchingToMap_keepsSearchFiltersAndResults() {
        val state = state()
        state.query = "oferta"
        state.applyFilters(OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE))
        val resultsBefore = state.filteredOffers

        state.viewMode = ExploreViewMode.MAP

        assertEquals("oferta", state.query)
        assertEquals(OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE), state.appliedFilters)
        assertEquals(resultsBefore, state.filteredOffers)
    }

    @Test
    fun saver_restoresMapModeAndSelection() {
        val state = state()
        state.viewMode = ExploreViewMode.MAP
        state.applyFilters(OfferFilters(OfferCategory.BAKERY))
        state.selectMapOffer("3")
        val saver = ExploreState.saver(offers)

        val saved = with(saver) { SaverScope { true }.save(state) }!!
        val restored = saver.restore(saved)!!

        assertEquals(ExploreViewMode.MAP, restored.viewMode)
        assertEquals("3", restored.selectedMapOfferId)
        assertEquals(OfferFilters(OfferCategory.BAKERY), restored.appliedFilters)
    }

    @Test
    fun saver_restoresNoSelectionAsNull() {
        val saver = ExploreState.saver(offers)

        val saved = with(saver) { SaverScope { true }.save(state()) }!!

        assertNull(saver.restore(saved)!!.selectedMapOfferId)
    }
}
