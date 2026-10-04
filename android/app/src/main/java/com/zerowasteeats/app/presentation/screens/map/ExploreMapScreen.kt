package com.zerowasteeats.app.presentation.screens.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.zerowasteeats.app.presentation.components.glass.GlassButton
import com.zerowasteeats.app.presentation.components.glass.GlassCard
import com.zerowasteeats.app.presentation.data.MockMapCoordinates
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun ExploreMapScreen(
    onBackClick: () -> Unit,
    onOfferClick: (String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    var selectedOfferId by rememberSaveable { mutableStateOf<String?>(null) }
    
    val offers = MockOffers.getList()
    val selectedOffer = offers.find { it.id == selectedOfferId }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MockMapCoordinates.pastoCenter, 14f)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = innerPadding,
                onMapClick = { selectedOfferId = null }
            ) {
                offers.forEach { offer ->
                    val position = MockMapCoordinates.getCoordinatesForOffer(offer.id)
                    val markerState = rememberMarkerState(position = position)
                    Marker(
                        state = markerState,
                        title = offer.merchantName,
                        snippet = offer.title,
                        onClick = {
                            selectedOfferId = offer.id
                            true // consume the event to show custom UI
                        }
                    )
                }
            }

            // Top controls
            val glassSurfaceColor = if (isDarkTheme) {
                GlassTokens.darkSurface.copy(alpha = GlassTokens.darkSurface.alpha * 0.85f)
            } else {
                GlassTokens.lightSurface.copy(alpha = GlassTokens.lightSurface.alpha * 0.90f)
            }
            val glassBorderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(AppSpacing.regular)
            ) {
                GlassTextButton(
                    text = "←",
                    contentDescription = "Volver",
                    onClick = onBackClick,
                    surfaceColor = glassSurfaceColor,
                    borderColor = glassBorderColor
                )
            }

            // Selected offer card at the bottom
            if (selectedOffer != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(innerPadding)
                        .padding(AppSpacing.regular)
                ) {
                    MapOfferCard(
                        offer = selectedOffer,
                        onViewOfferClick = { onOfferClick(selectedOffer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapOfferCard(
    offer: OfferUiModel,
    onViewOfferClick: () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.merchantName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
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
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.small))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.discountedPrice,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                GlassButton(
                    text = "Ver oferta",
                    onClick = onViewOfferClick
                )
            }
        }
    }
}

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
        shadowElevation = GlassTokens.elevation / 2,
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
