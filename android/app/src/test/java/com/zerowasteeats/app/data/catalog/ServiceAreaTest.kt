package com.zerowasteeats.app.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceAreaTest {

    @Test
    fun aPlaceInPasto_isKept() {
        val unicentro = GeoPoint(1.2160, -77.2880)

        assertEquals(unicentro, withinServiceArea(unicentro))
    }

    @Test
    fun aNearbyTown_withinFiftyKilometres_isKept() {
        // Tangua, about 18 km from the centre of Pasto
        val tangua = GeoPoint(1.0947, -77.3953)

        assertEquals(tangua, withinServiceArea(tangua))
    }

    @Test
    fun aPlaceFartherAway_searchesFromTheCentreOfPasto() {
        // An emulator's default location, Mountain View
        val mountainView = GeoPoint(37.4220, -122.0841)
        // Ipiales, about 59 km away, and Cali, about 300 km away
        val ipiales = GeoPoint(0.8302, -77.6444)
        val cali = GeoPoint(3.4516, -76.5320)

        assertEquals(PASTO_CENTER, withinServiceArea(mountainView))
        assertEquals(PASTO_CENTER, withinServiceArea(ipiales))
        assertEquals(PASTO_CENTER, withinServiceArea(cali))
    }

    @Test
    fun theDistance_isMeasuredLikeTheServer() {
        val oneDegreeNorth = GeoPoint(PASTO_CENTER.latitude + 1, PASTO_CENTER.longitude)

        // One degree of latitude is about 111 km
        assertEquals(111.2, distanceKm(PASTO_CENTER, oneDegreeNorth), 0.1)
    }
}
