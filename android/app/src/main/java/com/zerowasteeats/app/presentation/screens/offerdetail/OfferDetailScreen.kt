package com.zerowasteeats.app.presentation.screens.offerdetail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import com.zerowasteeats.app.presentation.components.glass.GlassButton
import com.zerowasteeats.app.presentation.data.offerPhotoRes
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCarbonElevated
import com.zerowasteeats.app.presentation.theme.RescateCoral
import com.zerowasteeats.app.presentation.theme.RescateCream
import com.zerowasteeats.app.presentation.theme.RescateCreamMuted
import com.zerowasteeats.app.presentation.theme.RescateButter
import kotlinx.coroutines.launch

@Composable
fun OfferDetailScreen(
    offer: OfferUiModel,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onReserveClick: () -> Unit,
) {
    val photo = offerPhotoRes(offer)
    val heartScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = RescateCarbon,
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
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // HERO: full-bleed photo with a fade into the carbon background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(RescateCarbonElevated)
            ) {
                if (photo != null) {
                    Image(
                        painter = painterResource(id = photo),
                        contentDescription = offer.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1f to RescateCarbon
                            )
                        )
                )

                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(AppSpacing.regular),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    CircleControl(
                        text = "←",
                        contentDescription = "Volver",
                        onClick = onBackClick
                    )
                    CircleControl(
                        text = if (offer.isFavorite) "♥" else "♡",
                        contentDescription = if (offer.isFavorite) "Quitar de favoritos" else "Marcar como favorito",
                        tint = if (offer.isFavorite) RescateCoral else RescateCream,
                        onClick = {
                            scope.launch {
                                heartScale.animateTo(
                                    1.35f,
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                )
                                heartScale.animateTo(1f, spring())
                            }
                            onFavoriteClick()
                        },
                        heartScale = heartScale
                    )
                }
            }

            // INFO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = offer.merchantName.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = RescateCreamMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = RescateCream
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = offer.discountedPrice,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = RescateButter
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = offer.originalPrice,
                        style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                        color = RescateCreamMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${offer.discountPercentage} · ${offer.remainingQuantityText}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = RescateCoral
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Acerca de esta oferta",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RescateCream
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Disfruta de nuestros productos frescos del día, empacados para que no se desperdicien. Ideal para sorprenderte o compartir.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = RescateCream.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Recogida",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RescateCream
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pickup card: flat carbon surface, no glass
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(RescateCarbonElevated)
                        .padding(20.dp)
                ) {
                    Text(
                        text = offer.merchantName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RescateCream
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = offer.pickupTime,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = RescateButter
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cra. 27 #18-45, Pasto", // Temporary location text
                        style = MaterialTheme.typography.bodyMedium,
                        color = RescateCreamMuted
                    )
                }

                // Space so the floating reservation panel does not cover the content
                Spacer(modifier = Modifier.height(160.dp))
            }
        }
    }
}

@Composable
private fun CircleControl(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = RescateCream,
    heartScale: Animatable<Float, *>? = null,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(RescateCarbon.copy(alpha = 0.55f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            color = tint,
            modifier = Modifier
                .graphicsLayer {
                    val s = heartScale?.value ?: 1f
                    scaleX = s
                    scaleY = s
                }
                .semantics { this.contentDescription = contentDescription }
        )
    }
}

@Composable
private fun ReservationPanel(
    offer: OfferUiModel,
    onReserveClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(AppSpacing.regular)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(RescateCarbonElevated)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = offer.discountedPrice,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = RescateButter
                )
                Text(
                    text = offer.remainingQuantityText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = RescateCoral
                )
            }

            GlassButton(
                text = "Reservar",
                onClick = onReserveClick
            )
        }
    }
}
