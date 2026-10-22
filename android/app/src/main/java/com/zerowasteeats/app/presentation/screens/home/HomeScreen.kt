package com.zerowasteeats.app.presentation.screens.home

import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.model.OfferCategory
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.AppSearchBar
import com.zerowasteeats.app.presentation.components.filters.CategoryChip
import com.zerowasteeats.app.presentation.components.offers.OfferCard
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

@Composable
fun HomeScreen(
    onMapClick: () -> Unit = {},
    onOfferClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(OfferCategory.ALL) }

    val categories = OfferCategory.entries
    val mockOffers = MockOffers.getList()

    val filteredOffers = mockOffers.filter { offer ->
        val matchesSearch = offer.title.contains(searchQuery, ignoreCase = true) ||
                offer.merchantName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == OfferCategory.ALL || offer.category == selectedCategory
        matchesSearch && matchesCategory
    }

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
                    top = AppSpacing.regular, // Slightly tighter top spacing
                    bottom = AppSpacing.large // Trailing gap; the bottom bar inset comes from the app scaffold
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium) // Improved info density
            ) {
                item {
                    HomeHeader()
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = stringResource(R.string.home_search_placeholder),
                        clearContentDescription = stringResource(R.string.common_clear_search_cd),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    SectionHeader(onMapClick = onMapClick)
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        contentPadding = PaddingValues(bottom = AppSpacing.small)
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
private fun HomeHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionHeader(
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_section_nearby),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        TextButton(
            onClick = onMapClick,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.common_view_map),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
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
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    ZeroWasteEatsTheme {
        HomeScreen()
    }
}
