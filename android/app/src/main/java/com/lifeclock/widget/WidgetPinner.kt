package com.lifeclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.lifeclock.widget.WidgetConfig.SIZE_LARGE
import com.lifeclock.widget.WidgetConfig.SIZE_MEDIUM
import com.lifeclock.widget.WidgetConfig.SIZE_SMALL
import com.lifeclock.widget.WidgetConfig.SIZE_WIDE

/**
 * Helper for pinning widgets to the home screen from inside the app.
 *
 * On Android 8+ (API 26+), we use [AppWidgetManager.requestCardAppWidget] which
 * shows the system's "place widget" UI. The user can resize / place the widget
 * like any other widget, but they don't have to leave the app to find it.
 *
 * On older Android versions (we still support API 24), there's no equivalent
 * API, so we just open the system widget picker (which on most launchers can
 * also be reached by long-pressing an empty space on the home screen).
 *
 * Each widget size has its own provider ComponentName — we offer the user a
 * size picker before launching the pin request.
 */
object WidgetPinner {

    /**
     * Try to pin a widget of the given size to the home screen.
     * Returns true if the pin request was launched, false if not supported.
     */
    fun requestPin(
        context: Context,
        @WidgetConfig.Size size: Int
    ): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        val provider = providerForSize(context, size) ?: return false

        // Only API 26+ supports requestCardAppWidget
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Fallback: open the system widget picker (best effort)
            return openWidgetPicker(context)
        }

        // Check that the launcher supports pinning
        if (!mgr.isRequestPinAppWidgetSupported) {
            return openWidgetPicker(context)
        }

        // Build a "success" callback that we'll get back when the user confirms
        val successIntent = Intent(context, WidgetPinReceiver::class.java).apply {
            action = "com.lifeclock.WIDGET_PINNED"
        }
        val successPI = PendingIntent.getBroadcast(
            context,
            size,
            successIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Optional extras for the widget host — we can pass the bound city id later
        val extras = Bundle().apply {
            // Reserved for future use (e.g. pre-binding to a city)
        }

        return try {
            mgr.requestPinAppWidget(provider, extras, successPI)
            true
        } catch (_: SecurityException) {
            // Some OEMs lock this down — fall back to opening widget picker
            openWidgetPicker(context)
        }
    }

    /**
     * Fall back: open the system widget picker (if available).
     * On most launchers this opens the "Widgets" panel where the user can
     * find "Life Clock" and drag a widget to the home screen.
     */
    private fun openWidgetPicker(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_PICK)
                .setClassName(
                    "com.android.launcher",
                    "com.android.launcher.LauncherAppWidgetPicker"
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Even the picker isn't available — user has to do it manually.
            false
        }
    }

    private fun providerForSize(
        context: Context,
        @WidgetConfig.Size size: Int
    ): ComponentName? = when (size) {
        SIZE_SMALL -> ComponentName(context, SmallWidgetReceiver::class.java)
        SIZE_WIDE -> ComponentName(context, WideWidgetReceiver::class.java)
        SIZE_MEDIUM -> ComponentName(context, MediumWidgetReceiver::class.java)
        SIZE_LARGE -> ComponentName(context, LargeWidgetReceiver::class.java)
        WidgetConfig.SIZE_PRAYER -> ComponentName(context, PrayerWidgetReceiver::class.java)
        else -> null
    }
}
