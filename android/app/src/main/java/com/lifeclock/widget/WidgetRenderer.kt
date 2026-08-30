package com.lifeclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.lifeclock.MainActivity
import com.lifeclock.R
import com.lifeclock.data.CityRepository
import com.lifeclock.data.SettingsRepository
import com.lifeclock.data.SunriseRepository
import com.lifeclock.domain.AppLanguage
import com.lifeclock.domain.City
import com.lifeclock.domain.LifeClockCalculator
import com.lifeclock.domain.LifeClockPeriod
import com.lifeclock.domain.PersianCalendar
import com.lifeclock.domain.SunriseAnchor
import com.lifeclock.domain.SunriseSource
import com.lifeclock.domain.TimeFormatter
import com.lifeclock.domain.WidgetTheme
import com.lifeclock.widget.WidgetConfig.SIZE_LARGE
import com.lifeclock.widget.WidgetConfig.SIZE_MEDIUM
import com.lifeclock.widget.WidgetConfig.SIZE_PRAYER
import com.lifeclock.widget.WidgetConfig.SIZE_SMALL
import com.lifeclock.widget.WidgetConfig.SIZE_WIDE
import kotlinx.coroutines.flow.first
import org.joda.time.DateTimeZone

/**
 * Renders a single widget instance with all theming + data applied.
 *
 * The widget uses [android.widget.TextClock] for the time display because
 * TextClock ticks automatically every minute without requiring a Service or
 * WorkManager callback. The life-clock offset is achieved by setting
 * TextClock.timeZone to a synthetic GMT offset that equals
 *   `city_timezone_offset + life_offset`.
 *
 *   life_offset = (9h - sunrise_local_seconds_of_day) millis
 *
 *   So if today's sunrise is at 04:57 local, life_offset = +4h03m, meaning
 *   lifeClock advances 4h03m ahead of real local time.
 *
 *   At real local 08:10 (3h13m after sunrise), lifeClock = 09:00 + 3h13m = 12:13.
 *
 *   TextClock timezone = GMT(city_offset + life_offset), e.g. for Tehran today
 *   with sunrise 04:57: GMT(3:30 + 4:03) = GMT+07:33. TextClock will tick this
 *   synthetic timezone forward automatically — zero WorkManager cost for ticking.
 */
object WidgetRenderer {

    /**
     * Build the RemoteViews for a widget instance.
     *
     * Wraps the entire render in a try-catch so that any failure (missing view
     * ID, layout parse error, etc.) returns a simple "error" widget instead of
     * leaving the user with "Can't load widget" on their home screen.
     */
    suspend fun render(
        context: Context,
        @WidgetConfig.Size widgetSize: Int,
        widgetId: Int
    ): RemoteViews {
        return try {
            renderInternal(context, widgetSize, widgetId)
        } catch (t: Throwable) {
            // Fallback: a tiny widget that just shows the app name.
            // This way the user sees *something* instead of "Can't load widget".
            try {
                val layoutRes = when (widgetSize) {
                    WidgetConfig.SIZE_SMALL, WidgetConfig.SIZE_WIDE,
                    WidgetConfig.SIZE_MEDIUM, WidgetConfig.SIZE_LARGE,
                    WidgetConfig.SIZE_PRAYER -> R.layout.widget_small
                    else -> R.layout.widget_small
                }
                val fallback = RemoteViews(context.packageName, layoutRes)
                fallback.setTextViewText(R.id.widget_city, "Life Clock")
                fallback.setTextViewText(R.id.widget_life_clock, "--:--")
                fallback.setTextViewText(R.id.widget_period, "tap to refresh")
                fallback
            } catch (_: Throwable) {
                // Last-resort: empty RemoteViews (better than crashing).
                RemoteViews(context.packageName, R.layout.widget_small)
            }
        }
    }

