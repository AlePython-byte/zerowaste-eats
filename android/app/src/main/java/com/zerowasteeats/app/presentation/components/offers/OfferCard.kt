package com.zerowasteeats.app.presentation.components.offers

import com.zerowasteeats.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowasteeats.app.presentation.data.offerPhotoRes
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.RescateButter
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCarbonElevated
import com.zerowasteeats.app.presentation.theme.RescateCoral
import com.zerowasteeats.app.presentation.theme.RescateCream
import com.zerowasteeats.app.presentation.theme.RescateCreamMuted
import kotlinx.coroutines.launch

/**
 * Immersive offer card: the photo fills the card, and the information floats
 * over a dark gradient at the bottom.
 */
@Composable
fun OfferCard(
    offer: OfferUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    favoriteContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val photo = offerPhotoRes(offer)
    val heartScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val fallbackBrush = Brush.linearGradient(listOf(RescateCarbonElevated, RescateCarbon))
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(cardShape)
            .background(fallbackBrush)
            .clickable { onClick() }
    ) {
        if (photo != null) {
            Image(
                painter = painterResource(id = photo),
                contentDescription = offer.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient so the text stays readable over any photo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.2f to RescateCarbon.copy(alpha = 0.10f),
                        0.45f to RescateCarbon.copy(alpha = 0.70f),
                        1f to RescateCarbon.copy(alpha = 0.96f)
                    )
                )
        )

        // Top row: remaining units + favorite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoPill(text = pluralStringResource(R.plurals.offer_remaining_count, offer.remainingQuantity, offer.remainingQuantity))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RescateCarbon.copy(alpha = 0.55f))
                    .clickable {
                        scope.launch {
                            heartScale.animateTo(
                                1.35f,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                            )
                            heartScale.animateTo(1f, spring())
                        }
                        onFavoriteClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (offer.isFavorite) "♥" else "♡",
                    fontSize = 22.sp,
                    color = if (offer.isFavorite) RescateCoral else RescateCream,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = heartScale.value
                            scaleY = heartScale.value
                        }
                        .semantics { contentDescription = favoriteContentDescription }
                )
            }
        }

        // Bottom block: merchant, title, price
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.merchantName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = RescateCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = offer.distance,
                    style = MaterialTheme.typography.labelSmall,
                    color = RescateCream
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = offer.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = RescateCream,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = offer.discountedPrice,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = RescateButter
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = offer.originalPrice,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                    color = RescateCreamMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                InfoPill(
                    text = stringResource(R.string.offer_discount_off, offer.discountPercent),
                    background = RescateCoral,
                    textColor = RescateCream
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = offer.pickupTime,
                style = MaterialTheme.typography.labelMedium,
                color = RescateCreamMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoPill(
    text: String,
    background: Color = RescateCarbon.copy(alpha = 0.6f),
    textColor: Color = RescateCream,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}
