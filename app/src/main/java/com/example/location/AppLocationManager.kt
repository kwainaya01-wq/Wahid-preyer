package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume

class AppLocationManager(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<UserLocation> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("Location permission not granted"))
        }

        try {
            val location = suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }

                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                cont.resume(lastLoc)
                            }.addOnFailureListener {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            cont.resume(lastLoc)
                        }.addOnFailureListener {
                            cont.resume(null)
                        }
                    }
            }

            if (location != null) {
                val userLocation = resolveLocationDetails(location.latitude, location.longitude, isGps = true)
                Result.success(userLocation)
            } else {
                Result.failure(IllegalStateException("Unable to retrieve device coordinates"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveLocationDetails(latitude: Double, longitude: Double, isGps: Boolean): UserLocation =
        withContext(Dispatchers.IO) {
            val systemTimezone = ZoneId.systemDefault().id
            var cityName = ""
            var countryName = ""

            // Attempt Geocoder lookup
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val addresses = suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(latitude, longitude, 1) { list ->
                                cont.resume(list)
                            }
                        }
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                            countryName = addr.countryName ?: ""
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                            countryName = addr.countryName ?: ""
                        }
                    }
                }
            } catch (_: Exception) {
                // Network or geocoder timeout, fallback below
            }

            // If geocoder failed or offline, find closest known city from database
            if (cityName.isBlank()) {
                val nearest = PresetCities.findNearestCity(latitude, longitude)
                cityName = nearest.city
                countryName = nearest.country
            }

            UserLocation(
                latitude = latitude,
                longitude = longitude,
                cityName = cityName,
                countryName = countryName,
                timezoneId = systemTimezone,
                isGps = isGps
            )
        }
}
