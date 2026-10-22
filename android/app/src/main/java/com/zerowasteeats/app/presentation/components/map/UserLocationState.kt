package com.zerowasteeats.app.presentation.components.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

sealed class LocationResult {
    data class Found(val latLng: LatLng) : LocationResult()
    data object Unavailable : LocationResult()
    data object PermissionDenied : LocationResult()
}

/**
 * Location permission and last-known-location lookup. [locate] is the only entry point:
 * it asks for the permission when missing and continues on its own once it is granted.
 */
@Stable
class UserLocationState internal constructor(
    initialPermission: Boolean,
    private val client: FusedLocationProviderClient,
    private val onResult: (LocationResult) -> Unit
) {
    var hasPermission by mutableStateOf(initialPermission)
        internal set

    internal var requestPermission: () -> Unit = {}

    @SuppressLint("MissingPermission") // guarded by hasPermission
    fun locate() {
        if (!hasPermission) {
            requestPermission()
            return
        }
        client.lastLocation
            .addOnSuccessListener { location ->
                onResult(
                    if (location != null) {
                        LocationResult.Found(LatLng(location.latitude, location.longitude))
                    } else {
                        LocationResult.Unavailable
                    }
                )
            }
            .addOnFailureListener { onResult(LocationResult.Unavailable) }
    }

    internal fun onPermissionResult(granted: Boolean) {
        hasPermission = granted
        if (granted) locate() else onResult(LocationResult.PermissionDenied)
    }
}

@Composable
fun rememberUserLocationState(onResult: (LocationResult) -> Unit): UserLocationState {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val state = remember {
        UserLocationState(
            initialPermission = context.hasLocationPermission(),
            client = LocationServices.getFusedLocationProviderClient(context),
            onResult = { currentOnResult(it) }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        state.onPermissionResult(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        )
    }
    SideEffect {
        state.requestPermission = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // The user can change the permission from Settings while the app is in the background
    LifecycleResumeEffect(Unit) {
        state.hasPermission = context.hasLocationPermission()
        onPauseOrDispose {}
    }

    return state
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
