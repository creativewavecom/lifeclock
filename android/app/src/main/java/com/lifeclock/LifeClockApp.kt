package com.lifeclock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.lifeclock.data.SettingsRepository
import com.lifeclock.domain.AppLanguage
import com.lifeclock.service.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Application class.
 *
 * Initializes:
 *  - Notification channel for the persistent life clock notification.
 *  - WorkManager-based widget refresh scheduler.
 *  - Persists user locale preference across configuration changes.
 *
 * Battery management strategy:
 *  - Uses WorkManager with a 5-minute minimum interval by default to avoid battery drain.
 *  - The user can choose a 30-second interval only when the persistent notification is enabled.
 *  - No background service is left running indefinitely — the persistent notification is
 *    rebuilt by a foreground service only when enabled by the user, and even that
 *    service uses a wake lock with a tight timeout.
 */
class LifeClockApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settings: SettingsRepository
        private set

    /** Currently-applicated language tag (used by [attachBaseContext] for child contexts). */
    @Volatile
    var currentLanguage: AppLanguage = AppLanguage.SYSTEM
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)

        createNotificationChannel()

        applicationScope.launch {
            // Apply persisted language preference on startup
            val lang = settings.language.first()
            applyLanguage(lang)

            // Schedule periodic widget updates
            WidgetUpdateScheduler.scheduleAll(this@LifeClockApp)
        }
    }

    /**
     * Wrap the base context with the user-selected locale.
     *
     * This is called every time the OS creates a new context for the app
     * (e.g. when a widget provider is invoked). Without this, Compose and
     * RemoteViews would use the system locale, ignoring the in-app setting.
     */
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("lifeclock_settings", Context.MODE_PRIVATE)
        val langId = prefs.getString("app_language", null)
        val lang = AppLanguage.fromId(langId)
        currentLanguage = lang
        val wrapped = wrapWithLocale(base, lang)
        super.attachBaseContext(wrapped)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // System locale may have changed — re-apply our app's preference
        applicationScope.launch {
            val lang = settings.language.first()
            applyLanguage(lang)
        }
    }

    /**
     * Apply the user-selected language at runtime.
     *
     * Steps:
     *   1. Persist the choice to DataStore (done by caller via SettingsRepository).
     *   2. Use AppCompatDelegate.setApplicationLocales (handles Activity recreation).
     *   3. Force widget refresh so RemoteViews pick up new strings.
     */
    fun applyLanguage(language: AppLanguage) {
        currentLanguage = language
        val localeList = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.PERSIAN -> LocaleListCompat.forLanguageTags("fa-IR")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en-US")
        }
        AppCompatDelegate.setApplicationLocales(localeList)

        // Also store to a simple SharedPreferences so attachBaseContext can read
        // synchronously (DataStore is async-only).
        getSharedPreferences("lifeclock_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", language.id)
            .apply()
    }

    /**
     * Create a context wrapper that overrides the locale. This is the trick
     * that makes stringResource() in Compose pick up the user's selected
     * language even if the system locale is different.
     */
    private fun wrapWithLocale(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val tag = when (language) {
            AppLanguage.PERSIAN -> "fa-IR"
            AppLanguage.ENGLISH -> "en-US"
            AppLanguage.SYSTEM -> return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        // For RTL layout direction
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descriptionText
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "life_clock_persistent"

        @Volatile
        private var instance: LifeClockApp? = null

        fun get(): LifeClockApp = instance ?: error("LifeClockApp not initialized")
    }
}
