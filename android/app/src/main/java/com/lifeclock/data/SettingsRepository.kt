package com.lifeclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifeclock.domain.AppLanguage
import com.lifeclock.domain.SunriseSource
import com.lifeclock.domain.UpdateFrequency
import com.lifeclock.domain.WidgetTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeclock_settings")

/**
 * Stores all app-level settings (theme, language, frequency, etc.) in DataStore.
 *
 * DataStore is async, type-safe, and survives app updates — perfect for a
 * settings store that the widget receivers read on every refresh.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("widget_theme")
        val FREQUENCY = stringPreferencesKey("update_frequency")
        val SUNRISE_SOURCE = stringPreferencesKey("sunrise_source")
        val LANGUAGE = stringPreferencesKey("app_language")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val NOTIFICATION_CITY_ID = longPreferencesKey("notification_city_id")
        val LAST_SUNRISE_SYNC = longPreferencesKey("last_sunrise_sync")
        val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
    }

    val theme: Flow<WidgetTheme> = context.dataStore.data.map { p ->
        WidgetTheme.fromId(p[Keys.THEME])
    }

    val frequency: Flow<UpdateFrequency> = context.dataStore.data.map { p ->
        UpdateFrequency.fromId(p[Keys.FREQUENCY])
    }

    val sunriseSource: Flow<SunriseSource> = context.dataStore.data.map { p ->
        SunriseSource.fromId(p[Keys.SUNRISE_SOURCE])
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { p ->
        AppLanguage.fromId(p[Keys.LANGUAGE])
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.NOTIFICATION_ENABLED] ?: false
    }

    val notificationCityId: Flow<Long?> = context.dataStore.data.map { p ->
        p[Keys.NOTIFICATION_CITY_ID]
    }

    val lastSunriseSync: Flow<Long> = context.dataStore.data.map { p ->
        p[Keys.LAST_SUNRISE_SYNC] ?: 0L
    }

    suspend fun setTheme(theme: WidgetTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.id }
    }

    suspend fun setFrequency(frequency: UpdateFrequency) {
        context.dataStore.edit { it[Keys.FREQUENCY] = frequency.id }
    }

    suspend fun setSunriseSource(source: SunriseSource) {
        context.dataStore.edit { it[Keys.SUNRISE_SOURCE] = source.id }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.id }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setNotificationCityId(cityId: Long?) {
        context.dataStore.edit {
            if (cityId == null) it.remove(Keys.NOTIFICATION_CITY_ID)
            else it[Keys.NOTIFICATION_CITY_ID] = cityId
        }
    }

    suspend fun markSunriseSyncNow() {
        context.dataStore.edit { it[Keys.LAST_SUNRISE_SYNC] = System.currentTimeMillis() }
    }

    suspend fun markFirstRunDone() {
        context.dataStore.edit { it[Keys.FIRST_RUN_DONE] = true }
    }

    suspend fun isFirstRunDone(): Boolean {
        var done = false
        context.dataStore.edit {
            done = it[Keys.FIRST_RUN_DONE] ?: false
        }
        return done
    }
}
