package com.zerowasteeats.app.presentation.screens.merchant.offers

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferStatus
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.ModeBadge
import com.zerowasteeats.app.presentation.components.common.offerListItem
import com.zerowasteeats.app.presentation.components.filters.CountFilterChip
import com.zerowasteeats.app.presentation.components.offers.MerchantOfferCard
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.screens.merchant.editor.OfferSaveResult
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Instant
import kotlinx.coroutines.launch

@Composable
fun MerchantOffersScreen(
    session: Session.Merchant,
    onNewOffer: () -> Unit,
    onEditOffer: (offerId: String) -> Unit,
    savedNotice: OfferSavedNotice,
    modifier: Modifier = Modifier
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: MerchantOfferListViewModel = viewModel(
        factory = MerchantOfferListViewModelFactory(
            container.merchantRepositories(session).offers
        )
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pausedMessage = stringResource(R.string.merchant_offer_paused_message)
    val resumedMessage = stringResource(R.string.merchant_offer_resumed_message)
    val undoLabel = stringResource(R.string.common_undo)
    val publishedMessage = stringResource(R.string.offer_published_message)
    val savedMessage = stringResource(R.string.offer_saved_message)

    // Coming back from the editor: say what was saved, once. Keyed on the notice itself, not
    // on its value: consuming it clears the value, which would otherwise restart the effect
    // and cancel the snackbar it just opened.
    LaunchedEffect(savedNotice) {
        snapshotFlow { savedNotice.pending }.collect {
            when (savedNotice.consume()) {
                OfferSaveResult.CREATED -> {
                    // A new offer is active, so show the group it landed in
                    viewModel.selectFilter(OfferFilter.ACTIVE)
                    snackbarHostState.showSnackbar(publishedMessage)
                }
                OfferSaveResult.UPDATED -> snackbarHostState.showSnackbar(savedMessage)
                OfferSaveResult.NOT_FOUND, null -> Unit
            }
        }
    }

    // Each pause or resume says so and offers to take it back. A newer one replaces the
    // snackbar of the one before, instead of queueing behind it.
    LaunchedEffect(viewModel) {
        viewModel.statusChanged.collect { event ->
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = if (event.change == OfferStatusChange.PAUSE) pausedMessage else resumedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
            }
        }
    }

    MerchantOffersContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onFilterSelected = viewModel::selectFilter,
        onNewOffer = onNewOffer,
        onEditOffer = onEditOffer,
        onToggleStatus = viewModel::toggleStatus,
        modifier = modifier
    )
}