    private suspend fun renderInternal(
        context: Context,
        @WidgetConfig.Size widgetSize: Int,
        widgetId: Int
    ): RemoteViews {
        val settings = SettingsRepository(context)
        val cityRepo = CityRepository(context)
        val sunriseRepo = SunriseRepository(settings)

        val theme = settings.theme.first()
        val language = settings.language.first()
        val sunriseSource = settings.sunriseSource.first()

        val layoutRes = when (widgetSize) {
            SIZE_SMALL -> R.layout.widget_small
            SIZE_WIDE -> R.layout.widget_wide
            SIZE_MEDIUM -> R.layout.widget_medium
            SIZE_LARGE -> R.layout.widget_large
            SIZE_PRAYER -> R.layout.widget_prayer
            else -> R.layout.widget_small
        }

        val views = RemoteViews(context.packageName, layoutRes)

        // Determine the city bound to this widget (or fall back to home city)
        val boundCityId = WidgetCityBinding.getCityForWidget(context, widgetId)
        val city = (boundCityId?.let { cityRepo.getCity(it) })
            ?: cityRepo.homeCity.first()
            ?: cityRepo.cities.first().firstOrNull()

        if (city == null) {
            // No city available — show placeholder
            views.setTextViewText(R.id.widget_city, "—")
            applyTheme(views, theme, widgetSize, context)
            return views
        }

        val nowUtc = System.currentTimeMillis()

        // ---- Compute sunrise (for offset + sunrise/sunset display) ----
        val solarTimes = sunriseRepo.getSunrise(
            sunriseSource, city.latitude, city.longitude, nowUtc
        )
        val lastSunriseUtc = SunriseAnchor.lastSunrise(
            nowUtc, city.latitude, city.longitude
        )
        val todaySunsetUtc = solarTimes.sunsetUtcMillis

        // ---- Apply life-clock timezone to TextClock ----
        // lifeClock = UTC + (city_offset + life_offset)
        // where life_offset = (9h - sunrise_local_seconds_of_day) * 1000 ms
        val syntheticTz = LifeClockCalculator.syntheticLifeTimezoneId(
            nowUtc, city.timeZoneId, lastSunriseUtc
        )
        // Guard each RemoteViews call so a missing view doesn't break the whole widget.
        safeSetString(views, R.id.widget_life_clock, "setTimeZone", syntheticTz)

        // Official clock uses the city's real timezone (only on widgets that have it)
        if (widgetSize != SIZE_SMALL) {
            safeSetString(views, R.id.widget_official_clock, "setTimeZone", city.timeZoneId)
        }

        // ---- Set city name ----
        val cityName = if (language == AppLanguage.PERSIAN) {
            translatePresetCityName(city.name)
        } else city.name
        views.setTextViewText(R.id.widget_city, cityName.uppercase())

        // ---- Compute life-clock snapshot for period + day-progress ----
        val lifeTime = LifeClockCalculator.toLifeClock(nowUtc, city.timeZoneId, lastSunriseUtc)
        views.setTextViewText(R.id.widget_period, periodLabel(lifeTime.period, language))

        // ---- Day progress ----
        if (widgetSize == SIZE_MEDIUM || widgetSize == SIZE_LARGE) {
            val pct = (lifeTime.dayProgressRatio * 100).toInt().coerceIn(0, 100)
            views.setProgressBar(R.id.widget_progress, 100, pct, false)
            views.setTextViewText(
                R.id.widget_progress_text,
                TimeFormatter.formatDayProgress(lifeTime.dayProgressRatio, language)
            )
        }

        // ---- Sunrise / sunset (TextViews, NOT TextClock!) ----
        // These are fixed times for the day; we set them once and refresh via WorkManager daily.
        if (widgetSize == SIZE_MEDIUM || widgetSize == SIZE_LARGE) {
            // Sunrise text
            val sunriseText = lastSunriseUtc?.let {
                TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
            } ?: "—"
            views.setTextViewText(R.id.widget_sunrise, sunriseText)

            // Sunset text
            val sunsetText = todaySunsetUtc?.let {
                TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
            } ?: "—"
            views.setTextViewText(R.id.widget_sunset, sunsetText)

            // Day length
            val dayLengthMinutes = solarTimes.dayLengthMinutes
            if (dayLengthMinutes > 0) {
                views.setTextViewText(
                    R.id.widget_day_length,
                    TimeFormatter.formatDuration(dayLengthMinutes, language)
                )
            }
        }

        // ---- Persian date (large and prayer) ----
        if (widgetSize == SIZE_LARGE || widgetSize == SIZE_PRAYER) {
            val persianDate = PersianCalendar.toPersian(nowUtc, city.timeZoneId)
            views.setTextViewText(
                R.id.widget_persian_date,
                PersianCalendar.run { persianDate.formatLong() }
            )
        }

        // ---- Gregorian date (prayer only) ----
        if (widgetSize == SIZE_PRAYER) {
            val greg = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            greg.timeZone = java.util.TimeZone.getTimeZone(city.timeZoneId)
            views.setTextViewText(R.id.widget_gregorian_date, greg.format(java.util.Date(nowUtc)))
        }

        // ---- Prayer times (prayer widget only) ----
        if (widgetSize == SIZE_PRAYER) {
            val prayers = com.lifeclock.domain.IslamicPrayerCalculator.compute(
                nowUtc, city.latitude, city.longitude
            )
            prayers.fajrUtcMillis?.let {
                views.setTextViewText(
                    R.id.widget_fajr,
                    TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
                )
            } ?: views.setTextViewText(R.id.widget_fajr, "—")

            prayers.sunriseUtcMillis?.let {
                views.setTextViewText(
                    R.id.widget_sunrise,
                    TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
                )
            } ?: views.setTextViewText(R.id.widget_sunrise, "—")

            prayers.dhuhrUtcMillis?.let {
                views.setTextViewText(
                    R.id.widget_dhuhr,
                    TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
                )
            } ?: views.setTextViewText(R.id.widget_dhuhr, "—")

            prayers.asrUtcMillis?.let {
                views.setTextViewText(
                    R.id.widget_asr,
                    TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
                )
            } ?: views.setTextViewText(R.id.widget_asr, "—")

            prayers.maghribUtcMillis?.let {
                views.setTextViewText(
                    R.id.widget_sunset,
                    TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
                )
            } ?: views.setTextViewText(R.id.widget_sunset, "—")

            prayers.ishaUtcMillis?.let {
                views.setTextViewText(
                    R.id.widget_isha,
                    TimeFormatter.formatHourMinute(it, city.timeZoneId, language)
                )
            } ?: views.setTextViewText(R.id.widget_isha, "—")

            // Next prayer label
            val next = prayers.nextPrayer(nowUtc)
            if (next != null) {
                val nextName = when (language) {
                    AppLanguage.PERSIAN -> prayerNameFa(next.next)
                    else -> next.next.name.lowercase()
                }
                val remaining = next.remainingMillis
                val h = (remaining / 3_600_000L).toInt()
                val m = ((remaining % 3_600_000L) / 60_000L).toInt()
                val remainStr = if (language == AppLanguage.PERSIAN) {
                    PersianCalendar.toPersianDigits(String.format("%02d:%02d", h, m))
                } else String.format("%02d:%02d", h, m)
                views.setTextViewText(
                    R.id.widget_next_prayer_label,
                    "→ $nextName $remainStr"
                )
            }
        }

        // ---- Apply theme (background, text colors) ----
        applyTheme(views, theme, widgetSize, context)

        // ---- Click intent: open the app ----
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("lifeclock://widget/$widgetId")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // ---- Refresh button (prayer widget only) ----
        // Tapping this forces an immediate widget refresh via broadcast.
        if (widgetSize == SIZE_PRAYER) {
            val refreshIntent = Intent(context, PrayerWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
            val refreshPI = PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + 1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPI)
        }

        return views
    }

