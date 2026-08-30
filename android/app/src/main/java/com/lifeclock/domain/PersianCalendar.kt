package com.lifeclock.domain

import java.util.Calendar
import java.util.TimeZone

/**
 * Persian (Jalali) calendar conversion utility — pure math, no library.
 *
 * Algorithm: the well-known 33-year cycle astronomical-empirical conversion
 * (Birashk algorithm). Accurate for years 1342–1498 (≈ 1963–2120 Gregorian).
 */
object PersianCalendar {

    private val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val PERSIAN_WEEKDAYS = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه",
        "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    data class PersianDate(
        val year: Int,     // e.g. 1403
        val month: Int,    // 1..12
        val day: Int,      // 1..31
        val weekday: Int,  // 0..6, where 0 = Saturday (Persian week start)
        val monthName: String,
        val weekdayName: String
    )

    /**
     * Convert a UTC-millis instant to a Persian (Jalali) date.
     * @param timeZoneId  time zone used to interpret the wall-clock day.
     */
    fun toPersian(utcMillis: Long, timeZoneId: String): PersianDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
        cal.timeInMillis = utcMillis
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1  // Gregorian month 1..12
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday ... 7=Saturday

        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)

        // Map Java weekday (1=Sun..7=Sat) to Persian week (0=Sat..6=Fri)
        val persianWeekday = when (weekday) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        return PersianDate(
            year = jy,
            month = jm,
            day = jd,
            weekday = persianWeekday,
            monthName = PERSIAN_MONTH_NAMES[jm - 1],
            weekdayName = PERSIAN_WEEKDAYS[persianWeekday]
        )
    }

    /** Format Persian digits, e.g. "1403/05/14" -> "۱۴۰۳/۰۵/۱۴" */
    fun toPersianDigits(value: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in value) {
            if (ch in '0'..'9') sb.append(persianDigits[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    /** Format as YYYY/MM/DD with Persian digits. */
    fun PersianDate.format(): String =
        toPersianDigits(String.format("%04d/%02d/%02d", year, month, day))

    /** Format weekday + day + month name, e.g. "شنبه ۱۴ مرداد" */
    fun PersianDate.formatLong(): String =
        "${weekdayName} ${toPersianDigits(day.toString())} $monthName"

    // ----- Birashk conversion (Gregorian → Jalali) -----

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var jy = if (gy <= 1600) 0 else gy - 621
        val gyCalc = if (gy <= 1600) gy - 621 else gy

        var gy2 = if (gm > 2) gyCalc + 1 else gyCalc
        var days = 365 * gyCalc +
            (gyCalc + 3) / 4 -
            (gyCalc + 99) / 100 +
            (gyCalc + 399) / 400 -
            80 +
            gd +
            gDays.sliceArray(0 until gm - 1).sum()

        jy += 33 * (days / 12053)
        days %= 12053

        jy += 4 * (days / 1461)
        days %= 1461

        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0 until 12) {
            if (days < jDays[i]) {
                jm = i + 1
                jd = days + 1
                break
            }
            days -= jDays[i]
        }

        return Triple(jy, jm, jd)
    }
}
