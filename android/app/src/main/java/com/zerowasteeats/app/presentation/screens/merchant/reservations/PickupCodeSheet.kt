package com.zerowasteeats.app.presentation.screens.merchant.reservations

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppTextField
import com.zerowasteeats.app.presentation.components.common.ENTRANCE_MILLIS
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Instant
import kotlin.math.ceil
import kotlinx.coroutines.delay

private const val SECOND_MILLIS = 1_000L

/**
 * The sheet where the business types the code the customer shows. [debugCode] is the right
 * code, shown as a hint only in debug builds. [onFinished] runs once a confirmed pickup has
 * played its check and the sheet has slid away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupCodeSheet(
    reservation: MerchantReservation,
    pickupTime: String,
    state: PickupSheetState,
    nowMillis: () -> Long,
    debugCode: String?,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLockExpired: () -> Unit,
    onDismiss: () -> Unit,
    onFinished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val animationsEnabled = rememberAnimationsEnabled()

    // Right code: let the check play, then slide the sheet away and report it
    LaunchedEffect(state.isConfirmed) {
        if (!state.isConfirmed) return@LaunchedEffect
        if (animationsEnabled) delay(ENTRANCE_MILLIS * 2L)
        sheetState.hide()
        onFinished()
    }

    ModalBottomSheet(
        // Dismissing during the check still counts as done: the pickup is already confirmed
        onDismissRequest = { if (state.isConfirmed) onFinished() else onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) } // the content pads for the nav bar
    ) {
        PickupCodeSheetContent(
            reservation = reservation,
            pickupTime = pickupTime,
            state = state,
            nowMillis = nowMillis,
            debugCode = debugCode,
            onCodeChange = onCodeChange,
            onSubmit = onSubmit,
            onLockExpired = onLockExpired
        )
    }
}

@Composable
internal fun PickupCodeSheetContent(
    reservation: MerchantReservation,
    pickupTime: String,
    state: PickupSheetState,
    nowMillis: () -> Long,
    debugCode: String?,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLockExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.regular),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)
    ) {
        Text(
            text = stringResource(R.string.merchant_confirm_pickup),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        ReservationSummary(reservation = reservation, pickupTime = pickupTime)

        if (state.isConfirmed) {
            ConfirmedCheck()
        } else {
            CodeEntry(
                state = state,
                nowMillis = nowMillis,
                debugCode = debugCode,
                onCodeChange = onCodeChange,
                onSubmit = onSubmit,
                onLockExpired = onLockExpired
            )
        }
    }
}

@Composable
private fun ReservationSummary(reservation: MerchantReservation, pickupTime: String) {
    val quantity = pluralStringResource(
        R.plurals.merchant_pickup_quantity,
        reservation.quantity,
        reservation.quantity
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(AppSpacing.regular)) {
            Text(
                text = reservation.offerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${reservation.customerName} · $quantity · $pickupTime",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CodeEntry(
    state: PickupSheetState,
    nowMillis: () -> Long,
    debugCode: String?,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLockExpired: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isPreview = LocalInspectionMode.current

    // The keyboard opens by itself: typing the code is the only thing to do here
    LaunchedEffect(Unit) {
        if (isPreview) return@LaunchedEffect
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Ticks once a second while locked, and reopens the field when the time is up
    val lockedUntil = state.lockedUntilMillis
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(lockedUntil) {
        if (lockedUntil == null) return@LaunchedEffect
        while (true) {
            now = nowMillis()
            if (now >= lockedUntil) {
                onLockExpired()
                break
            }
            delay(SECOND_MILLIS)
        }
    }
    val isLocked = lockedUntil != null && now < lockedUntil
    val secondsLeft = if (lockedUntil != null) {
        ceil((lockedUntil - now) / SECOND_MILLIS.toDouble()).toInt().coerceAtLeast(1)
    } else {
        0
    }

    val error = when {
        // Shown under the field instead: a disabled field greys out its own message
        isLocked -> null
        state.error == PickupCodeError.WRONG ->
            pluralStringResource(R.plurals.merchant_code_wrong, state.attemptsLeft, state.attemptsLeft)
        state.error == PickupCodeError.ALREADY_COMPLETED ->
            stringResource(R.string.merchant_code_already_completed)
        else -> null
    }

    Text(
        text = stringResource(R.string.merchant_code_instructions),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    AppTextField(
        value = state.code,
        onValueChange = onCodeChange,
        label = stringResource(R.string.merchant_code_label),
        modifier = Modifier.focusRequester(focusRequester),
        error = error,
        enabled = !isLocked && !state.isChecking,
        keyboardType = KeyboardType.Ascii,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        // Monospaced and spaced out, so each character is easy to compare with the customer's
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp
        ),
        capitalization = KeyboardCapitalization.Characters
    )
    if (isLocked) {
        Text(
            text = pluralStringResource(R.plurals.merchant_code_locked, secondsLeft, secondsLeft),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
    }
    if (debugCode != null) {
        Text(
            text = stringResource(R.string.merchant_code_debug_hint, debugCode),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    AppButton(
        text = stringResource(R.string.merchant_code_confirm),
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.code.isNotBlank() && !isLocked,
        isLoading = state.isChecking
    )
}

/** A check that grows in, then the sheet closes. Static when animations are off. */
@Composable
private fun ConfirmedCheck() {
    val animate = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(ENTRANCE_MILLIS, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.large)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = progress.value
                val scale = 0.6f + 0.4f * progress.value
                scaleX = scale
                scaleY = scale
            }
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
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
        }
        Text(
            text = stringResource(R.string.merchant_pickup_confirmed),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private val previewReservation = MerchantReservation(
    id = "r-1",
    offerTitle = "Croissants del día",
    customerName = "Laura Gómez",
    quantity = 2,
    pickupTime = Instant.EPOCH,
    code = "PAN-4821",
    status = MerchantReservationStatus.PENDING
)

@Composable
private fun SheetPreviewContent(themeMode: ThemeMode, state: PickupSheetState) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PickupCodeSheetContent(
                reservation = previewReservation,
                pickupTime = "18:15",
                state = state,
                nowMillis = { 0L },
                debugCode = "PAN-4821",
                onCodeChange = {},
                onSubmit = {},
                onLockExpired = {}
            )
        }
    }
}

@Preview(name = "Wrong code light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SheetWrongLightPreview() {
    SheetPreviewContent(
        ThemeMode.LIGHT,
        PickupSheetState("r-1", code = "PAN-0000", error = PickupCodeError.WRONG, attemptsLeft = 3)
    )
}

@Preview(name = "Wrong code dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetWrongDarkPreview() {
    SheetPreviewContent(
        ThemeMode.DARK,
        PickupSheetState("r-1", code = "PAN-0000", error = PickupCodeError.WRONG, attemptsLeft = 3)
    )
}

@Preview(name = "Locked light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SheetLockedLightPreview() {
    SheetPreviewContent(ThemeMode.LIGHT, PickupSheetState("r-1", lockedUntilMillis = 25_000L))
}

@Preview(name = "Locked dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetLockedDarkPreview() {
    SheetPreviewContent(ThemeMode.DARK, PickupSheetState("r-1", lockedUntilMillis = 25_000L))
}

@Preview(name = "Confirmed light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SheetConfirmedLightPreview() {
    SheetPreviewContent(ThemeMode.LIGHT, PickupSheetState("r-1", code = "PAN-4821", isConfirmed = true))
}

@Preview(name = "Confirmed dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetConfirmedDarkPreview() {
    SheetPreviewContent(ThemeMode.DARK, PickupSheetState("r-1", code = "PAN-4821", isConfirmed = true))
}
