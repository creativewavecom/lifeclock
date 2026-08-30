package com.lifeclock.data

import com.lifeclock.domain.SunriseCalculator
import com.lifeclock.domain.SunriseSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches sunrise/sunset times.
 *
 * Strategy (per user choice):
 *   - OFFLINE  : pure NOAA math, no network.
 *   - API      : always call sunrise-sunset.org (free, public).
 *   - HYBRID   : use offline result, but sync from API once per 7 days
 *                to correct the ±2-minute drift of the NOAA approximation.
 *
 * The API returns UTC ISO-8601 strings — we cache the parsed millis in DataStore
 * so that even if the user is offline for days, the last good result is reused.
 *
 * Implementation note: we use the built-in [HttpURLConnection] + [JSONObject]
 * instead of Retrofit/Moshi to keep the APK small (~3 MB savings).
 */
class SunriseRepository(
    private val settings: SettingsRepository
) {

    suspend fun getSunrise(
        source: SunriseSource,
        latitude: Double,
        longitude: Double,
        dateUtcMillis: Long
    ): SunriseCalculator.SolarTimes {
        // Always have an offline baseline (zero cost, zero permissions)
        val offline = SunriseCalculator.compute(dateUtcMillis, latitude, longitude)

        return when (source) {
            SunriseSource.OFFLINE -> offline
            SunriseSource.API -> {
                try {
                    fetchFromApi(latitude, longitude, dateUtcMillis) ?: offline
                } catch (_: Exception) {
                    offline
                }
            }
            SunriseSource.HYBRID -> {
                val lastSync: Long = settings.lastSunriseSync.first()
                val oneWeekMillis: Long = 7L * 24L * 60L * 60L * 1000L
                if (System.currentTimeMillis() - lastSync > oneWeekMillis) {
                    try {
                        val api = fetchFromApi(latitude, longitude, dateUtcMillis)
                        if (api != null) {
                            settings.markSunriseSyncNow()
                            return api
                        }
                    } catch (_: Exception) {
                        // fall through to offline
                    }
                }
                offline
            }
        }
    }

    private suspend fun fetchFromApi(
        lat: Double,
        lon: Double,
        dateUtcMillis: Long
    ): SunriseCalculator.SolarTimes? = withContext(Dispatchers.IO) {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = dateUtcMillis
        val dateStr = String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        val urlStr = "https://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&date=$dateStr&formatted=0"

        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true

            if (conn.responseCode != 200) return@withContext null

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            parseApiResponse(responseText)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Parse the sunrise-sunset.org JSON response.
     *
     * Sample: {"results":{"sunrise":"2024-08-04T01:21:31+00:00","sunset":"...","day_length":"..."},
     *         "status":"OK"}
     */
    private fun parseApiResponse(json: String): SunriseCalculator.SolarTimes? {
        return try {
            val root = JSONObject(json)
            val status = root.optString("status", "")
            if (status != "OK") return null
            val results = root.optJSONObject("results") ?: return null

            val sunriseStr = results.optString("sunrise", "")
            val sunsetStr = results.optString("sunset", "")
            val dayLengthStr = results.optString("day_length", "")

            val sunriseUtc = parseIso(sunriseStr) ?: return null
            val sunsetUtc = parseIso(sunsetStr) ?: return null
            val dayLengthMinutes = parseDuration(dayLengthStr)

            SunriseCalculator.SolarTimes(
                sunriseUtcMillis = sunriseUtc,
                sunsetUtcMillis = sunsetUtc,
                dayLengthMinutes = dayLengthMinutes
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parse "2024-08-04T05:42:13+00:00" into UTC millis.
     */
    private fun parseIso(iso: String): Long? {
        return try {
            // Strip offset — server returns "+00:00"
            val core = iso.substringBefore("+").substringBefore("Z")
            val parts = core.split("T")
            val (y, m, d) = parts[0].split("-").map { it.toInt() }
            val (h, mi, s) = parts[1].split(":").map { it.toInt() }
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.clear()
            cal.set(y, m - 1, d, h, mi, s)
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /** Parse "12:34:56" → minutes */
    private fun parseDuration(s: String): Int {
        val parts = s.split(":").map { it.toIntOrNull() ?: 0 }
        return (parts.getOrNull(0) ?: 0) * 60 + (parts.getOrNull(1) ?: 0)
    }
}
