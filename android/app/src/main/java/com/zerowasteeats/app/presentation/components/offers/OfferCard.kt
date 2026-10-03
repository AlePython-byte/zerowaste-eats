package com.zerowasteeats.app.presentation.components.offers

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.GlassCard
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun OfferCard(
    offer: OfferUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    favoriteContentDescription: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        contentPadding = PaddingValues(0.dp)
    ) {
        // Image Area (Large, dominant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(topStart = AppRadius.large, topEnd = AppRadius.large))
                .background(Color.LightGray.copy(alpha = 0.5f)) // Temporary placeholder for Coil
        ) {
            // Remaining quantity badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(AppRadius.medium),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacing.medium)
            ) {
                Text(
                    text = offer.remainingQuantityText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall)
                )
            }

            // Favorite Button
            Surface(
                shape = CircleShape,
                color = GlassTokens.lightSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppSpacing.medium)
            ) {
                IconButton(onClick = onFavoriteClick) {
                    val iconRes = if (offer.isFavorite) {
                        R.drawable.btn_star_big_on
                    } else {
                        R.drawable.btn_star_big_off
                    }
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = favoriteContentDescription,
                        tint = if (offer.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Discount Badge (Glass effect)
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(AppRadius.medium),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacing.medium)
            ) {
                Text(
                    text = offer.discountPercentage,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = AppSpacing.regular, vertical = AppSpacing.small)
                )
            }
        }

        // Content Area
        Column(
            modifier = Modifier.padding(AppSpacing.regular)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.merchantName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = offer.distance,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = offer.pickupTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = offer.originalPrice,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = AppSpacing.small)
                )

                Text(
                    text = offer.discountedPrice,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
