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
import androidx.compose.runtime.remember
import com.zerowasteeats.app.presentation.components.common.ProvideSkeletonShimmer
import com.zerowasteeats.app.presentation.components.common.offerListItem
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
import com.zerowasteeats.app.presentation.components.common.AppSearchBar
import com.zerowasteeats.app.presentation.components.common.EmptyStateIcon
import com.zerowasteeats.app.presentation.components.filters.CategoryChip
import com.zerowasteeats.app.presentation.components.offers.OfferCard
import com.zerowasteeats.app.presentation.components.offers.OfferCardSkeleton
import com.zerowasteeats.app.presentation.model.OfferFilters
import com.zerowasteeats.app.presentation.model.filterAndSort
import com.zerowasteeats.app.presentation.model.toUiModel
import com.zerowasteeats.app.presentation.navigation.aboveBottomBar
import com.zerowasteeats.app.presentation.screens.feed.CatalogLoadFailureSnackbar
import com.zerowasteeats.app.presentation.screens.feed.FavoriteProblem
import com.zerowasteeats.app.presentation.screens.feed.FavoriteProblemSnackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.zerowasteeats.app.presentation.screens.feed.OfferFeedState
import com.zerowasteeats.app.presentation.screens.feed.OfferFeedViewModel
import com.zerowasteeats.app.presentation.screens.feed.OfferFeedViewModelFactory
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.catalog.FakeOfferCatalogRepository
import com.zerowasteeats.app.data.session.Session
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.navigation.LocalBottomBarInset
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Clock

/** Home of the customer: the offers near them, loaded from the catalog of their account. */
@Composable
fun HomeScreen(
    session: Session.Customer,
    onMapClick: () -> Unit = {},
    onOfferClick: (String) -> Unit = {},
    clock: Clock = Clock.systemDefaultZone()
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: OfferFeedViewModel = viewModel(
        factory = OfferFeedViewModelFactory(
            container.offerCatalog(session),
            container.customerLocation,
            clock
        )
    )
    val state by viewModel.state.collectAsState()
    // Each time the screen shows, including coming back from another tab
    LaunchedEffect(viewModel) { viewModel.onShown() }

    ProvideSkeletonShimmer(active = state.showSkeletons) {
        HomeContent(
            state = state,
            favoriteFailures = viewModel.favoriteFailures,
            onRetry = viewModel::retryLoad,
            onMapClick = onMapClick,
            onOfferClick = onOfferClick,
            onFavoriteClick = viewModel::toggleFavorite,
            onRetryFavorite = viewModel::setFavorite,
            clock = clock
        )
    }
}

@Composable
private fun HomeContent(
    state: OfferFeedState,
    onRetry: () -> Unit,
    onMapClick: () -> Unit,
    onOfferClick: (String) -> Unit,
    favoriteFailures: Flow<FavoriteProblem> = emptyFlow(),
    onFavoriteClick: (String) -> Unit = {},
    onRetryFavorite: (offerId: String, favorite: Boolean) -> Unit = { _, _ -> },
    clock: Clock = Clock.systemDefaultZone()
) {
    // Worked out when the screen opens; it does not tick while the screen stays open
    val greeting = remember(clock) { Greeting.now(clock) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(OfferCategory.ALL) }

    val categories = OfferCategory.entries
    // Cards already shown once; they skip the entrance animation when they reappear
    val seenOfferIds = remember { mutableSetOf<String>() }
    // Search and category stay on the device, over the offers the server sent
    val filteredOffers = remember(state.offers, searchQuery, selectedCategory) {
        state.offers.filterAndSort(searchQuery, OfferFilters(category = selectedCategory))
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val loadingDescription = stringResource(R.string.catalog_loading)

    CatalogLoadFailureSnackbar(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = onRetry
    )
    FavoriteProblemSnackbar(
        problems = favoriteFailures,
        snackbarHostState = snackbarHostState,
        onRetry = onRetryFavorite
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.aboveBottomBar()) }
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
                    // Trailing gap, plus the floating bar the list runs under
                    bottom = AppSpacing.large + LocalBottomBarInset.current
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium) // Improved info density
            ) {
                item {
                    HomeHeader(greeting = greeting)
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

                if (state.showSkeletons) {
                    item(key = "loading") {
                        Column(
                            modifier = Modifier.semantics { contentDescription = loadingDescription },
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                        ) {
                            repeat(SKELETON_COUNT) { OfferCardSkeleton() }
                        }
                    }
                } else if (filteredOffers.isEmpty()) {
                    // After a failed first load the snackbar explains; "no offers" would not be true
                    if (state.showEmptyState) {
                        item(key = "empty") {
                            EmptyState()
                        }
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
private fun HomeHeader(greeting: Greeting, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(greeting.textRes),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        EmptyStateIcon(R.drawable.ic_empty_bag)
        Spacer(modifier = Modifier.height(AppSpacing.regular))
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    val clock = Clock.systemDefaultZone()
    ZeroWasteEatsTheme {
        HomeContent(
            state = OfferFeedState(
                offers = FakeOfferCatalogRepository.demoOffers(clock).map { it.toUiModel(clock) },
                loadState = CatalogLoadState.Loaded
            ),
            onRetry = {},
            onMapClick = {},
            onOfferClick = {}
        )
    }
}

/** Enough placeholders to fill the screen below the header. */
private const val SKELETON_COUNT = 2