    /**
     * Apply a [WidgetTheme] to the rendered RemoteViews.
     */
    private fun applyTheme(
        views: RemoteViews,
        theme: WidgetTheme,
        @WidgetConfig.Size widgetSize: Int,
        context: Context
    ) {
        val bgRes = when (theme) {
            WidgetTheme.DIGITAL_MINIMAL -> R.drawable.bg_widget_digital
            WidgetTheme.ANALOG -> R.drawable.bg_widget_digital
            WidgetTheme.NATURE_SUN -> R.drawable.bg_widget_nature
            WidgetTheme.PERSIAN_TRADITIONAL -> R.drawable.bg_widget_persian
            WidgetTheme.GLASS_DARK -> R.drawable.bg_widget_glass
            WidgetTheme.MODERN_GRADIENT -> R.drawable.bg_widget_gradient
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

        val accentColor = when (theme) {
            WidgetTheme.DIGITAL_MINIMAL -> R.color.digital_accent
            WidgetTheme.ANALOG -> R.color.analog_hour_hand
            WidgetTheme.NATURE_SUN -> R.color.nature_text
            WidgetTheme.PERSIAN_TRADITIONAL -> R.color.persian_accent
            WidgetTheme.GLASS_DARK -> R.color.glass_accent
            WidgetTheme.MODERN_GRADIENT -> R.color.gradient_accent
        }
        val mainTextColor = when (theme) {
            WidgetTheme.PERSIAN_TRADITIONAL -> R.color.persian_text
            else -> R.color.white
        }

        safeSetColor(views, R.id.widget_life_clock, ContextCompat.getColor(context, mainTextColor))
        safeSetColor(views, R.id.widget_city, ContextCompat.getColor(context, R.color.widget_label))
        safeSetColor(views, R.id.widget_period, ContextCompat.getColor(context, accentColor))
        safeSetColor(views, R.id.widget_official_clock, ContextCompat.getColor(context, accentColor))

        if (widgetSize == SIZE_MEDIUM || widgetSize == SIZE_LARGE || widgetSize == SIZE_PRAYER) {
            safeSetColor(views, R.id.widget_sunrise, ContextCompat.getColor(context, R.color.widget_label))
            safeSetColor(views, R.id.widget_sunset, ContextCompat.getColor(context, R.color.widget_label))
            safeSetColor(views, R.id.widget_progress_text, ContextCompat.getColor(context, mainTextColor))
            try {
                views.setInt(R.id.widget_progress, "setProgressTint", ContextCompat.getColor(context, accentColor))
            } catch (_: Throwable) {}
        }

        if (widgetSize == SIZE_LARGE || widgetSize == SIZE_PRAYER) {
            safeSetColor(views, R.id.widget_persian_date, ContextCompat.getColor(context, R.color.widget_label))
            safeSetColor(views, R.id.widget_day_length, ContextCompat.getColor(context, mainTextColor))
        }

        // Prayer widget extras
        if (widgetSize == SIZE_PRAYER) {
            safeSetColor(views, R.id.widget_fajr, ContextCompat.getColor(context, mainTextColor))
            safeSetColor(views, R.id.widget_dhuhr, ContextCompat.getColor(context, mainTextColor))
            safeSetColor(views, R.id.widget_asr, ContextCompat.getColor(context, mainTextColor))
            safeSetColor(views, R.id.widget_isha, ContextCompat.getColor(context, mainTextColor))
            safeSetColor(views, R.id.widget_next_prayer_label, ContextCompat.getColor(context, accentColor))
            safeSetColor(views, R.id.widget_gregorian_date, ContextCompat.getColor(context, R.color.widget_label))
        }
    }

    private fun periodLabel(period: LifeClockPeriod, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.PERSIAN -> when (period) {
                LifeClockPeriod.SUNRISE -> "طلوع"
                LifeClockPeriod.MORNING -> "صبح"
                LifeClockPeriod.NOON -> "ظهر"
                LifeClockPeriod.AFTERNOON -> "بعدازظهر"
                LifeClockPeriod.SUNSET -> "غروب"
                LifeClockPeriod.EVENING -> "شب اول"
                LifeClockPeriod.NIGHT -> "شب"
            }
            else -> "• ${period.key.uppercase()}"
        }
    }

