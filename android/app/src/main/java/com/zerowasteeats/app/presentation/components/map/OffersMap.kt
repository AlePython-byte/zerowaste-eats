package com.zerowasteeats.app.presentation.components.map

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.data.MockMapCoordinates
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.isAppInDarkTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Map of [offers] with the selected offer's card and a "my location" button.
 * Owns the location permission flow; errors are reported through [onLocationMessage]
 * so the host decides how to show them. [contentPadding] keeps the map controls and
 * the card clear of system bars. [onMapLoaded] fires once the map tiles have loaded.
 */
@Composable
fun OffersMap(
    offers: List<OfferUiModel>,
    selectedOfferId: String?,
    onSelectOffer: (String?) -> Unit,
    onOfferClick: (String) -> Unit,
    onLocationMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MockMapCoordinates.pastoCenter, 14f)
    },
    contentPadding: PaddingValues = PaddingValues(),
    onMapLoaded: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    // Resolved here because messages are sent from callbacks, outside composition
    val locationUnavailableMessage = stringResource(R.string.map_location_unavailable)
    val locationPermissionMessage = stringResource(R.string.map_location_permission_required)

    val userLocation = rememberUserLocationState { result ->
        when (result) {
            is LocationResult.Found -> scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(result.latLng, 15f))
            }
            LocationResult.Unavailable -> onLocationMessage(locationUnavailableMessage)
            LocationResult.PermissionDenied -> onLocationMessage(locationPermissionMessage)
        }
    }

    val selectedOffer = offers.find { it.id == selectedOfferId }
    val animationsEnabled = rememberAnimationsEnabled()
    // Follows the app's theme, not the system's. It is part of the map's properties, so a
    // change is applied to the live map without rebuilding it.
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val mapStyle = remember(isDark) {
        MapStyleOptions.loadRawResourceStyle(
            context,
            if (isDark) R.raw.map_style_dark else R.raw.map_style_light
        )
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = userLocation.hasPermission,
                mapStyleOptions = mapStyle
            ),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false), // We use custom UI
            contentPadding = contentPadding,
            onMapLoaded = onMapLoaded,
            onMapClick = { onSelectOffer(null) }
        ) {
            offers.forEach { offer ->
                // Keyed so a marker's state stays with its offer when the list changes
                key(offer.id) {
                    val position = MockMapCoordinates.getCoordinatesForOffer(offer.id)
                    val markerState = rememberMarkerState(position = position)
                    val selected = offer.id == selectedOfferId
                    val pinScale by animateFloatAsState(
                        targetValue = if (selected) SELECTED_PIN_SCALE else 1f,
                        animationSpec = if (animationsEnabled) {
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        } else {
                            snap()
                        },
                        label = "pinScale"
                    )
                    // The pin is redrawn into a bitmap whenever a key changes, so the scale is
                    // rounded to hundredths: a few redraws, only for the selected pin
                    val scaleStep = (pinScale * 100).roundToInt()
                    val pinTitle = stringResource(R.string.map_pin_title, offer.title, offer.discountedPrice)
                    MarkerComposable(
                        offer.id, offer.discountedPrice, selected, scaleStep,
                        state = markerState,
                        title = pinTitle,
                        // Above the others, so the selected pin is never hidden under a neighbour
                        zIndex = if (selected) 1f else 0f,
                        onClick = {
                            onSelectOffer(offer.id)
                            true // consume the event to show custom UI
                        }
                    ) {
                        PricePin(price = offer.discountedPrice, selected = selected, scale = scaleStep / 100f)
                    }
                }
            }
        }

        if (selectedOffer != null) {
            // Selected offer card at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(contentPadding)
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
                    .padding(contentPadding)
                    .padding(AppSpacing.regular)
            ) {
                MapCircleButton(
                    contentDescription = stringResource(R.string.map_my_location_cd),
                    onClick = userLocation::locate
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                        contentDescription = null, // semantics are on the button
                        tint = MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = AppSpacing.small),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(AppSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.discountedPrice,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = AppSpacing.small),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                AppButton(
                    text = stringResource(R.string.map_view_offer),
                    onClick = onViewOfferClick
                )
            }
        }
    }
}
