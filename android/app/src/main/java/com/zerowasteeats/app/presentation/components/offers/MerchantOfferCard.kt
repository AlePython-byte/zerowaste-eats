package com.zerowasteeats.app.presentation.components.offers

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.merchant.MerchantOffer
import com.zerowasteeats.app.data.merchant.MerchantOfferStatus
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.ENTRANCE_MILLIS
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.format.formatPesos
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Instant

/**
 * An offer in the merchant's list: photo, title, prices, stock and status. The three-dot menu
 * edits the offer and, while it is active or paused, pauses or resumes it.
 */
@Composable
fun MerchantOfferCard(
    offer: MerchantOffer,
    onEditClick: () -> Unit,
    /** Null when the offer cannot be paused or resumed (sold out, expired). */
    onToggleStatusClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Image(
                painter = painterResource(offer.photo.drawableRes),
                // Decorative: the title next to it says what the offer is
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(AppRadius.medium)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Text(
                        text = formatPesos(offer.discountedPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = formatPesos(offer.originalPrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Row(
                    modifier = Modifier.padding(top = AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    OfferStatusPill(status = offer.status)
                    if (offer.quantity > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.offer_remaining_count,
                                offer.quantity,
                                offer.quantity
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            OfferMenu(
                offerTitle = offer.title,
                isPaused = offer.status == MerchantOfferStatus.PAUSED,
                onEditClick = onEditClick,
                onToggleStatusClick = onToggleStatusClick
            )
        }
    }
}

@Composable
private fun OfferMenu(
    offerTitle: String,
    isPaused: Boolean,
    onEditClick: () -> Unit,
    onToggleStatusClick: (() -> Unit)?
) {
    var isOpen by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { isOpen = true },
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.merchant_offer_menu_cd, offerTitle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.merchant_offer_edit)) },
                onClick = {
                    isOpen = false
                    onEditClick()
                }
            )
            if (onToggleStatusClick != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isPaused) R.string.merchant_offer_resume
                                else R.string.merchant_offer_pause
                            )
                        )
                    },
                    onClick = {
                        isOpen = false
                        onToggleStatusClick()
                    }
                )
            }
        }
    }
}

/** Status as a coloured pill with its name, so the colour is never the only signal. */
@Composable
fun OfferStatusPill(status: MerchantOfferStatus, modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()

    // A short crossfade when the status changes, so pausing does not make the pill jump
    Crossfade(
        targetState = status,
        modifier = modifier,
        animationSpec = if (animationsEnabled) tween(ENTRANCE_MILLIS) else snap(),
        label = "offerStatusPill"
    ) { current ->
        val (container, content) = statusColors(current)
        Surface(shape = CircleShape, color = container) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = content) {}
                Text(
                    text = stringResource(statusLabelRes(current)),
                    style = MaterialTheme.typography.labelMedium,
                    color = content
                )
            }
        }
    }
}

@Composable
private fun statusColors(status: MerchantOfferStatus): Pair<Color, Color> = when (status) {
    MerchantOfferStatus.ACTIVE ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    MerchantOfferStatus.PAUSED, MerchantOfferStatus.EXPIRED ->
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    MerchantOfferStatus.SOLD_OUT ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

private fun statusLabelRes(status: MerchantOfferStatus): Int = when (status) {
    MerchantOfferStatus.ACTIVE -> R.string.merchant_offer_status_active
    MerchantOfferStatus.PAUSED -> R.string.merchant_offer_status_paused
    MerchantOfferStatus.SOLD_OUT -> R.string.merchant_offer_status_sold_out
    MerchantOfferStatus.EXPIRED -> R.string.merchant_offer_status_expired
}

private fun previewOffer(
    title: String,
    status: MerchantOfferStatus,
    quantity: Int,
    photo: OfferPhoto
) = MerchantOffer(
    id = title,
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

@Composable
private fun MerchantOfferCardPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AppSpacing.regular),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                MerchantOfferCard(
                    offer = previewOffer("Croissants del día", MerchantOfferStatus.ACTIVE, 6, OfferPhoto.CROISSANT),
                    onEditClick = {},
                    onToggleStatusClick = {}
                )
                MerchantOfferCard(
                    offer = previewOffer("Sándwich artesanal con un nombre muy largo", MerchantOfferStatus.PAUSED, 4, OfferPhoto.BURGER),
                    onEditClick = {},
                    onToggleStatusClick = {}
                )
                MerchantOfferCard(
                    offer = previewOffer("Caja de verduras", MerchantOfferStatus.SOLD_OUT, 0, OfferPhoto.VEGETABLES),
                    onEditClick = {},
                    onToggleStatusClick = null
                )
                MerchantOfferCard(
                    offer = previewOffer("Postres del lunes", MerchantOfferStatus.EXPIRED, 2, OfferPhoto.DESSERTS),
                    onEditClick = {},
                    onToggleStatusClick = null
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MerchantOfferCardLightPreview() {
    MerchantOfferCardPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantOfferCardDarkPreview() {
    MerchantOfferCardPreviewContent(ThemeMode.DARK)
}
