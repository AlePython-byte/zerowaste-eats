package com.zerowasteeats.app.presentation.screens.offerdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.ProvideSkeletonShimmer
import com.zerowasteeats.app.presentation.components.common.skeletonShimmer
import com.zerowasteeats.app.presentation.screens.feed.FavoriteProblemSnackbar
import com.zerowasteeats.app.presentation.screens.feed.LoadFailureSnackbar
import com.zerowasteeats.app.presentation.theme.PhotoScrim
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCream

/**
 * The detail of offer [offerId]. What the list already knew shows at once while the server is
 * asked; placeholders only when nothing is known yet, and a clear message when the offer can no
 * longer be reserved.
 */
@Composable
fun OfferDetailScreen(
    session: Session.Customer,
    offerId: String,
    onBackClick: () -> Unit,
    onViewOrders: () -> Unit
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: OfferDetailViewModel = viewModel(
        factory = OfferDetailViewModelFactory(
            offerId,
            container.offerCatalog(session),
            container.customerReservations(session),
            container.customerLocation
        )
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val messages = mapOf(
        CatalogError.NETWORK to stringResource(R.string.offer_detail_load_failed_network),
        CatalogError.SESSION to stringResource(R.string.catalog_session_failed),
        CatalogError.UNEXPECTED to stringResource(R.string.offer_detail_load_failed)
    )
    LoadFailureSnackbar(
        error = state.loadError,
        hasContent = state.offer != null,
        snackbarHostState = snackbarHostState,
        messageFor = messages::getValue,
        retryLabel = stringResource(R.string.auth_retry),
        onRetry = viewModel::retry
    )
    FavoriteProblemSnackbar(
        problems = viewModel.favoriteFailures,
        snackbarHostState = snackbarHostState,
        onRetry = { _, favorite -> viewModel.setFavorite(favorite) }
    )

    ReserveProblemSnackbar(
        problems = viewModel.reserveFailures,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::reserve
    )

    val offer = state.offer
    when {
        state.showNotAvailable -> OfferNotAvailable(onBackClick = onBackClick)
        offer != null -> OfferDetailContent(
            offer = offer,
            reserve = state.toPanel(),
            onBackClick = onBackClick,
            onFavoriteClick = viewModel::toggleFavorite,
            onDecreaseQuantity = viewModel::decreaseQuantity,
            onIncreaseQuantity = viewModel::increaseQuantity,
            onReserveClick = viewModel::reserve,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        )
        else -> OfferDetailSkeleton(
            onBackClick = onBackClick,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        )
    }

    state.reserve.reserved?.let { reservation ->
        ReservedSheet(
            reservation = reservation,
            onViewOrders = {
                viewModel.dismissReserved()
                onViewOrders()
            },
            onDismiss = viewModel::dismissReserved
        )
    }
}

/**
 * Says why a reservation was not made. Running out of units needs no action (the offer was
 * loaded again); a network or server failure offers to try again; an ended session only says
 * to sign in again.
 */
@Composable
private fun ReserveProblemSnackbar(
    problems: Flow<ReserveProblem>,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit
) {
    val messages = mapOf(
        ReserveProblem.NOT_ENOUGH_UNITS to stringResource(R.string.reserve_failed_not_enough_units),
        ReserveProblem.NETWORK to stringResource(R.string.reserve_failed_network),
        ReserveProblem.SESSION to stringResource(R.string.reserve_failed_session),
        ReserveProblem.UNEXPECTED to stringResource(R.string.reserve_failed)
    )
    val retryLabel = stringResource(R.string.auth_retry)
    val currentMessages by rememberUpdatedState(messages)
    val currentOnRetry by rememberUpdatedState(onRetry)

    LaunchedEffect(problems) {
        problems.collect { problem ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val canRetry = problem == ReserveProblem.NETWORK || problem == ReserveProblem.UNEXPECTED
            val result = snackbarHostState.showSnackbar(
                message = currentMessages.getValue(problem),
                actionLabel = if (canRetry) retryLabel else null,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) currentOnRetry()
        }
    }
}

/** The offer can no longer be reserved: say so, and send the person back to the others. */
@Composable
private fun OfferNotAvailable(onBackClick: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EmptyState(
                title = stringResource(R.string.offer_not_available_title),
                message = stringResource(R.string.offer_not_available_subtitle),
                modifier = Modifier.align(Alignment.Center),
                icon = R.drawable.ic_empty_tag,
                actionText = stringResource(R.string.common_back_cd),
                onActionClick = onBackClick
            )
        }
    }
}

/**
 * The page's shape while the offer loads: the hero and a few lines, in the theme's quiet surface
 * color, with the back button already working. Screen readers hear that it is loading.
 */
@Composable
private fun OfferDetailSkeleton(
    onBackClick: () -> Unit,
    snackbarHost: @Composable () -> Unit
) {
    ProvideSkeletonShimmer(active = true) {
        OfferDetailSkeletonContent(onBackClick, snackbarHost)
    }
}

@Composable
private fun OfferDetailSkeletonContent(
    onBackClick: () -> Unit,
    snackbarHost: @Composable () -> Unit
) {
    val block = MaterialTheme.colorScheme.surfaceVariant
    val lineShape = RoundedCornerShape(8.dp)
    val loading = stringResource(R.string.offer_detail_loading)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .semantics { contentDescription = loading }
            ) {
                Column(
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .skeletonShimmer()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .background(block)
                    )
                    Column(
                        modifier = Modifier.padding(AppSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                    ) {
                        Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(lineShape).background(block))
                        Box(Modifier.fillMaxWidth(0.85f).height(32.dp).clip(lineShape).background(block))
                        Box(Modifier.fillMaxWidth(0.6f).height(16.dp).clip(lineShape).background(block))
                        Spacer(Modifier.height(AppSpacing.small))
                        Box(Modifier.fillMaxWidth(0.35f).height(40.dp).clip(lineShape).background(block))
                    }
                }
            }

            // Back works before anything arrives
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(AppSpacing.regular)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RescateCarbon.copy(alpha = PhotoScrim.CONTROL_ALPHA))
                    .clickable(role = Role.Button, onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                val backDescription = stringResource(R.string.common_back_cd)
                Text(
                    text = "←",
                    fontSize = 22.sp,
                    color = RescateCream,
                    modifier = Modifier.semantics { contentDescription = backDescription }
                )
            }
        }
    }
}
