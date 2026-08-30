package com.lifeclock.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * One saved city/location.
 *
 * Each city carries its own IANA timezone ID and lat/lon — both are needed
 * because the timezone drives the official clock and lat/lon drive the
 * real sunrise/sunset for the supplementary info.
 */
@Parcelize
data class City(
    val id: Long,                  // stable identifier (DataStore-assigned)
    val name: String,              // user-visible name
    val timeZoneId: String,       // IANA ID, e.g. "Asia/Tehran"
    val latitude: Double,
    val longitude: Double,
    val isHome: Boolean = false,   // marks the auto-detected home location
    val isPreset: Boolean = false   // marks preset built-in cities
) : Parcelable
