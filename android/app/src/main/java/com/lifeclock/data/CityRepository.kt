package com.lifeclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifeclock.domain.City
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cityDataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeclock_cities")

/**
 * Stores the user's saved cities as a JSON-like set of comma-delimited strings.
 *
 * Each city is persisted as a set of strings in the format:
 *   "<id>|<name>|<tzId>|<lat>|<lon>|<isHome>|<isPreset>"
 *
 * For a small list of cities (≤20), this is simpler and as efficient as
 * Room/SQLite — and it avoids a SQLite dependency, keeping APK size small.
 */
class CityRepository(private val context: Context) {

    private object Keys {
        val CITIES = stringSetPreferencesKey("cities")
        val NEXT_ID = longPreferencesKey("next_id")
    }

    val cities: Flow<List<City>> = context.cityDataStore.data.map { p ->
        (p[Keys.CITIES] ?: emptySet()).mapNotNull { decode(it) }
            .sortedBy { it.name }
    }

    val homeCity: Flow<City?> = cities.map { list -> list.firstOrNull { it.isHome } }

    suspend fun addCity(city: City): Long {
        var assignedId = city.id
        context.cityDataStore.edit { p ->
            val nextId = (p[Keys.NEXT_ID] ?: 1L) + 1
            assignedId = if (city.id == 0L) nextId else city.id
            val toStore = if (city.id == 0L) city.copy(id = assignedId) else city
            val set = p[Keys.CITIES]?.toMutableSet() ?: mutableSetOf()
            // Remove any existing entry for the same id
            set.removeAll { it.startsWith("${assignedId}|") }
            set.add(encode(toStore))
            p[Keys.CITIES] = set
            if (city.id == 0L) p[Keys.NEXT_ID] = nextId
        }
        return assignedId
    }

    suspend fun removeCity(cityId: Long) {
        context.cityDataStore.edit { p ->
            val set = p[Keys.CITIES]?.toMutableSet() ?: return@edit
            set.removeAll { it.startsWith("$cityId|") }
            p[Keys.CITIES] = set
        }
    }

    suspend fun setHomeCity(cityId: Long) {
        context.cityDataStore.edit { p ->
            val set = p[Keys.CITIES]?.toMutableSet() ?: return@edit
            val updated = set.mapNotNull { decode(it) }.map { city ->
                if (city.id == cityId) city.copy(isHome = true)
                else if (city.isHome) city.copy(isHome = false)
                else city
            }.map { encode(it) }.toSet()
            p[Keys.CITIES] = updated
        }
    }

    suspend fun getCity(cityId: Long): City? {
        return cities.first().firstOrNull { it.id == cityId }
    }

    private fun encode(c: City): String =
        listOf(
            c.id.toString(),
            c.name.replace("|", " "),
            c.timeZoneId,
            c.latitude.toString(),
            c.longitude.toString(),
            c.isHome.toString(),
            c.isPreset.toString()
        ).joinToString("|")

    private fun decode(s: String): City? {
        val parts = s.split("|")
        if (parts.size < 7) return null
        return try {
            City(
                id = parts[0].toLong(),
                name = parts[1],
                timeZoneId = parts[2],
                latitude = parts[3].toDouble(),
                longitude = parts[4].toDouble(),
                isHome = parts[5].toBoolean(),
                isPreset = parts[6].toBoolean()
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Seed preset cities on first run.
     */
    suspend fun seedPresetsIfEmpty() {
        val current = cities.first()
        if (current.isNotEmpty()) return
        val presets = listOf(
            City(0, "Tehran", "Asia/Tehran", 35.6892, 51.3890, isHome = false, isPreset = true),
            City(0, "Kabul", "Asia/Kabul", 34.5553, 69.2075, isHome = false, isPreset = true),
            City(0, "Istanbul", "Europe/Istanbul", 41.0082, 28.9784, isHome = false, isPreset = true),
            City(0, "Dubai", "Asia/Dubai", 25.2048, 55.2708, isHome = false, isPreset = true),
            City(0, "London", "Europe/London", 51.5074, -0.1278, isHome = false, isPreset = true),
            City(0, "New York", "America/New_York", 40.7128, -74.0060, isHome = false, isPreset = true),
            City(0, "Tokyo", "Asia/Tokyo", 35.6762, 139.6503, isHome = false, isPreset = true)
        )
        presets.forEach { addCity(it) }
    }
}
