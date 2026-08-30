package com.lifeclock.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifeclock.data.SettingsRepository
import com.lifeclock.domain.UpdateFrequency
import com.lifeclock.widget.LargeWidgetReceiver
import com.lifeclock.widget.MediumWidgetReceiver
import com.lifeclock.widget.SmallWidgetReceiver
import com.lifeclock.widget.WideWidgetReceiver
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that refreshes all widget instances and rebuilds
 * the persistent notification (if enabled).
 *
 * Battery strategy:
 *  - Uses [PeriodicWorkRequest] with [ExistingPeriodicWorkPolicy.UPDATE]
 *    so a single instance of this worker runs at the chosen interval.
 *  - Min interval is 15 minutes (WorkManager hard limit). For 30-second /
 *    1-minute intervals we fall back to a foreground service that ticks
 *    the notification (foreground services have no minimum interval).
 *  - Each refresh is a quick computation (NOAA sunrise + simple arithmetic),
 *    plus at most one optional API call (sunrise-sunset.org, weekly).
 *  - No location fetch from here — widgets show cities bound to them,
 *    not the current device location.
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Refresh every widget instance
        val classes = listOf(
            SmallWidgetReceiver::class.java,
            WideWidgetReceiver::class.java,
            MediumWidgetReceiver::class.java,
            LargeWidgetReceiver::class.java
        )
        val mgr = AppWidgetManager.getInstance(applicationContext)
        classes.forEach { cls ->
            val cn = ComponentName(applicationContext, cls)
            val ids = mgr.getAppWidgetIds(cn)
            // Force a fresh UPDATE broadcast by calling the receiver directly
            val intent = android.content.Intent(applicationContext, cls).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            applicationContext.sendBroadcast(intent)
        }

        // Refresh notification text
        NotificationRefreshHelper.refresh(applicationContext)

        return Result.success()
    }
}

/**
 * Schedules the periodic widget refresh worker based on user-chosen frequency.
 *
 * WorkManager 15-min minimum is enforced by the platform. For shorter intervals,
 * the foreground notification service takes over the ticking duty.
 */
object WidgetUpdateScheduler {

    private const val PERIODIC_TAG = "lifeclock_widget_refresh"

    suspend fun scheduleAll(context: Context) {
        scheduleWidgetRefresh(context)
        // Also make sure the notification service is running if enabled
        NotificationRefreshHelper.refresh(context)
    }

    suspend fun scheduleWidgetRefresh(context: Context) {
        val settings = SettingsRepository(context)
        val freq = settings.frequency.first()

        // WorkManager minimum period is 15 minutes — clamp to that.
        val intervalMin = (freq.seconds / 60L).coerceAtLeast(15L)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            intervalMin, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_TAG)
    }
}
