package com.lifeclock.domain

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.Locale

/**
 * Core Life Clock calculator.
 *
 * Definition (per user spec):
 *   - "Life clock 09:00 always equals the actual sunrise time."
 *
 * This is NOT a fixed offset — the offset depends on the actual sunrise time
 * for the city on the current day. As sunrise shifts earlier/later through the
 * year, the life clock automatically adjusts so that the moment of real sunrise
 * always shows as 09:00 on the life clock.
 *
 * Math:
 *   Given the most recent sunrise (in local time-of-day seconds):
 *     lifeOffsetSeconds = 9*3600 - sunriseLocalSeconds
 *
 *   So if sunrise is at 04:57 local, lifeOffset = +4h03m, meaning
 *   lifeClock advances 4h03m ahead of real local time.
 *
 *   At real local 08:10 (3h13m after sunrise), lifeClock = 09:00 + 3h13m = 12:13.
 *
 * The offset is fixed for the duration of one "life day" (from one sunrise to
 * the next). After sunrise next day, the offset recomputes to match the new
 * sunrise time.
 *
 * For widget display, this offset is realized as a synthetic GMT timezone ID
 * (e.g. "GMT+07:33") passed to TextClock. This lets TextClock tick on its own
 * (zero battery cost for ticking) while still showing the correct life clock.
 */
object LifeClockCalculator {

    /** The life-clock hour at which sunrise occurs. */
    const val SUNRISE_LIFE_HOUR: Int = 9

    /** Seconds from midnight on the life clock at which sunrise falls. */
    const val SUNRISE_LIFE_SECONDS: Int = SUNRISE_LIFE_HOUR * 3600  // 32400

    /**
     * Compute the life-clock offset in milliseconds for a given city/day.
     *
     * The offset is relative to the city's local time — i.e.
     *   lifeClock_local = real_local + offset
     *
     * Caller passes the most recent sunrise (UTC millis) — typically today's
     * sunrise, or yesterday's if we're before today's sunrise.
     *
     * Returns null if sunrise is unavailable (e.g. polar regions during
     * polar night/day) — in that case we fall back to a +0 offset.
     */
    fun lifeOffsetMillis(realUtcMillis: Long, timeZoneId: String, sunriseUtcMillis: Long?): Long {
        if (sunriseUtcMillis == null) return 0L
        val tz = DateTimeZone.forID(timeZoneId)
        // Convert sunrise UTC instant → local seconds-of-day
        val sunriseLocal = DateTime(sunriseUtcMillis, tz)
        val sunriseLocalSeconds = sunriseLocal.hourOfDay * 3600 +
            sunriseLocal.minuteOfHour * 60 +
            sunriseLocal.secondOfMinute
        val offsetSeconds = (SUNRISE_LIFE_SECONDS - sunriseLocalSeconds).toLong()
        return offsetSeconds * 1000L
    }

    /**
     * Convert a real instant (in UTC millis) to the life clock time
     * for a city in the given timezone, given today's sunrise.
     *
     * @param realUtcMillis   UTC milliseconds since epoch (real wall-clock time)
     * @param timeZoneId      IANA timezone ID, e.g. "Asia/Tehran"
     * @param sunriseUtcMillis  Today's (or last) sunrise as UTC millis. If null,
     *                          falls back to a +0 offset (life clock == real time).
     */
    fun toLifeClock(
        realUtcMillis: Long,
        timeZoneId: String,
        sunriseUtcMillis: Long? = null
    ): LifeClockTime {
        val tz = DateTimeZone.forID(timeZoneId)
        val localReal = DateTime(realUtcMillis, tz)

        val lifeOffsetMs = lifeOffsetMillis(realUtcMillis, timeZoneId, sunriseUtcMillis)
        val lifeInstant = realUtcMillis + lifeOffsetMs
        val lifeLocal = DateTime(lifeInstant, tz)

        val lifeSecondsOfDay =
            lifeLocal.hourOfDay * 3600 + lifeLocal.minuteOfHour * 60 + lifeLocal.secondOfMinute
        val dayProgressRatio = lifeSecondsOfDay / 86_400.0

        return LifeClockTime(
            hours = lifeLocal.hourOfDay,
            minutes = lifeLocal.minuteOfHour,
            seconds = lifeLocal.secondOfMinute,
            formatted = formatTime(lifeLocal.hourOfDay, lifeLocal.minuteOfHour),
            formattedWithSeconds = formatTimeWithSeconds(
                lifeLocal.hourOfDay, lifeLocal.minuteOfHour, lifeLocal.secondOfMinute
            ),
            period = periodOf(lifeLocal.hourOfDay),
            dayProgressRatio = dayProgressRatio,
            realLocalTime = localReal,
            lifeInstant = lifeLocal,
            lifeOffsetMillis = lifeOffsetMs
        )
    }

