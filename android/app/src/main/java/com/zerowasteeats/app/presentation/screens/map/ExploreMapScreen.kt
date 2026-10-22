package com.zerowasteeats.app.presentation.screens.map

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.map.LocationResult
import com.zerowasteeats.app.presentation.components.map.rememberUserLocationState
import com.zerowasteeats.app.presentation.data.MockMapCoordinates
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import kotlinx.coroutines.launch

@Composable
fun ExploreMapScreen(
    onBackClick: () -> Unit,
    onOfferClick: (String) -> Unit
) {
    var selectedOfferId by rememberSaveable { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Resolved here because snackbars are shown from callbacks, outside composition
    val locationUnavailableMessage = stringResource(R.string.map_location_unavailable)
    val locationPermissionMessage = stringResource(R.string.map_location_permission_required)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MockMapCoordinates.pastoCenter, 14f)
    }

    val userLocation = rememberUserLocationState { result ->
        when (result) {
            is LocationResult.Found -> scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(result.latLng, 15f))
            }
            LocationResult.Unavailable -> scope.launch {
                snackbarHostState.showSnackbar(locationUnavailableMessage)
            }
            LocationResult.PermissionDenied -> scope.launch {
                snackbarHostState.showSnackbar(locationPermissionMessage)
            }
        }
    }

    val offers = MockOffers.getList()
    val selectedOffer = offers.find { it.id == selectedOfferId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = userLocation.hasPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false), // We use custom UI
                contentPadding = innerPadding,
                onMapClick = { selectedOfferId = null }
            ) {
                offers.forEach { offer ->
                    // Keyed so a marker's state stays with its offer when the list changes
                    key(offer.id) {
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
            }

            // Top controls
            val surfaceColor = SurfaceTokens.surface
            val borderColor = SurfaceTokens.border

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(AppSpacing.regular)
            ) {
                CircleTextButton(
                    text = "←",
                    contentDescription = stringResource(R.string.common_back_cd),
                    onClick = onBackClick,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor
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
            } else {
                // Mi ubicación button (only visible when no offer is selected, to avoid clutter)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(innerPadding)
                        .padding(AppSpacing.regular)
                ) {
                    CircleTextButton(
                        text = "◎", // Location symbol placeholder
                        contentDescription = stringResource(R.string.map_my_location_cd),
                        onClick = userLocation::locate,
                        surfaceColor = surfaceColor,
                        borderColor = borderColor
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
    AppCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.merchantName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
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
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                AppButton(
                    text = stringResource(R.string.map_view_offer),
                    onClick = onViewOfferClick
                )
            }
        }
    }
}

@Composable
private fun CircleTextButton(
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
        border = BorderStroke(SurfaceTokens.borderWidth, borderColor),
        shadowElevation = SurfaceTokens.elevation,
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
