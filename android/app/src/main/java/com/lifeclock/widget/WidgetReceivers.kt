package com.lifeclock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifeclock.service.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base class for all 4 widget receivers (small, wide, medium, large).
 *
 * Responsibilities:
 *  1. On UPDATE: render the widget via [WidgetRenderer] and push the RemoteViews.
 *  2. On UPDATE: reschedule WorkManager so periodic refresh keeps running.
 *  3. On DELETE: clean up the city binding for this widgetId.
 *
 * Battery management:
 *  - We never start a long-running service from here.
 *  - We rely on WorkManager to do periodic refresh, which the OS batches with
 *    other apps to minimize wakeups.
 *  - TextClock handles the actual ticking display, so even between WorkManager
 *    callbacks the displayed time stays accurate.
 */
abstract class BaseWidgetReceiver : AppWidgetProvider() {

    @WidgetConfig.Size
    abstract fun widgetSize(): Int

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            scope.launch {
                val views = WidgetRenderer.render(context, widgetSize(), widgetId)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
        // Make sure the periodic refresh worker is scheduled
        scope.launch { WidgetUpdateScheduler.scheduleWidgetRefresh(context) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetCityBinding.removeWidget(context, it) }
    }

    override fun onEnabled(context: Context) {
        // First widget of this kind was placed — schedule periodic updates
        scope.launch { WidgetUpdateScheduler.scheduleWidgetRefresh(context) }
    }

    override fun onDisabled(context: Context) {
        // Last widget of this kind was removed — keep the worker running if
        // any other widget sizes still exist or notification is enabled.
        scope.launch { WidgetUpdateScheduler.scheduleWidgetRefresh(context) }
    }

        companion object {
        /**
         * Force-update all instances of all 5 widget providers.
         * Used by the WorkManager worker and by the city picker activity.
         */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            listOf(
                SmallWidgetReceiver::class.java,
                WideWidgetReceiver::class.java,
                MediumWidgetReceiver::class.java,
                LargeWidgetReceiver::class.java,
                PrayerWidgetReceiver::class.java
            ).forEach { cls ->
                val cn = ComponentName(context, cls)
                val ids = mgr.getAppWidgetIds(cn)
                ids.forEach { id ->
                    mgr.updateAppWidget(id, kotlinx.coroutines.runBlocking {
                        WidgetRenderer.render(context, sizeForClass(cls), id)
                    })
                }
            }
        }

        private fun sizeForClass(cls: Class<*>): Int = when (cls) {
            SmallWidgetReceiver::class.java -> WidgetConfig.SIZE_SMALL
            WideWidgetReceiver::class.java -> WidgetConfig.SIZE_WIDE
            MediumWidgetReceiver::class.java -> WidgetConfig.SIZE_MEDIUM
            LargeWidgetReceiver::class.java -> WidgetConfig.SIZE_LARGE
            PrayerWidgetReceiver::class.java -> WidgetConfig.SIZE_PRAYER
            else -> WidgetConfig.SIZE_SMALL
        }
    }
}

class SmallWidgetReceiver : BaseWidgetReceiver() {
    override fun widgetSize() = WidgetConfig.SIZE_SMALL
}

class WideWidgetReceiver : BaseWidgetReceiver() {
    override fun widgetSize() = WidgetConfig.SIZE_WIDE
}

class MediumWidgetReceiver : BaseWidgetReceiver() {
    override fun widgetSize() = WidgetConfig.SIZE_MEDIUM
}

class LargeWidgetReceiver : BaseWidgetReceiver() {
    override fun widgetSize() = WidgetConfig.SIZE_LARGE
}

class PrayerWidgetReceiver : BaseWidgetReceiver() {
    override fun widgetSize() = WidgetConfig.SIZE_PRAYER
}

/**
 * Receiver for the "widget pinned" success callback.
 *
 * When [WidgetPinner.requestPin] succeeds, the system fires this broadcast
 * with EXTRA_APPWIDGET_ID. We use it to do an immediate first-render of the
 * pinned widget so the user sees correct content right away (instead of the
 * system's default "place widget" preview).
 */
class WidgetPinReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.lifeclock.WIDGET_PINNED") return
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (widgetId < 0) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default).launch {
            // Force-update all widget providers — cheap and correct
            BaseWidgetReceiver.updateAll(context)
        }
    }
}
