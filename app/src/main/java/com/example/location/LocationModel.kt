package com.example.location

import java.time.ZoneId

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val countryName: String,
    val timezoneId: String = ZoneId.systemDefault().id,
    val isGps: Boolean = true
) {
    val displayName: String
        get() = if (countryName.isNotBlank() && countryName != cityName) {
            "$cityName, $countryName"
        } else {
            cityName
        }

    val zoneId: ZoneId
        get() = try {
            ZoneId.of(timezoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

    companion object {
        val DEFAULT_MAKKAH = UserLocation(
            latitude = 21.4225,
            longitude = 39.8262,
            cityName = "Makkah",
            countryName = "Saudi Arabia",
            timezoneId = "Asia/Riyadh",
            isGps = false
        )
    }
}
