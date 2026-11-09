package com.zerowasteeats.app.presentation.screens.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.EmptyStateIcon
import com.zerowasteeats.app.presentation.components.common.offerListItem
import com.zerowasteeats.app.presentation.components.common.ProvideSkeletonShimmer
import com.zerowasteeats.app.presentation.components.common.skeletonShimmer
import com.zerowasteeats.app.presentation.components.orders.OrderCard
import com.zerowasteeats.app.presentation.navigation.LocalBottomBarInset
import com.zerowasteeats.app.presentation.navigation.aboveBottomBar
import com.zerowasteeats.app.presentation.screens.feed.LoadFailureSnackbar
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens

/**
 * The customer's reservations: to pick up, with the code, and picked up or cancelled. They are
 * loaded again each time the screen shows, so a pickup the business just confirmed moves to
 * the history.
 */
@Composable
fun OrdersScreen(session: Session.Customer) {
    val container = (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: OrdersViewModel = viewModel(
        factory = OrdersViewModelFactory(container.customerReservations(session))
    )
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(OrdersTab.ACTIVE) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Ids already shown, so a card that only moves between tabs does not enter again
    val seenIds = remember { mutableSetOf<String>() }

    LaunchedEffect(Unit) { viewModel.refresh() }

    val refreshMessages = mapOf(
        CatalogError.NETWORK to stringResource(R.string.orders_refresh_failed_network),
        CatalogError.SESSION to stringResource(R.string.catalog_session_failed),
        CatalogError.UNEXPECTED to stringResource(R.string.orders_refresh_failed)
    )
    // With orders on screen a failure only gets a snackbar; without them, the whole screen says it
    LoadFailureSnackbar(
        error = state.loadError.takeIf { !state.showError },
        hasContent = true,
        snackbarHostState = snackbarHostState,
        messageFor = refreshMessages::getValue,
        retryLabel = stringResource(R.string.auth_retry),
        onRetry = viewModel::refresh
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.aboveBottomBar()) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.regular,
                        end = AppSpacing.regular,
                        top = AppSpacing.regular,
                        bottom = AppSpacing.small
                    )
            ) {
                Text(
                    text = stringResource(R.string.orders_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                Text(
                    text = stringResource(R.string.orders_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                // Segmented Selector
                OrdersSegmentedSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            val displayedOrders = state.ordersIn(selectedTab)
            val loadError = state.loadError
            when {
                state.showSkeleton -> OrdersSkeleton()
                state.showError && loadError != null -> OrdersLoadError(
                    error = loadError,
                    onRetry = viewModel::refresh
                )
                displayedOrders.isEmpty() -> EmptyOrdersState(selectedTab = selectedTab)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.regular,
                        end = AppSpacing.regular,
                        top = AppSpacing.small,
                        // Trailing gap, plus the floating bar the list runs under
                        bottom = AppSpacing.large + LocalBottomBarInset.current
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    items(displayedOrders, key = { it.id }) { order ->
                        OrderCard(order = order, modifier = offerListItem(seenIds, order.id))
                    }
                }
            }
        }
    }
}

/** The shape of a few order cards while the first load runs. Screen readers hear it loads. */
@Composable
private fun OrdersSkeleton() {
    ProvideSkeletonShimmer(active = true) { OrdersSkeletonContent() }
}

@Composable
private fun OrdersSkeletonContent() {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val lineShape = RoundedCornerShape(8.dp)
    val loading = stringResource(R.string.orders_loading)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.regular, vertical = AppSpacing.small)
            .semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .clip(RoundedCornerShape(AppRadius.large))
                    .skeletonShimmer()
                    .background(surface)
                    .padding(AppSpacing.regular),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Box(Modifier.fillMaxWidth(0.35f).height(12.dp).clip(lineShape).background(line))
                Box(Modifier.fillMaxWidth(0.75f).height(20.dp).clip(lineShape).background(line))
                Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(lineShape).background(line))
                Box(Modifier.fillMaxWidth(0.3f).height(18.dp).clip(lineShape).background(line))
            }
        }
    }
}

/** The orders could not be loaded and there is nothing to show instead. */
@Composable
private fun OrdersLoadError(error: CatalogError, onRetry: () -> Unit) {
    val message = when (error) {
        CatalogError.NETWORK -> stringResource(R.string.orders_load_failed_network)
        CatalogError.SESSION -> stringResource(R.string.catalog_session_failed)
        CatalogError.UNEXPECTED -> stringResource(R.string.orders_load_failed)
    }
    // Trying again does not help an ended session: only signing in again does
    val canRetry = error != CatalogError.SESSION
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LocalBottomBarInset.current),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            title = stringResource(R.string.orders_load_failed_title),
            message = message,
            icon = R.drawable.ic_empty_receipt,
            actionText = if (canRetry) stringResource(R.string.auth_retry) else null,
            onActionClick = if (canRetry) onRetry else null
        )
    }
}

@Composable
private fun OrdersSegmentedSelector(
    selectedTab: OrdersTab,
    onTabSelected: (OrdersTab) -> Unit,
    modifier: Modifier = Modifier
) {
Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrdersTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppRadius.extraLarge))
                        .clickable { onTabSelected(tab) }
                        .padding(AppSpacing.extraSmall)
                ) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(AppRadius.large),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {}
                    }
                    
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(selectedTab: OrdersTab, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = AppSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        val text = when (selectedTab) {
            OrdersTab.ACTIVE -> stringResource(R.string.orders_empty_active)
            OrdersTab.HISTORY -> stringResource(R.string.orders_empty_history)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EmptyStateIcon(R.drawable.ic_empty_receipt)
            Spacer(modifier = Modifier.height(AppSpacing.regular))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
