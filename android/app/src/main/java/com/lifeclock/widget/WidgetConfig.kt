package com.lifeclock.widget

import androidx.annotation.IntDef

/**
 * Constants for widget sizes and a simple binding system
 * that maps appWidgetId → cityId.
 *
 * Each widget instance is bound to a city through a SharedPreferences entry
 * of the form: widget_<id> = <cityId>. When the user picks a city in
 * the configuration activity, we write this mapping.
 */
object WidgetConfig {

    const val SIZE_SMALL = 1
    const val SIZE_WIDE = 2
    const val SIZE_MEDIUM = 3
    const val SIZE_LARGE = 4
    const val SIZE_PRAYER = 5  // 4x3 — prayer times + life clock + dates

    @IntDef(SIZE_SMALL, SIZE_WIDE, SIZE_MEDIUM, SIZE_LARGE, SIZE_PRAYER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Size
}
