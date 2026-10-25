package com.zerowasteeats.app.presentation.screens.merchant.reservations

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.ModeBadge
import com.zerowasteeats.app.presentation.components.common.SegmentOption
import com.zerowasteeats.app.presentation.components.common.SegmentedCountControl
import com.zerowasteeats.app.presentation.components.common.offerListItem
import com.zerowasteeats.app.presentation.components.reservations.MerchantReservationCard
import com.zerowasteeats.app.presentation.navigation.LocalBottomBarInset
import com.zerowasteeats.app.presentation.navigation.aboveBottomBar
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
fun MerchantReservationsScreen(
    session: Session.Merchant,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = (context.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: MerchantReservationsViewModel = viewModel(
        factory = MerchantReservationsViewModelFactory(
            container.merchantRepositories(session).reservations
        )
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val confirmedMessage = stringResource(R.string.merchant_pickup_confirmed)
    val timeFormatter = rememberPickupTimeFormatter()
    // A hint with the right code, so the flow can be tried without looking it up
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    // A short vibration for each wrong code
    LaunchedEffect(viewModel) {
        viewModel.wrongCode.collect { haptics.performHapticFeedback(HapticFeedbackType.Reject) }
    }

    MerchantReservationsContent(
        state = state,
        timeFormatter = timeFormatter,
        snackbarHostState = snackbarHostState,
        onTabSelected = viewModel::selectTab,
        onConfirmClick = viewModel::openSheet,
        modifier = modifier
    )

    val sheet = viewModel.sheet
    // The reservation comes from the list, so the sheet follows it if it changes underneath
    val reservation = sheet?.let { open -> state.reservations.firstOrNull { it.id == open.reservationId } }
    if (sheet != null && reservation != null) {
        PickupCodeSheet(
            reservation = reservation,
            pickupTime = timeFormatter.format(reservation.pickupTime),
            state = sheet,
            nowMillis = viewModel.nowMillis,
            debugCode = if (isDebuggable) reservation.code else null,
            onCodeChange = viewModel::onCodeChange,
            onSubmit = viewModel::submit,
            onLockExpired = viewModel::onLockExpired,
            onDismiss = viewModel::closeSheet,
            onFinished = {
                viewModel.closeSheet()
                scope.launch { snackbarHostState.showSnackbar(confirmedMessage) }
            }
        )
    }
}

@Composable
fun MerchantReservationsContent(
    state: MerchantReservationListState,
    timeFormatter: DateTimeFormatter,
    snackbarHostState: SnackbarHostState,
    onTabSelected: (ReservationTab) -> Unit,
    onConfirmClick: (reservationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ids already shown, so a card that only moves between tabs does not enter again
    val seenIds = remember { mutableSetOf<String>() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.aboveBottomBar()) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = AppSpacing.regular,
                end = AppSpacing.regular,
                top = AppSpacing.regular,
                // The floating bar sits over the bottom of the list
                bottom = AppSpacing.regular + LocalBottomBarInset.current
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            item(key = "header") {
                Column {
                    ModeBadge()
                    Text(
                        text = stringResource(R.string.merchant_tab_reservations),
                        modifier = Modifier
                            .padding(top = AppSpacing.small)
                            .semantics { heading() },
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item(key = "tabs") {
                SegmentedCountControl(
                    options = ReservationTab.entries.map { tab ->
                        SegmentOption(stringResource(tabLabelRes(tab)), state.counts.getValue(tab))
                    },
                    selectedIndex = state.tab.ordinal,
                    onSelect = { index -> onTabSelected(ReservationTab.entries[index]) }
                )
            }

            if (state.visibleReservations.isEmpty()) {
                item(key = "empty-${state.tab}") { EmptyReservations(state.tab) }
            } else {
                items(state.visibleReservations, key = { it.id }) { reservation ->
                    MerchantReservationCard(
                        reservation = reservation,
                        pickupTime = timeFormatter.format(reservation.pickupTime),
                        onConfirmClick = if (reservation.status == MerchantReservationStatus.PENDING) {
                            { onConfirmClick(reservation.id) }
                        } else {
                            null
                        },
                        modifier = offerListItem(seenIds, reservation.id)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReservations(tab: ReservationTab) {
    when (tab) {
        ReservationTab.PENDING -> EmptyState(
            icon = R.drawable.ic_empty_calendar,
            title = stringResource(R.string.merchant_reservations_empty_pending_title),
            message = stringResource(R.string.merchant_reservations_empty_pending_message)
        )
        ReservationTab.COMPLETED -> EmptyState(
            icon = R.drawable.ic_empty_receipt,
            title = stringResource(R.string.merchant_reservations_empty_completed_title),
            message = stringResource(R.string.merchant_reservations_empty_completed_message)
        )
    }
}

private fun tabLabelRes(tab: ReservationTab): Int = when (tab) {
    ReservationTab.PENDING -> R.string.merchant_reservations_tab_pending
    ReservationTab.COMPLETED -> R.string.merchant_reservations_tab_completed
}

@Composable
private fun rememberPickupTimeFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
    }
}

private val previewBase: Instant = Instant.parse("2026-10-24T23:00:00Z")

private val previewReservations = listOf(
    MerchantReservation("1", "Croissants del día", "Laura Gómez", 2, previewBase, "PAN-4821", MerchantReservationStatus.PENDING),
    MerchantReservation("2", "Caja de postres surtidos", "Carlos Ruiz", 1, previewBase.plusSeconds(1800), "PAN-7305", MerchantReservationStatus.PENDING),
    MerchantReservation("3", "Croissants del día", "Marta Díaz", 3, previewBase.minusSeconds(3600), "PAN-1190", MerchantReservationStatus.COMPLETED)
)

@Composable
private fun ReservationsPreviewContent(themeMode: ThemeMode, reservations: List<MerchantReservation>) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantReservationsContent(
            state = MerchantReservationListState(reservations, ReservationTab.PENDING),
            timeFormatter = rememberPickupTimeFormatter(),
            snackbarHostState = remember { SnackbarHostState() },
            onTabSelected = {},
            onConfirmClick = {}
        )
    }
}

@Preview(name = "List light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ReservationsLightPreview() {
    ReservationsPreviewContent(ThemeMode.LIGHT, previewReservations)
}

@Preview(name = "List dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReservationsDarkPreview() {
    ReservationsPreviewContent(ThemeMode.DARK, previewReservations)
}

@Preview(name = "Empty light", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ReservationsEmptyLightPreview() {
    ReservationsPreviewContent(ThemeMode.LIGHT, emptyList())
}

@Preview(name = "Empty dark", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReservationsEmptyDarkPreview() {
    ReservationsPreviewContent(ThemeMode.DARK, emptyList())
}
