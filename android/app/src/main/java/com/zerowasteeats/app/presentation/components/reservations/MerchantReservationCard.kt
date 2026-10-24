package com.zerowasteeats.app.presentation.components.reservations

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Instant

/**
 * A reservation in the business's list. It never shows the pickup code: the customer shows it
 * and the business types it, which is the whole check. A pending one offers to confirm it.
 */
@Composable
fun MerchantReservationCard(
    reservation: MerchantReservation,
    pickupTime: String,
    /** Null for a completed reservation. */
    onConfirmClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.regular)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservation.offerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reservation.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.merchant_pickup_quantity,
                        reservation.quantity,
                        reservation.quantity
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pickupTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (reservation.status == MerchantReservationStatus.COMPLETED) {
                    Surface(
                        modifier = Modifier.padding(top = AppSpacing.small),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.merchant_reservation_delivered),
                            modifier = Modifier.padding(
                                horizontal = AppSpacing.medium,
                                vertical = AppSpacing.extraSmall
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (onConfirmClick != null) {
            AppButton(
                text = stringResource(R.string.merchant_confirm_pickup),
                onClick = onConfirmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.medium)
            )
        }
    }
}

private fun previewReservation(status: MerchantReservationStatus) = MerchantReservation(
    id = status.name,
    offerTitle = "Croissants del día",
    customerName = "Laura Gómez",
    quantity = 2,
    pickupTime = Instant.EPOCH,
    code = "PAN-4821",
    status = status
)

@Composable
private fun MerchantReservationCardPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AppSpacing.regular),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                MerchantReservationCard(
                    reservation = previewReservation(MerchantReservationStatus.PENDING),
                    pickupTime = "18:15",
                    onConfirmClick = {}
                )
                MerchantReservationCard(
                    reservation = previewReservation(MerchantReservationStatus.COMPLETED),
                    pickupTime = "12:30",
                    onConfirmClick = null
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MerchantReservationCardLightPreview() {
    MerchantReservationCardPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantReservationCardDarkPreview() {
    MerchantReservationCardPreviewContent(ThemeMode.DARK)
}
