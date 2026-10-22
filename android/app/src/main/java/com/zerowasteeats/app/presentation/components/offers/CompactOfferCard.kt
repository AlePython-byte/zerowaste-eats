package com.zerowasteeats.app.presentation.components.offers

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.data.offerPhotoRes
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.navigation.sharedOfferImage
import com.zerowasteeats.app.presentation.theme.RescateButter
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCarbonElevated
import com.zerowasteeats.app.presentation.theme.RescateCoral
import com.zerowasteeats.app.presentation.theme.RescateCream
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * Compact offer card for a two-column grid: a 4:5 photo with the discount, favorite and
 * the essentials (merchant, title, price) floating over a dark gradient at the bottom.
 * The photo shares the same transition key as [OfferCard], so it flies into the detail.
 */
@Composable
fun CompactOfferCard(
    offer: OfferUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    favoriteContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val photo = offerPhotoRes(offer)
    val cardShape = RoundedCornerShape(24.dp)
    val fallbackBrush = Brush.linearGradient(listOf(RescateCarbonElevated, RescateCarbon))
    val summary = stringResource(
        R.string.offer_card_summary_cd,
        offer.title,
        offer.merchantName,
        offer.discountedPrice,
        offer.discountPercent
    )

    // Press feedback: the card shrinks slightly instead of showing a ripple
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = rememberAnimationsEnabled()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "compactOfferCardPressScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(cardShape)
            .background(fallbackBrush)
            // One spoken summary for the card; the favorite button keeps its own semantics
            .semantics { contentDescription = summary }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        if (photo != null) {
            Image(
                painter = painterResource(id = photo),
                contentDescription = null, // described by the card summary
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedOfferImage(offer.id)
            )
        }

        // Dark gradient so the text stays readable over any photo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to RescateCarbon.copy(alpha = 0.10f),
                        0.7f to RescateCarbon.copy(alpha = 0.72f),
                        1f to RescateCarbon.copy(alpha = 0.96f)
                    )
                )
        )

        // Top row: discount + favorite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(RescateCoral)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clearAndSetSemantics {}
            ) {
                Text(
                    text = stringResource(R.string.offer_discount_off, offer.discountPercent),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = RescateCream
                )
            }

            FavoriteButton(
                isFavorite = offer.isFavorite,
                contentDescription = favoriteContentDescription,
                onClick = onFavoriteClick
            )
        }

        // Bottom block: merchant, title, price
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp)
                .clearAndSetSemantics {}
        ) {
            Text(
                text = offer.merchantName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = RescateCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = RescateCream,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = offer.discountedPrice,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = RescateButter,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactOfferCardPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MockOffers.getList().take(2).forEach { offer ->
                CompactOfferCard(
                    offer = offer,
                    onClick = {},
                    onFavoriteClick = {},
                    favoriteContentDescription = stringResource(
                        if (offer.isFavorite) R.string.common_favorite_remove_cd else R.string.common_favorite_add_cd
                    ),
                    modifier = Modifier.size(width = 168.dp, height = 210.dp)
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CompactOfferCardLightPreview() {
    CompactOfferCardPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CompactOfferCardDarkPreview() {
    CompactOfferCardPreviewContent(ThemeMode.DARK)
}
