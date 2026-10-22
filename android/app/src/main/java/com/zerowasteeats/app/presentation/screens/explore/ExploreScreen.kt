package com.zerowasteeats.app.presentation.screens.explore

import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferFilters
import com.zerowasteeats.app.presentation.model.OfferSort
import com.zerowasteeats.app.presentation.model.filterAndSort
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zerowasteeats.app.presentation.components.common.offerListItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.filters.CategoryChip
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.AppSearchBar
import com.zerowasteeats.app.presentation.components.offers.OfferCard
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.theme.AppSpacing

@Composable
fun ExploreScreen(
    onOfferClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(OfferCategory.ALL) }
    var sortOption by rememberSaveable { mutableStateOf(OfferSort.DISTANCE) }

    val categories = OfferCategory.entries
    // Cards already shown once; they skip the entrance animation when they reappear
    val seenOfferIds = remember { mutableSetOf<String>() }
    val sortOptions = OfferSort.entries

    val mockOffers = MockOffers.getList()

    val filteredOffers = mockOffers.filterAndSort(searchQuery, OfferFilters(selectedCategory, sortOption))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AppSpacing.regular,
                    end = AppSpacing.regular,
                    top = AppSpacing.regular,
                    bottom = AppSpacing.large // Trailing gap; the bottom bar inset comes from the app scaffold
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Header
                item {
                    ExploreHeader()
                }

                // Search Bar
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = stringResource(R.string.explore_search_placeholder),
                        clearContentDescription = stringResource(R.string.common_clear_search_cd),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category Filters
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        contentPadding = PaddingValues(vertical = AppSpacing.small)
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                text = stringResource(category.labelRes),
                                isSelected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                }

                // Sorting
                item {
                    Text(
                        text = stringResource(R.string.explore_sort_by),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = AppSpacing.small, bottom = AppSpacing.extraSmall)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        contentPadding = PaddingValues(bottom = AppSpacing.small)
                    ) {
                        items(sortOptions) { option ->
                            CategoryChip(
                                text = stringResource(option.labelRes),
                                isSelected = sortOption == option,
                                onClick = { sortOption = option }
                            )
                        }
                    }
                }

                // Map Entry Point Inline
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.small),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        AppButton(
                            text = stringResource(R.string.common_view_map),
                            onClick = onMapClick,
                            style = AppButtonStyle.SECONDARY
                        )
                    }
                }

                // Results Header
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.medium))
                    ResultsHeader(count = filteredOffers.size)
                }

                // Offers or Empty State
                if (filteredOffers.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(
                        items = filteredOffers,
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
    }
}

@Composable
private fun ExploreHeader(modifier: Modifier = Modifier) {
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
private fun ResultsHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(R.string.explore_results_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        
        val countText = pluralStringResource(R.plurals.explore_offers_count, count, count)
        Text(
            text = countText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
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
    }
}
