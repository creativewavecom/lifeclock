package com.lifeclock.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Maps each appWidgetId to the city it should display.
 *
 * Stored as a SharedPreferences entry: `widget_<id>` → `cityId`.
 *
 * If no mapping exists, the widget falls back to the user's "home" city,
 * or the first city in their list, or `null` (shows "—" placeholder).
 *
 * This keeps widget configuration lightweight and avoids requiring a full
 * configuration Activity on Android 12+ (though users can still tap the
 * widget settings button to launch the city picker).
 */
object WidgetCityBinding {

    private const val PREFS_NAME = "widget_city_bindings"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setCityForWidget(context: Context, widgetId: Int, cityId: Long) {
        prefs(context).edit { putLong("widget_$widgetId", cityId) }
    }

    fun getCityForWidget(context: Context, widgetId: Int): Long? {
        val prefs = prefs(context)
        if (!prefs.contains("widget_$widgetId")) return null
        return prefs.getLong("widget_$widgetId", -1L).takeIf { it >= 0 }
    }

    fun removeWidget(context: Context, widgetId: Int) {
        prefs(context).edit { remove("widget_$widgetId") }
    }
}
