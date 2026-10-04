package com.zerowasteeats.app.presentation.screens.offerdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.GlassButton
import com.zerowasteeats.app.presentation.components.glass.GlassCard
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun OfferDetailScreen(
    offer: OfferUiModel,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onReserveClick: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Enhanced visual depth on the placeholder hero
    val placeholderBrush = when (offer.category) {
        "Panadería" -> Brush.verticalGradient(listOf(Color(0xFFF0E5D1), Color(0xFFC7B18E)))
        "Comida preparada" -> Brush.verticalGradient(listOf(Color(0xFFF0E0D1), Color(0xFFC9A685)))
        "Postres" -> Brush.verticalGradient(listOf(Color(0xFFF0D1E0), Color(0xFFC48FA7)))
        else -> Brush.verticalGradient(listOf(Color(0xFFF0F5F2), Color(0xFFBBC9C1)))
    }

    // Highly translucent controls over hero
    val glassSurfaceColor = if (isDarkTheme) {
        GlassTokens.darkSurface.copy(alpha = GlassTokens.darkSurface.alpha * 0.85f)
    } else {
        GlassTokens.lightSurface.copy(alpha = GlassTokens.lightSurface.alpha * 0.90f)
    }
    
    val glassBorderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ReservationPanel(
                offer = offer,
                onReserveClick = onReserveClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // We do not pad the bottom with innerPadding so content scrolls under floating panel
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // HERO IMAGE AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f) // Reduced height to avoid unnecessary vertical empty space
                    .background(placeholderBrush)
            ) {
                // Top controls overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(AppSpacing.regular),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    GlassTextButton(
                        text = "←",
                        contentDescription = "Volver",
                        onClick = onBackClick,
                        surfaceColor = glassSurfaceColor,
                        borderColor = glassBorderColor
                    )
                    
                    GlassTextButton(
                        text = if (offer.isFavorite) "♥" else "♡",
                        contentDescription = if (offer.isFavorite) "Quitar de favoritos" else "Marcar como favorito",
                        onClick = onFavoriteClick,
                        surfaceColor = glassSurfaceColor,
                        borderColor = glassBorderColor,
                        tint = if (offer.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // INFO AREA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.regular)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = offer.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(AppRadius.small)
                    ) {
                        Text(
                            text = offer.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.small))

                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.small))
                
                Text(
                    text = offer.distance,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                // COMPACT PRICING BLOCK
                Text(
                    text = offer.discountedPrice,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = offer.originalPrice,
                        style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " · ${offer.discountPercentage}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

                Text(
                    text = offer.remainingQuantityText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

                Text(
                    text = "Acerca de esta oferta",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.small))

                Text(
                    text = "Disfruta de nuestros productos frescos del día, empacados para que no se desperdicien. Ideal para sorprenderte o compartir.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

                // PICKUP SECTION
                Text(
                    text = "Recogida",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.small))

                GlassCard {
                    Text(
                        text = offer.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    Text(
                        text = offer.pickupTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    Text(
                        text = "Cra. 27 #18-45, Pasto", // Updated temporary location text
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.huge * 3)) // Padding for bottom floating panel
            }
        }
    }
}

// Simple custom visual solution to avoid android.R legacy icons
@Composable
private fun GlassTextButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    surfaceColor: Color,
    borderColor: Color,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = CircleShape,
        color = surfaceColor,
        border = BorderStroke(GlassTokens.borderWidth, borderColor),
        shadowElevation = GlassTokens.elevation / 2, // Restrained shadow
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = tint,
                modifier = Modifier.semantics { this.contentDescription = contentDescription }
            )
        }
    }
}

@Composable
private fun ReservationPanel(
    offer: OfferUiModel,
    onReserveClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val backgroundColor = if (isDarkTheme) {
        GlassTokens.darkSurface.copy(alpha = GlassTokens.darkSurface.alpha * 0.9f)
    } else {
        GlassTokens.lightSurface.copy(alpha = GlassTokens.lightSurface.alpha * 0.95f)
    }

    val borderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(AppSpacing.regular)
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(AppRadius.extraLarge),
            border = BorderStroke(GlassTokens.borderWidth, borderColor),
            shadowElevation = GlassTokens.elevation / 1.5f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.regular, vertical = AppSpacing.regular),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = offer.discountedPrice,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.small))
                        Text(
                            text = offer.originalPrice,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = offer.remainingQuantityText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                GlassButton(
                    text = "Reservar",
                    onClick = onReserveClick
                )
            }
        }
    }
}
