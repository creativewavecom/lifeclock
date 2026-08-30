package com.lifeclock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeclock.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts the notification service after device reboot or app update.
 *
 * Does NOT auto-start — only restarts if the user has explicitly enabled
 * the persistent notification in Settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = SettingsRepository(context)
                if (settings.notificationEnabled.first()) {
                    LifeClockNotificationService.start(context)
                }
                WidgetUpdateScheduler.scheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
