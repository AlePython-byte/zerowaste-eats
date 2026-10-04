package com.zerowasteeats.app.presentation.components.offers

import android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
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
    val isDarkTheme = isSystemInDarkTheme()
    
    val placeholderBrush = when (offer.category) {
        "Panadería" -> Brush.linearGradient(listOf(Color(0xFFE5D9C5), Color(0xFFD4C1A3)))
        "Comida preparada" -> Brush.linearGradient(listOf(Color(0xFFE5D5C5), Color(0xFFD4BBA3)))
        "Postres" -> Brush.linearGradient(listOf(Color(0xFFE5C5D5), Color(0xFFD4A3BB)))
        else -> Brush.linearGradient(listOf(Color(0xFFE5EBE7), Color(0xFFD4DBD7)))
    }
    
    val glassSurfaceColor = if (isDarkTheme) GlassTokens.darkSurface else GlassTokens.lightSurface
    val glassBorderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        contentPadding = PaddingValues(0.dp)
    ) {
        // Image Area (Large, dominant, optimized aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
                .clip(RoundedCornerShape(topStart = AppRadius.large, topEnd = AppRadius.large))
                .background(placeholderBrush) // Visual richer placeholder
        ) {
            // Remaining quantity badge
            Surface(
                color = glassSurfaceColor,
                shape = RoundedCornerShape(AppRadius.medium),
                border = BorderStroke(GlassTokens.borderWidth, glassBorderColor),
                shadowElevation = GlassTokens.elevation / 2,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacing.medium)
            ) {
                Text(
                    text = offer.remainingQuantityText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall)
                )
            }

            // Favorite Button
            Surface(
                shape = CircleShape,
                color = glassSurfaceColor,
                border = BorderStroke(GlassTokens.borderWidth, glassBorderColor),
                shadowElevation = GlassTokens.elevation / 2,
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

            // Discount Badge (Glass effect over primary color)
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(AppRadius.medium),
                border = BorderStroke(GlassTokens.borderWidth, glassBorderColor),
                shadowElevation = GlassTokens.elevation / 2,
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
