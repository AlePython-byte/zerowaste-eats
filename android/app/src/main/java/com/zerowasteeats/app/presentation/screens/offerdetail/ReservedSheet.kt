package com.zerowasteeats.app.presentation.screens.offerdetail

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.reservations.CustomerReservation
import com.zerowasteeats.app.data.reservations.CustomerReservationStatus
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.format.formatPesos
import com.zerowasteeats.app.presentation.format.formatted
import com.zerowasteeats.app.presentation.model.pickupWindowOf
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.launch

/**
 * Tells the person the reservation is made and shows the pickup code, big and spaced out so it
 * reads at a glance from across a counter. [onViewOrders] goes to the orders, where the code
 * stays; [onDismiss] just closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservedSheet(
    reservation: CustomerReservation,
    onViewOrders: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(reservation.id) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    /** Slides the sheet away before acting, so it does not vanish abruptly. */
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) } // the content pads for the nav bar
    ) {
        ReservedSheetContent(
            reservation = reservation,
            onViewOrders = { closeThen(onViewOrders) },
            onDone = { closeThen(onDismiss) }
        )
    }
}

@Composable
internal fun ReservedSheetContent(
    reservation: CustomerReservation,
    onViewOrders: () -> Unit,
    onDone: () -> Unit,
    clock: Clock = Clock.systemDefaultZone()
) {
    val animate = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val check = remember { Animatable(if (animate) 0f else 1f) }
    val code = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        launch {
            check.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        code.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    val quantity = pluralStringResource(R.plurals.merchant_pickup_quantity, reservation.quantity, reservation.quantity)
    val window = pickupWindowOf(reservation.pickupStart, reservation.pickupEnd, clock)
        .formatted(withPickupPrefix = true)
    val codeDescription = stringResource(R.string.reserved_code_cd, spokenCode(reservation.code))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.regular)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)
    ) {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    alpha = check.value.coerceIn(0f, 1f)
                    val scale = 0.6f + 0.4f * check.value
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.padding(AppSpacing.regular),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Text(
            text = stringResource(R.string.reserved_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // The code: the one thing to remember from this sheet
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = code.value
                    translationY = (1f - code.value) * 12.dp.toPx()
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = AppSpacing.large, horizontal = AppSpacing.regular)
                    .clearAndSetSemantics { contentDescription = codeDescription },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.reserved_code_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = reservation.code,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = reservation.offerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${reservation.businessName} · $quantity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = window,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.reserved_total, formatPesos(reservation.totalPrice)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = stringResource(R.string.reserved_code_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        AppButton(
            text = stringResource(R.string.reserved_view_orders),
            onClick = onViewOrders,
            modifier = Modifier.fillMaxWidth()
        )
        AppButton(
            text = stringResource(R.string.reserved_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.SECONDARY
        )
    }
}

/** "ZW-7KQ3" read one character at a time, so a screen reader does not try to say it as a word. */
internal fun spokenCode(code: String): String =
    code.filter { it.isLetterOrDigit() }.toCharArray().joinToString(" ")

private val previewReservation = CustomerReservation(
    id = "r-1",
    code = "ZW-7KQ3",
    status = CustomerReservationStatus.PENDING,
    quantity = 2,
    unitPrice = 8_900,
    totalPrice = 17_800,
    originalUnitPrice = 15_000,
    createdAt = Instant.EPOCH,
    offerId = "o-1",
    offerTitle = "Croissants del día",
    businessName = "Panadería Artesanal",
    pickupStart = Instant.parse("2026-11-08T23:00:00Z"),
    pickupEnd = Instant.parse("2026-11-09T00:30:00Z")
)

@Composable
private fun ReservedPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ReservedSheetContent(reservation = previewReservation, onViewOrders = {}, onDone = {})
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ReservedLightPreview() {
    ReservedPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReservedDarkPreview() {
    ReservedPreviewContent(ThemeMode.DARK)
}