    /** Format HH:MM (24-hour) */
    private fun formatTime(h: Int, m: Int): String =
        String.format(Locale.US, "%02d:%02d", h, m)

    /** Format HH:MM:SS (24-hour) */
    private fun formatTimeWithSeconds(h: Int, m: Int, s: Int): String =
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)

    /**
     * Classify the life-clock hour into a human-readable period.
     * Life clock 09:00 = sunrise, ~21:00 = sunset (varies by city/day).
     */
    fun periodOf(lifeHour: Int): LifeClockPeriod = when (lifeHour) {
        in 7..9 -> LifeClockPeriod.SUNRISE
        in 10..12 -> LifeClockPeriod.MORNING
        in 13..14 -> LifeClockPeriod.NOON
        in 15..18 -> LifeClockPeriod.AFTERNOON
        in 19..21 -> LifeClockPeriod.SUNSET
        in 22..23 -> LifeClockPeriod.EVENING
        in 0..3 -> LifeClockPeriod.NIGHT
        else -> LifeClockPeriod.NIGHT
    }

    /**
     * Format a real instant as a localized official-time string (HH:MM).
     */
    fun formatOfficialTime(realUtcMillis: Long, timeZoneId: String): String {
        val tz = DateTimeZone.forID(timeZoneId)
        val dt = DateTime(realUtcMillis, tz)
        val fmt = DateTimeFormat.forPattern("HH:mm").withZone(tz)
        return fmt.print(dt)
    }

    /**
     * Build a synthetic GMT timezone ID for use with TextClock.
     *
     * TextClock.setTimeZone(s) expects a Java timezone ID. We compute the
     * total offset (city_offset + life_offset) and format it as "GMT±HH:MM".
     *
     * When TextClock ticks in this synthetic timezone, it shows life clock time
     * because: lifeClock = UTC + (city_offset + life_offset) = local + life_offset
     */
    fun syntheticLifeTimezoneId(
        realUtcMillis: Long,
        timeZoneId: String,
        sunriseUtcMillis: Long?
    ): String {
        val tz = DateTimeZone.forID(timeZoneId)
        val cityOffsetMs = tz.getOffset(realUtcMillis).toLong()
        val lifeOffsetMs = lifeOffsetMillis(realUtcMillis, timeZoneId, sunriseUtcMillis)
        val totalMs = cityOffsetMs + lifeOffsetMs
        return syntheticGmtId(totalMs)
    }

    /** Build "GMT+HH:MM" / "GMT-HH:MM" from offset millis. */
    fun syntheticGmtId(offsetMillis: Long): String {
        // Java accepts "GMT+HH:MM" universally. The 3-second "GMT+HH:MM:SS" format
        // is technically valid in Java but some Android versions reject it inside
        // TextClock.setTimeZone — which causes the widget to fail with "Can't load
        // widget". To stay safe across all OEM ROMs, we round to the nearest minute.
        // The drift this introduces is ≤ 30 seconds/day, well below the ±2-minute
        // error of the NOAA sunrise formula itself.
        val totalMinutes = (offsetMillis + 30_000L) / 60_000L  // round to nearest minute
        val sign = if (totalMinutes >= 0) "+" else "-"
        val absMin = kotlin.math.abs(totalMinutes.toInt())
        val h = absMin / 60
        val m = absMin % 60
        return String.format("GMT%s%02d:%02d", sign, h, m)
    }
}

/** One snapshot of the life clock for a given city. */
data class LifeClockTime(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val formatted: String,           // "12:13"
    val formattedWithSeconds: String, // "12:13:45"
    val period: LifeClockPeriod,
    val dayProgressRatio: Double,     // 0.0 .. 1.0 over 24h life day
    val realLocalTime: DateTime,
    val lifeInstant: DateTime,
    val lifeOffsetMillis: Long        // local offset applied (for debugging / TextClock)
)

enum class LifeClockPeriod(val key: String) {
    SUNRISE("sunrise"),
    MORNING("morning"),
    NOON("noon"),
    AFTERNOON("afternoon"),
    SUNSET("sunset"),
    EVENING("evening"),
    NIGHT("night");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: MORNING
    }
}
