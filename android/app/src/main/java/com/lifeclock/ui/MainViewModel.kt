package com.lifeclock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeclock.data.CityRepository
import com.lifeclock.data.LocationRepository
import com.lifeclock.data.SettingsRepository
import com.lifeclock.domain.AppLanguage
import com.lifeclock.domain.City
import com.lifeclock.domain.SunriseSource
import com.lifeclock.domain.UpdateFrequency
import com.lifeclock.domain.WidgetTheme
import com.lifeclock.service.NotificationRefreshHelper
import com.lifeclock.service.WidgetUpdateScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single ViewModel shared across all screens.
 *
 * Exposes:
 *  - The list of saved cities (as StateFlow).
 *  - Current settings (theme, frequency, language, notification toggle, etc.).
 *  - Actions to add/remove/home a city, change settings, refresh widgets.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val cityRepo = CityRepository(app)
    private val locationRepo = LocationRepository(app)

    val cities: StateFlow<List<City>> = cityRepo.cities
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val theme: StateFlow<WidgetTheme> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Lazily, WidgetTheme.DIGITAL_MINIMAL)

    val frequency: StateFlow<UpdateFrequency> = settings.frequency
        .stateIn(viewModelScope, SharingStarted.Lazily, UpdateFrequency.EVERY_1_MINUTE)

    val sunriseSource: StateFlow<SunriseSource> = settings.sunriseSource
        .stateIn(viewModelScope, SharingStarted.Lazily, SunriseSource.HYBRID)

    val language: StateFlow<AppLanguage> = settings.language
        .stateIn(viewModelScope, SharingStarted.Lazily, AppLanguage.SYSTEM)

    val notificationEnabled: StateFlow<Boolean> = settings.notificationEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // ---- City actions ----

    fun addCity(city: City, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = cityRepo.addCity(city)
            refreshWidgets()
            onSaved(id)
        }
    }

    fun removeCity(city: City) {
        viewModelScope.launch {
            cityRepo.removeCity(city.id)
            refreshWidgets()
        }
    }

    fun setHomeCity(city: City) {
        viewModelScope.launch {
            cityRepo.setHomeCity(city.id)
            refreshWidgets()
        }
    }

    fun useCurrentLocationAsHome() {
        viewModelScope.launch {
            val location = locationRepo.getCurrentLocation() ?: return@launch
            val city = locationRepo.resolveCity(location) ?: return@launch
            // Mark old home city as non-home, then add new home
            val existingHome = cityRepo.homeCity.first()
            if (existingHome != null) {
                cityRepo.removeCity(existingHome.id)
            }
            cityRepo.addCity(city)
            refreshWidgets()
        }
    }

    // ---- Settings actions ----

    fun setTheme(theme: WidgetTheme) {
        viewModelScope.launch {
            settings.setTheme(theme)
            refreshWidgets()
        }
    }

    fun setFrequency(frequency: UpdateFrequency) {
        viewModelScope.launch {
            settings.setFrequency(frequency)
            WidgetUpdateScheduler.scheduleWidgetRefresh(getApplication())
        }
    }

    fun setSunriseSource(source: SunriseSource) {
        viewModelScope.launch {
            settings.setSunriseSource(source)
            refreshWidgets()
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settings.setLanguage(language)
            // Apply via the Application class — this updates context wrappers
            // for all future widget receivers and the activity itself.
            (getApplication() as com.lifeclock.LifeClockApp).applyLanguage(language)
            refreshWidgets()
            // Trigger Activity recreation so Compose picks up the new locale.
            // We post it to the main thread so it happens after the current coroutine tick.
            val app = getApplication<Application>()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                app.startActivity(
                    android.content.Intent(app, com.lifeclock.MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
                    }
                )
            }
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNotificationEnabled(enabled)
            NotificationRefreshHelper.refresh(getApplication())
        }
    }

    fun setNotificationCity(cityId: Long) {
        viewModelScope.launch {
            settings.setNotificationCityId(cityId)
            NotificationRefreshHelper.refresh(getApplication())
        }
    }

    // ---- Bootstrap ----

    fun seedPresetsIfEmpty() {
        viewModelScope.launch {
            cityRepo.seedPresetsIfEmpty()
        }
    }

    /**
     * Force an immediate refresh of all widgets + notification, bypassing
     * the WorkManager schedule. Triggered by the in-app "Refresh" button.
     */
    fun refreshAllNow() {
        viewModelScope.launch {
            refreshWidgets()
            NotificationRefreshHelper.refresh(getApplication())
        }
    }

    private fun refreshWidgets() {
        com.lifeclock.widget.BaseWidgetReceiver.updateAll(getApplication())
    }

    // ---- Location permission ----

    fun hasLocationPermission(): Boolean = locationRepo.hasLocationPermission()

    companion object {
        object Factory : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                throw IllegalArgumentException("Use AndroidViewModelFactory with Application")
            }
        }

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(app) as T
                }
            }
    }
}
