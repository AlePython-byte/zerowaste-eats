package com.zerowasteeats.app.presentation.screens.merchant.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.merchant.MerchantMetrics
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.MetricCard
import com.zerowasteeats.app.presentation.components.common.ModeBadge
import com.zerowasteeats.app.presentation.components.common.staggeredEntrance
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MerchantDashboardScreen(
    session: Session.Merchant,
    onPublishOffer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: MerchantDashboardViewModel = viewModel(
        factory = MerchantDashboardViewModelFactory(container.merchantRepositories(session))
    )
    val state by viewModel.state.collectAsState()

    // The sections stagger in the first time only: coming back to this tab restores the flag,
    // so they do not replay on every tab switch
    val entranceDone = rememberSaveable { mutableStateOf(false) }
    val animateEntrance = remember { !entranceDone.value }
    LaunchedEffect(Unit) { entranceDone.value = true }

    MerchantDashboardContent(
        state = state,
        onPublishOffer = onPublishOffer,
        modifier = modifier,
        animateEntrance = animateEntrance
    )
}

@Composable
fun MerchantDashboardContent(
    state: MerchantDashboardState,
    onPublishOffer: () -> Unit,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.regular),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraLarge)
        ) {
            Column(modifier = Modifier.staggeredEntrance(0, animateEntrance)) {
                ModeBadge()
                Text(
                    text = stringResource(R.string.merchant_dashboard_greeting, state.businessName),
                    modifier = Modifier
                        .padding(top = AppSpacing.small)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!state.isVerified) {
                ReviewNotice(modifier = Modifier.staggeredEntrance(1, animateEntrance))
            }

            if (!state.hasData) {
                EmptyState(
                    icon = R.drawable.ic_empty_bag,
                    title = stringResource(R.string.merchant_dashboard_empty_title),
                    message = stringResource(R.string.merchant_dashboard_empty_message),
                    modifier = Modifier.staggeredEntrance(2, animateEntrance),
                    actionText = stringResource(R.string.merchant_publish_offer),
                    onActionClick = onPublishOffer
                )
            } else {
                MetricsSection(
                    metrics = state.metrics,
                    modifier = Modifier.staggeredEntrance(2, animateEntrance)
                )
                AppButton(
                    text = stringResource(R.string.merchant_publish_offer),
                    onClick = onPublishOffer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(3, animateEntrance)
                )
                UpcomingPickupsSection(
                    pickups = state.upcomingPickups,
                    modifier = Modifier.staggeredEntrance(4, animateEntrance)
                )
            }
        }
    }
}

/** Informative only: an unverified business can still use everything. */
@Composable
private fun ReviewNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.large),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(AppSpacing.regular)) {
            Text(
                text = stringResource(R.string.merchant_review_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.merchant_review_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MetricsSection(metrics: MerchantMetrics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            MetricCard(
                label = stringResource(R.string.merchant_metric_active_offers),
                value = metrics.activeOffers.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.merchant_metric_pending_reservations),
                value = metrics.pendingReservations.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        MetricCard(
            label = stringResource(R.string.merchant_metric_rescued_meals),
            value = metrics.rescuedMeals.toString(),
            modifier = Modifier.fillMaxWidth(),
            isHighlight = true
        )
    }
}

@Composable
private fun UpcomingPickupsSection(
    pickups: List<MerchantReservation>,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.merchant_upcoming_pickups),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (pickups.isEmpty()) {
            Text(
                text = stringResource(R.string.merchant_upcoming_empty),
                modifier = Modifier.padding(top = AppSpacing.small),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.padding(top = AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                pickups.forEach { reservation ->
                    PickupRow(reservation, timeFormatter)
                }
            }
        }
    }
}

@Composable
private fun PickupRow(reservation: MerchantReservation, timeFormatter: DateTimeFormatter) {
    val quantity = pluralStringResource(
        R.plurals.merchant_pickup_quantity,
        reservation.quantity,
        reservation.quantity
    )
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.regular)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservation.offerTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${reservation.customerName} · $quantity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeFormatter.format(reservation.pickupTime),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

private val previewBase: Instant = Instant.parse("2026-10-24T23:00:00Z")

private fun previewReservation(id: String, offer: String, customer: String, quantity: Int, minutes: Long) =
    MerchantReservation(
        id = id,
        offerTitle = offer,
        customerName = customer,
        quantity = quantity,
        pickupTime = previewBase.plusSeconds(minutes * 60),
        code = "PAN-$id",
        status = MerchantReservationStatus.PENDING
    )

@Composable
private fun DashboardPreviewContent(themeMode: ThemeMode, state: MerchantDashboardState) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantDashboardContent(state = state, onPublishOffer = {})
    }
}

private val previewFullState = MerchantDashboardState(
    businessName = "Panadería Demo",
    isVerified = false,
    metrics = MerchantMetrics(activeOffers = 2, pendingReservations = 2, rescuedMeals = 3),
    upcomingPickups = listOf(
        previewReservation("1", "Croissants del día", "Laura Gómez", 2, 15),
        previewReservation("2", "Caja de postres surtidos", "Carlos Ruiz", 1, 45)
    ),
    hasData = true
)

private val previewEmptyState = MerchantDashboardState(
    businessName = "Panadería Sol",
    isVerified = false,
    metrics = MerchantMetrics(0, 0, 0),
    upcomingPickups = emptyList(),
    hasData = false
)

@Preview(name = "Full light", widthDp = 380, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DashboardFullLightPreview() {
    DashboardPreviewContent(ThemeMode.LIGHT, previewFullState)
}

@Preview(name = "Full dark", widthDp = 380, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardFullDarkPreview() {
    DashboardPreviewContent(ThemeMode.DARK, previewFullState.copy(isVerified = true))
}

@Preview(name = "Empty light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DashboardEmptyLightPreview() {
    DashboardPreviewContent(ThemeMode.LIGHT, previewEmptyState)
}

@Preview(name = "Empty dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardEmptyDarkPreview() {
    DashboardPreviewContent(ThemeMode.DARK, previewEmptyState.copy(isVerified = true))
}
