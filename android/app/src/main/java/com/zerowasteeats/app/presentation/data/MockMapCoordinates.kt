package com.zerowasteeats.app.presentation.data

import com.google.android.gms.maps.model.LatLng

/**
 * Temporary UI mock data. 
 * These coordinates are NOT real merchant locations and do not reflect real establishments.
 * They are placed approximately around Pasto, Nariño for UI demonstration purposes only.
 */
object MockMapCoordinates {
    // Center around Pasto, Nariño, Colombia
    val pastoCenter = LatLng(1.2136, -77.2811)

    fun getCoordinatesForOffer(offerId: String): LatLng {
        return when (offerId) {
            "1" -> LatLng(1.2150, -77.2800) // Croissants del día
            "2" -> LatLng(1.2120, -77.2830) // Bowl saludable de salmón
            "3" -> LatLng(1.2170, -77.2780) // Caja de postres surtidos
            else -> pastoCenter
        }
    }
}
