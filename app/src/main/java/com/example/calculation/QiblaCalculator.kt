package com.example.calculation

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object QiblaCalculator {

    const val KAABA_LATITUDE = 21.4225241
    const val KAABA_LONGITUDE = 39.8261818

    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the Qibla direction (bearing in degrees clockwise from True North, 0..360)
     * from the given user latitude and longitude.
     */
    fun calculateQiblaDirection(userLatitude: Double, userLongitude: Double): Double {
        val phi1 = userLatitude * DEG_TO_RAD
        val phi2 = KAABA_LATITUDE * DEG_TO_RAD
        val deltaLambda = (KAABA_LONGITUDE - userLongitude) * DEG_TO_RAD

        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)

        var qiblaDegrees = atan2(y, x) * RAD_TO_DEG
        qiblaDegrees = (qiblaDegrees + 360.0) % 360.0
        return qiblaDegrees
    }

    /**
     * Calculates great-circle distance to the Holy Kaaba in kilometers.
     */
    fun calculateDistanceToKaabaKm(userLatitude: Double, userLongitude: Double): Double {
        val lat1 = userLatitude * DEG_TO_RAD
        val lon1 = userLongitude * DEG_TO_RAD
        val lat2 = KAABA_LATITUDE * DEG_TO_RAD
        val lon2 = KAABA_LONGITUDE * DEG_TO_RAD

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
                cos(lat1) * cos(lat2) * sin(dLon / 2.0) * sin(dLon / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }
}