    /** Translate preset city names to Persian for Persian UI. */
    private fun translatePresetCityName(name: String): String = when (name.lowercase()) {
        "tehran" -> "تهران"
        "kabul" -> "کابل"
        "istanbul" -> "استانبول"
        "dubai" -> "دبی"
        "london" -> "لندن"
        "new york" -> "نیویورک"
        "tokyo" -> "توکیو"
        "my location" -> "موقعیت من"
        else -> name
    }

    /** Persian prayer names. */
    private fun prayerNameFa(slot: com.lifeclock.domain.PrayerSlot): String = when (slot) {
        com.lifeclock.domain.PrayerSlot.FAJR -> "اذان صبح"
        com.lifeclock.domain.PrayerSlot.SUNRISE -> "طلوع"
        com.lifeclock.domain.PrayerSlot.DHUHR -> "اذان ظهر"
        com.lifeclock.domain.PrayerSlot.ASR -> "اذان عصر"
        com.lifeclock.domain.PrayerSlot.MAGHRIB -> "اذان مغرب"
        com.lifeclock.domain.PrayerSlot.ISHA -> "اذان عشاء"
    }

    /**
     * Wraps [RemoteViews.setString] in a try-catch so that a missing view ID
     * (which would throw IllegalArgumentException on some Android versions)
     * doesn't crash the entire widget render.
     */
    private fun safeSetString(views: RemoteViews, viewId: Int, methodName: String, value: String) {
        try {
            views.setString(viewId, methodName, value)
        } catch (_: Throwable) {
            // Best-effort: skip if the view doesn't exist in this layout.
        }
    }

    /** Wraps [RemoteViews.setTextViewText] in a try-catch (same rationale). */
    private fun safeSetText(views: RemoteViews, viewId: Int, text: CharSequence) {
        try {
            views.setTextViewText(viewId, text)
        } catch (_: Throwable) {
            // Skip if the view doesn't exist in this layout.
        }
    }

    /** Wraps [RemoteViews.setTextColor] in a try-catch. */
    private fun safeSetColor(views: RemoteViews, viewId: Int, color: Int) {
        try {
            views.setTextColor(viewId, color)
        } catch (_: Throwable) {
            // Skip if the view doesn't exist in this layout.
        }
    }
}
