package com.lifeclock.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lifeclock.LifeClockApp
import com.lifeclock.MainActivity
import com.lifeclock.R
import com.lifeclock.data.CityRepository
import com.lifeclock.data.SettingsRepository
import com.lifeclock.domain.LifeClockCalculator
import com.lifeclock.domain.TimeFormatter
import com.lifeclock.domain.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that shows a persistent notification with the life clock.
 *
 * Battery strategy:
 *  - The service is *only* started when the user explicitly enables the
 *    persistent notification in Settings. Default is OFF.
 *  - When running, it ticks every 30 seconds by default (configurable).
 *  - Uses [LifecycleService] so we can use coroutines tied to the service lifecycle.
 *  - No location fetch — uses the saved home city.
 *  - No internet — uses offline NOAA math.
 *
 * On Android 14+ we use foregroundServiceType="specialUse" (declared in manifest).
 */
class LifeClockNotificationService : LifecycleService() {

    private var tickJob: Job? = null
    private lateinit var settings: SettingsRepository
    private lateinit var cityRepo: CityRepository

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(applicationContext)
        cityRepo = CityRepository(applicationContext)
        // Build an initial non-async notification synchronously for startForeground.
        startForeground(NOTIF_ID, buildInitialNotification())
        startTicking()
    }

    /**
     * Lightweight synchronous notification used only for the startForeground call.
     * The real content is filled in shortly after by [buildNotification] via [startTicking].
     */
    private fun buildInitialNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, LifeClockApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.life_clock))
            .setContentText("--:--")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = lifecycleScope.launch {
            while (true) {
                val notif = buildNotification()
                val mgr = getSystemService(NotificationManager::class.java)
                mgr.notify(NOTIF_ID, notif)
                // Default tick = 30 seconds. Use settings.frequency if needed.
                val freq = settings.frequency.first()
                delay(freq.seconds * 1000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY  // restart if killed by the OS
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        tickJob?.cancel()
        super.onDestroy()
    }

    private suspend fun buildNotification(): Notification {
        val cityId = settings.notificationCityId.first()
        val city = cityRepo.cities.first().firstOrNull { it.id == cityId }
            ?: cityRepo.homeCity.first()
            ?: cityRepo.cities.first().firstOrNull()

        val lang = settings.language.first()

        val lifeTime = if (city != null) {
            val lastSunrise = com.lifeclock.domain.SunriseAnchor.lastSunrise(
                System.currentTimeMillis(), city.latitude, city.longitude
            )
            LifeClockCalculator.toLifeClock(
                System.currentTimeMillis(), city.timeZoneId, lastSunrise
            )
        } else null

        val contentText = if (city != null && lifeTime != null) {
            val official = TimeFormatter.formatHourMinute(System.currentTimeMillis(), city.timeZoneId, lang)
            val lifeStr = if (lang == AppLanguage.PERSIAN)
                com.lifeclock.domain.PersianCalendar.toPersianDigits(lifeTime.formatted)
            else lifeTime.formatted
            val offStr = if (lang == AppLanguage.PERSIAN)
                com.lifeclock.domain.PersianCalendar.toPersianDigits(official)
            else official
            if (lang == AppLanguage.PERSIAN) {
                "ساعت زندگی: $lifeStr  •  رسمی: $offStr"
            } else {
                "Life: $lifeStr  •  Official: $offStr"
            }
        } else {
            getString(R.string.no_cities_yet)
        }

        val title = if (lang == AppLanguage.PERSIAN) "ساعت زندگی" else "Life Clock"

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LifeClockApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    companion object {
        const val NOTIF_ID = 4201

        const val ACTION_START = "com.lifeclock.START_NOTIFICATION"
        const val ACTION_STOP = "com.lifeclock.STOP_NOTIFICATION"

        fun start(context: Context) {
            val intent = Intent(context, LifeClockNotificationService::class.java)
            intent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LifeClockNotificationService::class.java)
            context.stopService(intent)
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.cancel(NOTIF_ID)
        }
    }
}

/**
 * Helper that refreshes the notification immediately when settings change
 * (e.g., user picked a different city).
 */
object NotificationRefreshHelper {
    suspend fun refresh(context: Context) {
        val settings = SettingsRepository(context)
        val enabled = settings.notificationEnabled.first()
        if (enabled) {
            LifeClockNotificationService.start(context)
        } else {
            LifeClockNotificationService.stop(context)
        }
    }
}
