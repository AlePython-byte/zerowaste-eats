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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.zerowasteeats.app.presentation.components.common.rememberSkeletonAlpha
import com.zerowasteeats.app.presentation.screens.feed.FavoriteProblemSnackbar
import com.zerowasteeats.app.presentation.screens.feed.LoadFailureSnackbar
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
    onReserveClick: () -> Unit
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: OfferDetailViewModel = viewModel(
        factory = OfferDetailViewModelFactory(
            offerId,
            container.offerCatalog(session),
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

    val offer = state.offer
    when {
        state.showNotAvailable -> OfferNotAvailable(onBackClick = onBackClick)
        offer != null -> OfferDetailContent(
            offer = offer,
            onBackClick = onBackClick,
            onFavoriteClick = viewModel::toggleFavorite,
            onReserveClick = onReserveClick,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        )
        else -> OfferDetailSkeleton(
            onBackClick = onBackClick,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        )
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
    val alpha = rememberSkeletonAlpha()
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
                        .alpha(alpha)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .background(block)
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(lineShape).background(block))
                        Box(Modifier.fillMaxWidth(0.85f).height(32.dp).clip(lineShape).background(block))
                        Box(Modifier.fillMaxWidth(0.6f).height(16.dp).clip(lineShape).background(block))
                        Spacer(Modifier.height(8.dp))
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
                    .background(RescateCarbon.copy(alpha = 0.55f))
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
