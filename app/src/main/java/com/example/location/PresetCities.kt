package com.example.location

import com.example.calculation.CalculationMethod
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PresetCity(
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val suggestedMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
) {
    fun toUserLocation(): UserLocation {
        return UserLocation(
            latitude = latitude,
            longitude = longitude,
            cityName = city,
            countryName = country,
            timezoneId = timezoneId,
            isGps = false
        )
    }
}

object PresetCities {
    val CITIES = listOf(
        PresetCity("Makkah", "Saudi Arabia", 21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PresetCity("Madinah", "Saudi Arabia", 24.4672, 39.6111, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PresetCity("Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PresetCity("Jeddah", "Saudi Arabia", 21.4858, 39.1925, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PresetCity("Dubai", "United Arab Emirates", 25.2048, 55.2708, "Asia/Dubai", CalculationMethod.DUBAI),
        PresetCity("Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773, "Asia/Dubai", CalculationMethod.DUBAI),
        PresetCity("Doha", "Qatar", 25.2854, 51.5310, "Asia/Qatar", CalculationMethod.QATAR),
        PresetCity("Kuwait City", "Kuwait", 29.3759, 47.9774, "Asia/Kuwait", CalculationMethod.KUWAIT),
        PresetCity("Manama", "Bahrain", 26.2285, 50.5860, "Asia/Bahrain", CalculationMethod.UMM_AL_QURA),
        PresetCity("Muscat", "Oman", 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.UMM_AL_QURA),
        PresetCity("Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPTIAN),
        PresetCity("Alexandria", "Egypt", 31.2001, 29.9187, "Africa/Cairo", CalculationMethod.EGYPTIAN),
        PresetCity("Amman", "Jordan", 31.9454, 35.9284, "Asia/Amman", CalculationMethod.UMM_AL_QURA),
        PresetCity("Jerusalem", "Palestine", 31.7683, 35.2137, "Asia/Jerusalem", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Beirut", "Lebanon", 33.8938, 35.5018, "Asia/Beirut", CalculationMethod.EGYPTIAN),
        PresetCity("Damascus", "Syria", 33.5138, 36.2765, "Asia/Damascus", CalculationMethod.EGYPTIAN),
        PresetCity("Baghdad", "Iraq", 33.3152, 44.3661, "Asia/Baghdad", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Karachi", "Pakistan", 24.8607, 67.0011, "Asia/Karachi", CalculationMethod.KARACHI),
        PresetCity("Lahore", "Pakistan", 31.5497, 74.3436, "Asia/Karachi", CalculationMethod.KARACHI),
        PresetCity("Islamabad", "Pakistan", 33.6844, 73.0479, "Asia/Karachi", CalculationMethod.KARACHI),
        PresetCity("Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka", CalculationMethod.KARACHI),
        PresetCity("Chittagong", "Bangladesh", 22.3569, 91.7832, "Asia/Dhaka", CalculationMethod.KARACHI),
        PresetCity("Delhi", "India", 28.6139, 77.2090, "Asia/Kolkata", CalculationMethod.KARACHI),
        PresetCity("Mumbai", "India", 19.0760, 72.8777, "Asia/Kolkata", CalculationMethod.KARACHI),
        PresetCity("Hyderabad", "India", 17.3850, 78.4867, "Asia/Kolkata", CalculationMethod.KARACHI),
        PresetCity("Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Surabaya", "Indonesia", -7.2575, 112.7521, "Asia/Jakarta", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Singapore", "Singapore", 1.3521, 103.8198, "Asia/Singapore", CalculationMethod.SINGAPORE),
        PresetCity("London", "United Kingdom", 51.5074, -0.1278, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Birmingham", "United Kingdom", 52.4862, -1.8904, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Manchester", "United Kingdom", 53.4808, -2.2426, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Paris", "France", 48.8566, 2.3522, "Europe/Paris", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Berlin", "Germany", 52.5200, 13.4050, "Europe/Berlin", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Toronto", "Canada", 43.6532, -79.3832, "America/Toronto", CalculationMethod.NORTH_AMERICA),
        PresetCity("Montreal", "Canada", 45.5017, -73.5673, "America/Toronto", CalculationMethod.NORTH_AMERICA),
        PresetCity("New York", "United States", 40.7128, -74.0060, "America/New_York", CalculationMethod.NORTH_AMERICA),
        PresetCity("Chicago", "United States", 41.8781, -87.6298, "America/Chicago", CalculationMethod.NORTH_AMERICA),
        PresetCity("Houston", "United States", 29.7604, -95.3698, "America/Chicago", CalculationMethod.NORTH_AMERICA),
        PresetCity("Los Angeles", "United States", 34.0522, -118.2437, "America/Los_Angeles", CalculationMethod.NORTH_AMERICA),
        PresetCity("San Francisco", "United States", 37.7749, -122.4194, "America/Los_Angeles", CalculationMethod.NORTH_AMERICA),
        PresetCity("Sydney", "Australia", -33.8688, 151.2093, "Australia/Sydney", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Melbourne", "Australia", -37.8136, 144.9631, "Australia/Melbourne", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Casablanca", "Morocco", 33.5731, -7.5898, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Rabat", "Morocco", 34.0209, -6.8416, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Algiers", "Algeria", 36.7538, 3.0588, "Africa/Algiers", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Tunis", "Tunisia", 36.8065, 10.1815, "Africa/Tunis", CalculationMethod.MUSLIM_WORLD_LEAGUE),
        PresetCity("Kano", "Nigeria", 12.0022, 8.5920, "Africa/Lagos", CalculationMethod.EGYPTIAN),
        PresetCity("Lagos", "Nigeria", 6.5244, 3.3792, "Africa/Lagos", CalculationMethod.EGYPTIAN),
        PresetCity("Johannesburg", "South Africa", -26.2041, 28.0473, "Africa/Johannesburg", CalculationMethod.MUSLIM_WORLD_LEAGUE)
    )

    fun findNearestCity(latitude: Double, longitude: Double): PresetCity {
        var closest = CITIES[0]
        var minDistance = Double.MAX_VALUE
        for (city in CITIES) {
            val dist = distanceKm(latitude, longitude, city.latitude, city.longitude)
            if (dist < minDistance) {
                minDistance = dist
                closest = city
            }
        }
        return closest
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rad = Math.PI / 180.0
        val dLat = (lat2 - lat1) * rad
        val dLon = (lon2 - lon1) * rad
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * rad) * cos(lat2 * rad) * sin(dLon / 2) * sin(dLon / 2)
        return 6371.0 * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    }
}