@Composable
fun MerchantOffersContent(
    state: MerchantOfferListState,
    snackbarHostState: SnackbarHostState,
    onFilterSelected: (OfferFilter) -> Unit,
    onNewOffer: () -> Unit,
    onEditOffer: (offerId: String) -> Unit,
    onToggleStatus: (MerchantOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ids already shown, so a card that only moves between groups does not enter again
    val seenOfferIds = remember { mutableSetOf<String>() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewOffer,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
                Text(
                    text = stringResource(R.string.merchant_new_offer),
                    modifier = Modifier.padding(start = AppSpacing.small),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = AppSpacing.regular,
                end = AppSpacing.regular,
                top = AppSpacing.regular,
                // Room for the button, so the last card can scroll clear of it
                bottom = FAB_CLEARANCE
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            item(key = "header") {
                Column {
                    ModeBadge()
                    Text(
                        text = stringResource(R.string.merchant_tab_offers),
                        modifier = Modifier
                            .padding(top = AppSpacing.small)
                            .semantics { heading() },
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item(key = "filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    OfferFilter.entries.forEach { filter ->
                        CountFilterChip(
                            text = stringResource(filterLabelRes(filter)),
                            count = state.counts.getValue(filter),
                            isSelected = state.filter == filter,
                            onClick = { onFilterSelected(filter) }
                        )
                    }
                }
            }

            if (state.visibleOffers.isEmpty()) {
                item(key = "empty-${state.filter}") {
                    EmptyOffers(filter = state.filter, onNewOffer = onNewOffer)
                }
            } else {
                items(state.visibleOffers, key = { it.id }) { offer ->
                    MerchantOfferCard(
                        offer = offer,
                        onEditClick = { onEditOffer(offer.id) },
                        onToggleStatusClick = if (OfferStatusChange.offeredFor(offer) != null) {
                            { onToggleStatus(offer) }
                        } else {
                            null
                        },
                        modifier = offerListItem(seenOfferIds, offer.id)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOffers(filter: OfferFilter, onNewOffer: () -> Unit) {
    when (filter) {
        OfferFilter.ACTIVE -> EmptyState(
            title = stringResource(R.string.merchant_offers_empty_active_title),
            message = stringResource(R.string.merchant_offers_empty_active_message),
            actionText = stringResource(R.string.merchant_publish_offer),
            onActionClick = onNewOffer
        )
        OfferFilter.PAUSED -> EmptyState(
            title = stringResource(R.string.merchant_offers_empty_paused_title),
            message = stringResource(R.string.merchant_offers_empty_paused_message)
        )
        OfferFilter.SOLD_OUT -> EmptyState(
            title = stringResource(R.string.merchant_offers_empty_sold_out_title),
            message = stringResource(R.string.merchant_offers_empty_sold_out_message)
        )
    }
}

private fun filterLabelRes(filter: OfferFilter): Int = when (filter) {
    OfferFilter.ACTIVE -> R.string.merchant_filter_active
    OfferFilter.PAUSED -> R.string.merchant_filter_paused
    OfferFilter.SOLD_OUT -> R.string.merchant_filter_sold_out
}

private val FAB_CLEARANCE = AppSpacing.screen * 2 + AppSpacing.extraLarge

private fun previewOffer(
    id: String,
    title: String,
    status: MerchantOfferStatus,
    quantity: Int,
    photo: OfferPhoto
) = MerchantOffer(
    id = id,
    title = title,
    category = OfferCategory.BAKERY,
    originalPrice = 15_000,
    discountedPrice = 8_900,
    quantity = quantity,
    pickupStart = Instant.EPOCH,
    pickupEnd = Instant.EPOCH.plusSeconds(3600),
    status = status,
    photo = photo
)

private val previewOffers = listOf(
    previewOffer("1", "Croissants del día", MerchantOfferStatus.ACTIVE, 6, OfferPhoto.CROISSANT),
    previewOffer("2", "Caja de postres surtidos", MerchantOfferStatus.ACTIVE, 3, OfferPhoto.DESSERTS),
    previewOffer("3", "Sándwich artesanal", MerchantOfferStatus.PAUSED, 4, OfferPhoto.BURGER),
    previewOffer("4", "Caja de verduras", MerchantOfferStatus.SOLD_OUT, 0, OfferPhoto.VEGETABLES)
)

@Composable
private fun OffersPreviewContent(themeMode: ThemeMode, offers: List<MerchantOffer>) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantOffersContent(
            state = MerchantOfferListState(offers, OfferFilter.ACTIVE),
            snackbarHostState = remember { SnackbarHostState() },
            onFilterSelected = {},
            onNewOffer = {},
            onEditOffer = {},
            onToggleStatus = {}
        )
    }
}

@Preview(name = "List light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun OffersLightPreview() {
    OffersPreviewContent(ThemeMode.LIGHT, previewOffers)
}

@Preview(name = "List dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OffersDarkPreview() {
    OffersPreviewContent(ThemeMode.DARK, previewOffers)
}

@Preview(name = "Empty light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun OffersEmptyLightPreview() {
    OffersPreviewContent(ThemeMode.LIGHT, emptyList())
}

@Preview(name = "Empty dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OffersEmptyDarkPreview() {
    OffersPreviewContent(ThemeMode.DARK, emptyList())
}
