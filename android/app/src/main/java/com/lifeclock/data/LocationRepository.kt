package com.lifeclock.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.lifeclock.domain.City
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * Resolves the current device location into a [City].
 *
 * Uses FusedLocationProvider (Google Play Services) which is battery-friendly:
 *  - It reuses location already computed by other apps.
 *  - Falls back to coarse location to minimize power.
 *  - Never requests continuous updates — only a single one-shot read.
 */
class LocationRepository(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Try to read the last known location (zero-cost). If null, request a fresh
     * single-shot location with [Priority.BALANCED_POWER_ACCURACY] (coarse, low power).
     *
     * Returns null if location is unavailable (permission denied, GPS off, etc.).
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            // Try cached location first (no battery cost)
            val last = fusedClient.lastLocation.await()
            if (last != null) return last

            // No cache — request one-shot, low-power fresh location
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).await()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reverse-geocode the location into a city name + best-guess timezone.
     * Falls back gracefully if Geocoder fails (network down).
     */
    suspend fun resolveCity(location: Location): City? {
        return suspendCancellableCoroutine { cont ->
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                }.getOrNull() ?: emptyList()

                val addr = addresses.firstOrNull()
                val cityName = addr?.let {
                    it.locality ?: it.subAdminArea ?: it.adminArea ?: it.countryName
                } ?: "My Location"

                // Reverse-geocode to figure out the timezone.
                // Android's Geocoder doesn't expose timezone on API < 30. Even on
                // API 30+ the Address.getTimeZone() may be null. We always fall
                // back to the device's default timezone if we can't resolve one.
                val tzId = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val tz = android.location.LocationManager::class.java
                        // Use reflection-free path: Address.getTimeZone() (API 30+)
                        @Suppress("UNCHECKED_CAST")
                        val getter = android.location.Address::class.java
                            .getMethod("getTimeZone")
                        val tzObj = if (addr != null) getter.invoke(addr) as? java.util.TimeZone else null
                        tzObj?.id ?: TimeZone.getDefault().id
                    } else {
                        TimeZone.getDefault().id
                    }
                } catch (_: Exception) {
                    TimeZone.getDefault().id
                }

                if (cont.isActive) {
                    cont.resume(
                        City(
                            id = 0L,
                            name = cityName,
                            timeZoneId = tzId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isHome = true,
                            isPreset = false
                        )
                    )
                }
            } catch (_: Exception) {
                if (cont.isActive) {
                    cont.resume(
                        City(
                            id = 0L,
                            name = "My Location",
                            timeZoneId = TimeZone.getDefault().id,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isHome = true,
                            isPreset = false
                        )
                    )
                }
            }
        }
    }
}
