package com.lifeclock.domain

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.Locale

/**
 * Helpers for formatting official times and sunrise/sunset times.
 *
 * Kept in the domain layer because the formatting must be consistent between
 * the app UI (Compose) and the widget RemoteViews.
 */
object TimeFormatter {

    /** "HH:MM" in the given timezone */
    fun formatHourMinute(utcMillis: Long, timeZoneId: String): String {
        val dt = DateTime(utcMillis, DateTimeZone.forID(timeZoneId))
        return DateTimeFormat.forPattern("HH:mm").print(dt)
    }

    /** "HH:MM" in the given timezone, but localized to Persian digits if language is fa */
    fun formatHourMinute(utcMillis: Long, timeZoneId: String, language: AppLanguage): String {
        val lat = formatHourMinute(utcMillis, timeZoneId)
        return when (language) {
            AppLanguage.PERSIAN -> PersianCalendar.toPersianDigits(lat)
            else -> lat
        }
    }

    /** "HHh MMm" day-length style, localized */
    fun formatDuration(minutes: Int, language: AppLanguage): String {
        if (minutes <= 0) return when (language) {
            AppLanguage.PERSIAN -> "—"
            else -> "—"
        }
        val h = minutes / 60
        val m = minutes % 60
        val s = when (language) {
            AppLanguage.PERSIAN -> String.format(Locale.US, "%d ساعت و %d دقیقه", h, m)
            else -> String.format(Locale.US, "%dh %dm", h, m)
        }
        return when (language) {
            AppLanguage.PERSIAN -> PersianCalendar.toPersianDigits(s)
            else -> s
        }
    }

    /** Day-progress percent (0..100, integer) */
    fun formatDayProgress(ratio: Double, language: AppLanguage): String {
        val pct = (ratio * 100).toInt().coerceIn(0, 100)
        val s = when (language) {
            AppLanguage.PERSIAN -> "$pct٪"
            else -> "$pct%"
        }
        return when (language) {
            AppLanguage.PERSIAN -> PersianCalendar.toPersianDigits(s)
            else -> s
        }
    }
}
