package com.zerowasteeats.app.presentation.screens.explore

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.AppSearchBar
import com.zerowasteeats.app.presentation.components.common.offerGridItem
import com.zerowasteeats.app.presentation.components.common.offerListItem
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.components.filters.ActiveFilterChips
import com.zerowasteeats.app.presentation.components.filters.ExploreFilterSheet
import com.zerowasteeats.app.presentation.components.filters.FilterButton
import com.zerowasteeats.app.presentation.components.offers.CompactOfferCard
import com.zerowasteeats.app.presentation.components.offers.OfferCard
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppSpacing

// Flip to bring back the large "Explorar" title above the search bar
private const val SHOW_EXPLORE_TITLE = false

private const val VIEW_MODE_CROSSFADE_MILLIS = 150

private val GridSpacing = 12.dp

@Composable
fun ExploreScreen(
    onOfferClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {}
) {
    val offers = remember { MockOffers.getList() }
    val state = rememberExploreState(offers)
    val filteredOffers = state.filteredOffers
    // Cards already shown once; they skip the entrance animation when they reappear
    val seenOfferIds = remember { mutableSetOf<String>() }
    // One scroll state per mode, so each keeps its position while the other is shown
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val animationsEnabled = rememberAnimationsEnabled()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ExploreSearchHeader(
                state = state,
                resultCount = filteredOffers.size,
                modifier = Modifier.padding(
                    start = AppSpacing.regular,
                    end = AppSpacing.regular,
                    top = AppSpacing.regular
                )
            )

            Crossfade(
                targetState = state.viewMode,
                modifier = Modifier.fillMaxSize(),
                animationSpec = if (animationsEnabled) tween(VIEW_MODE_CROSSFADE_MILLIS) else snap(),
                label = "exploreViewMode"
            ) { mode ->
                when (mode) {
                    ExploreViewMode.LIST -> OfferList(
                        offers = filteredOffers,
                        listState = listState,
                        seenOfferIds = seenOfferIds,
                        onOfferClick = onOfferClick,
                        onFavoriteClick = onFavoriteClick,
                        onClearFilters = state::clearAll
                    )
                    ExploreViewMode.GRID -> OfferGrid(
                        offers = filteredOffers,
                        gridState = gridState,
                        seenOfferIds = seenOfferIds,
                        onOfferClick = onOfferClick,
                        onFavoriteClick = onFavoriteClick,
                        onClearFilters = state::clearAll
                    )
                }
            }
        }
    }

    if (state.isFilterSheetOpen) {
        ExploreFilterSheet(
            draft = state.draftFilters,
            onDraftChange = state::updateDraft,
            onApply = state::applyDraft,
            onClear = state::clearDraft,
            onDismiss = state::discardDraft
        )
    }
}

@Composable
private fun OfferGrid(
    offers: List<OfferUiModel>,
    gridState: LazyGridState,
    seenOfferIds: MutableSet<String>,
    onOfferClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.regular,
            end = AppSpacing.regular,
            top = AppSpacing.small,
            bottom = AppSpacing.large // Trailing gap; the bottom bar inset comes from the app scaffold
        ),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing)
    ) {
        if (offers.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(onClearFilters = onClearFilters)
            }
        } else {
            items(
                items = offers,
                key = { it.id }
            ) { offer ->
                CompactOfferCard(
                    modifier = offerGridItem(seenOfferIds, offer.id),
                    offer = offer,
                    onClick = { onOfferClick(offer.id) },
                    onFavoriteClick = { onFavoriteClick(offer.id) },
                    favoriteContentDescription = stringResource(
                        if (offer.isFavorite) R.string.common_favorite_remove_cd else R.string.common_favorite_add_cd
                    )
                )
            }
        }
    }
}

@Composable
private fun OfferList(
    offers: List<OfferUiModel>,
    listState: LazyListState,
    seenOfferIds: MutableSet<String>,
    onOfferClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.regular,
            end = AppSpacing.regular,
            top = AppSpacing.small,
            bottom = AppSpacing.large // Trailing gap; the bottom bar inset comes from the app scaffold
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        if (offers.isEmpty()) {
            item(key = "empty") {
                EmptyState(onClearFilters = onClearFilters)
            }
        } else {
            items(
                items = offers,
                key = { it.id }
            ) { offer ->
                OfferCard(
                    modifier = offerListItem(seenOfferIds, offer.id),
                    offer = offer,
                    onClick = { onOfferClick(offer.id) },
                    onFavoriteClick = { onFavoriteClick(offer.id) },
                    favoriteContentDescription = stringResource(
                        if (offer.isFavorite) R.string.common_favorite_remove_cd else R.string.common_favorite_add_cd
                    )
                )
            }
        }
    }
}

@Composable
private fun ExploreSearchHeader(
    state: ExploreState,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (SHOW_EXPLORE_TITLE) {
            ExploreTitle()
            Spacer(modifier = Modifier.height(AppSpacing.medium))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSearchBar(
                query = state.query,
                onQueryChange = { state.query = it },
                placeholder = stringResource(R.string.explore_search_placeholder),
                clearContentDescription = stringResource(R.string.common_clear_search_cd),
                modifier = Modifier.weight(1f)
            )
            FilterButton(
                activeCount = state.activeFilterCount,
                onClick = state::openFilterSheet
            )
        }

        ActiveFilterChips(
            filters = state.appliedFilters,
            onRemoveCategory = state::removeCategoryFilter,
            onRemoveSort = state::removeSortFilter
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.extraSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResultsCount(count = resultCount)
            ViewModeSelector(
                selected = state.viewMode,
                onSelect = { state.viewMode = it }
            )
        }
    }
}

@Composable
private fun ExploreTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.explore_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = stringResource(R.string.explore_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ResultsCount(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = pluralStringResource(R.plurals.explore_offers_count, count, count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        modifier = modifier
    )
}

@Composable
private fun EmptyState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.explore_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = stringResource(R.string.explore_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.regular))
        AppButton(
            text = stringResource(R.string.explore_empty_clear),
            onClick = onClearFilters,
            style = AppButtonStyle.SECONDARY
        )
    }
}
