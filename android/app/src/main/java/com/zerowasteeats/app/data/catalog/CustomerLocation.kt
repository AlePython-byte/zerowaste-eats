package com.zerowasteeats.app.data.catalog

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Where to look for offers. */
fun interface CustomerLocationSource {
    suspend fun current(): GeoPoint
}

/** Always the centre of Pasto: for demo mode, whose sample offers are there. */
val PastoLocationSource = CustomerLocationSource { PASTO_CENTER }

/** How far from the centre of Pasto the service reaches. */
const val SERVICE_RADIUS_KM = 50.0

/**
 * [point] when it is within [SERVICE_RADIUS_KM] of the centre of Pasto, the centre otherwise:
 * there are no offers farther away, so a person elsewhere (or an emulator placed in another
 * country) still sees Pasto's.
 */
fun withinServiceArea(point: GeoPoint): GeoPoint =
    if (distanceKm(point, PASTO_CENTER) > SERVICE_RADIUS_KM) PASTO_CENTER else point

/**
 * The device's last known location when the person already allowed it, the centre of Pasto
 * otherwise. It never asks for the permission: that only happens when they tap "my location"
 * on the map. A location that takes too long, none at all, or one outside the service area
 * also falls back to Pasto.
 */
class DeviceLocationSource(context: Context) : CustomerLocationSource {

    private val appContext = context.applicationContext
    private val client by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    @SuppressLint("MissingPermission") // checked just before
    override suspend fun current(): GeoPoint {
        if (!hasPermission()) return PASTO_CENTER
        val found = withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<GeoPoint?> { continuation ->
                client.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { continuation.resume(null) }
            }
        } ?: return PASTO_CENTER
        return withinServiceArea(found)
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }

    private companion object {
        const val TIMEOUT_MILLIS = 3_000L
    }
}
