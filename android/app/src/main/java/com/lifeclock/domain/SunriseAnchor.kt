package com.lifeclock.domain

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days

/**
 * Picks the most recent sunrise relative to "now" for a given location.
 *
 * Why we need this:
 *   The life clock offset is anchored to the most recent sunrise. Before
 *   today's sunrise happens, we should still use yesterday's sunrise so
 *   the life clock continues to advance normally through the night.
 *
 * Strategy:
 *   1. Compute today's sunrise (UTC millis).
 *   2. If now >= today's sunrise → use today's sunrise.
 *   3. Else → compute yesterday's sunrise and use that.
 *
 * Both calls are pure-math (NOAA formula), so the cost is negligible.
 *
 * For polar regions where sunrise/sunset doesn't happen on a given day,
 * the [SunriseCalculator] returns nulls; we propagate that null so
 * [LifeClockCalculator] can fall back to a +0 offset.
 */
object SunriseAnchor {

    /**
     * Returns the UTC millis of the most recent sunrise (today or yesterday).
     * Returns null if sunrise is undefined for both days (polar regions).
     */
    fun lastSunrise(
        nowUtcMillis: Long,
        latitude: Double,
        longitude: Double
    ): Long? {
        val todaySunrise = SunriseCalculator.compute(nowUtcMillis, latitude, longitude).sunriseUtcMillis
        if (todaySunrise != null && nowUtcMillis >= todaySunrise) {
            return todaySunrise
        }
        // Before today's sunrise, or today has no sunrise — try yesterday
        val yesterdayUtc = nowUtcMillis - 24L * 60 * 60 * 1000
        val yesterdaySunrise = SunriseCalculator.compute(yesterdayUtc, latitude, longitude).sunriseUtcMillis
        return yesterdaySunrise ?: todaySunrise  // fall back to today even if "future"
    }

    /**
     * Returns the UTC millis of the next upcoming sunset after now.
     * Used to display the "today's sunset" info — if sunset already passed,
     * we still show today's sunset (rather than tomorrow's) so the user can see
     * "sunset was at X" until midnight.
     */
    fun todayOrLastSunset(
        nowUtcMillis: Long,
        latitude: Double,
        longitude: Double
    ): Long? {
        return SunriseCalculator.compute(nowUtcMillis, latitude, longitude).sunsetUtcMillis
    }
}
