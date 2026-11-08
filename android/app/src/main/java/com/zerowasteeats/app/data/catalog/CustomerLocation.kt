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

/**
 * The device's last known location when the person already allowed it, the centre of Pasto
 * otherwise. It never asks for the permission: that only happens when they tap "my location"
 * on the map. A location that takes too long, or none at all, also falls back to Pasto.
 */
class DeviceLocationSource(context: Context) : CustomerLocationSource {

    private val appContext = context.applicationContext
    private val client by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    @SuppressLint("MissingPermission") // checked just before
    override suspend fun current(): GeoPoint {
        if (!hasPermission()) return PASTO_CENTER
        return withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                client.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { continuation.resume(null) }
            }
        } ?: PASTO_CENTER
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }

    private companion object {
        const val TIMEOUT_MILLIS = 3_000L
    }
}
