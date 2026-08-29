/* ============================================================
 * Life Clock website — single-file vanilla JS
 *
 * Definition: life clock 09:00 always equals real sunrise for the city.
 *   lifeClock = officialLocalTimeOfDay + (9h - sunriseLocalSecondsOfDay)
 *
 * All math runs in the browser. After first load, fully offline.
 *
 * Detection strategy (first visit):
 *   1. Try navigator.geolocation with a SHORT timeout (3s). If granted, use it.
 *   2. If denied OR timed out, fall back to IP-based detection (ipapi.co).
 *   3. Always show a confirmation modal — user must confirm before saving.
 * ============================================================ */

(function () {
    'use strict';

    // ============== Constants ==============
    const SUNRISE_LIFE_HOUR = 9;
    const SUNRISE_LIFE_SECONDS = SUNRISE_LIFE_HOUR * 3600;
    const ZENITH_OFFICIAL = 90.833;
    const ZENITH_FAJR = 108.0;
    const ZENITH_ISHA = 107.0;
    const STORAGE_KEY = 'lifeclock.city';
    const THEME_KEY = 'lifeclock.theme';
    const DETECT_REJECTED_KEY = 'lifeclock.detectRejected';

    // Preset cities — searchable by Persian or English name
    const PRESET_CITIES = [
        { name: 'تهران', en: 'Tehran', lat: 35.6892, lon: 51.3890, tz: 'Asia/Tehran' },
        { name: 'شیراز', en: 'Shiraz', lat: 29.5918, lon: 52.5837, tz: 'Asia/Tehran' },
        { name: 'مشهد', en: 'Mashhad', lat: 36.2605, lon: 59.6168, tz: 'Asia/Tehran' },
        { name: 'اصفهان', en: 'Isfahan', lat: 32.6539, lon: 51.6660, tz: 'Asia/Tehran' },
        { name: 'تبریز', en: 'Tabriz', lat: 38.0800, lon: 46.2919, tz: 'Asia/Tehran' },
        { name: 'اهواز', en: 'Ahvaz', lat: 31.3203, lon: 48.6692, tz: 'Asia/Tehran' },
        { name: 'کرج', en: 'Karaj', lat: 35.8400, lon: 50.9391, tz: 'Asia/Tehran' },
        { name: 'قم', en: 'Qom', lat: 34.6416, lon: 50.8746, tz: 'Asia/Tehran' },
        { name: 'رشت', en: 'Rasht', lat: 37.2760, lon: 49.5880, tz: 'Asia/Tehran' },
        { name: 'کرمان', en: 'Kerman', lat: 30.2832, lon: 57.0788, tz: 'Asia/Tehran' },
        { name: 'یزد', en: 'Yazd', lat: 31.8974, lon: 54.3569, tz: 'Asia/Tehran' },
        { name: 'اردبیل', en: 'Ardabil', lat: 38.2498, lon: 48.2957, tz: 'Asia/Tehran' },
        { name: 'بندرعباس', en: 'Bandar Abbas', lat: 27.1832, lon: 56.2666, tz: 'Asia/Tehran' },
        { name: 'زاهدان', en: 'Zahedan', lat: 29.5011, lon: 60.8629, tz: 'Asia/Tehran' },
        { name: 'ارومیه', en: 'Urmia', lat: 37.5470, lon: 45.0730, tz: 'Asia/Tehran' },
        { name: 'گرگان', en: 'Gorgan', lat: 36.8450, lon: 54.4340, tz: 'Asia/Tehran' },
        { name: 'سنندج', en: 'Sanandaj', lat: 35.3140, lon: 46.9930, tz: 'Asia/Tehran' },
        { name: 'خرم‌آباد', en: 'Khorramabad', lat: 33.4870, lon: 48.3530, tz: 'Asia/Tehran' },
        { name: 'بجنورد', en: 'Bojnourd', lat: 37.4760, lon: 57.3270, tz: 'Asia/Tehran' },
        { name: 'ایلام', en: 'Ilam', lat: 33.6370, lon: 46.4230, tz: 'Asia/Tehran' },
        { name: 'بوشهر', en: 'Bushehr', lat: 28.9230, lon: 50.8230, tz: 'Asia/Tehran' },
        { name: 'ساری', en: 'Sari', lat: 36.5630, lon: 53.0600, tz: 'Asia/Tehran' },
        { name: 'قزوین', en: 'Qazvin', lat: 36.2710, lon: 50.0040, tz: 'Asia/Tehran' },
        { name: 'همدان', en: 'Hamadan', lat: 34.7990, lon: 48.5150, tz: 'Asia/Tehran' },
        { name: 'بیرجند', en: 'Birjand', lat: 32.8650, lon: 59.2160, tz: 'Asia/Tehran' },
        { name: 'کرمانشاه', en: 'Kermanshah', lat: 34.3140, lon: 47.0650, tz: 'Asia/Tehran' },
        { name: 'کابل', en: 'Kabul', lat: 34.5553, lon: 69.2075, tz: 'Asia/Kabul' },
        { name: 'هرات', en: 'Herat', lat: 34.3430, lon: 62.1990, tz: 'Asia/Kabul' },
        { name: 'بغداد', en: 'Baghdad', lat: 33.3152, lon: 44.3661, tz: 'Asia/Baghdad' },
        { name: 'استانبول', en: 'Istanbul', lat: 41.0082, lon: 28.9784, tz: 'Europe/Istanbul' },
        { name: 'آنکارا', en: 'Ankara', lat: 39.9334, lon: 32.8597, tz: 'Europe/Istanbul' },
        { name: 'دبی', en: 'Dubai', lat: 25.2048, lon: 55.2708, tz: 'Asia/Dubai' },
        { name: 'ابوظبی', en: 'Abu Dhabi', lat: 24.4539, lon: 54.3773, tz: 'Asia/Dubai' },
        { name: 'دوحه', en: 'Doha', lat: 25.2854, lon: 51.5310, tz: 'Asia/Qatar' },
        { name: 'مکه', en: 'Mecca', lat: 21.3891, lon: 39.8579, tz: 'Asia/Riyadh' },
        { name: 'مدینه', en: 'Medina', lat: 24.5247, lon: 39.5692, tz: 'Asia/Riyadh' },
        { name: 'ریاض', en: 'Riyadh', lat: 24.7136, lon: 46.6753, tz: 'Asia/Riyadh' },
        { name: 'لندن', en: 'London', lat: 51.5074, lon: -0.1278, tz: 'Europe/London' },
        { name: 'پاریس', en: 'Paris', lat: 48.8566, lon: 2.3522, tz: 'Europe/Paris' },
        { name: 'برلین', en: 'Berlin', lat: 52.5200, lon: 13.4050, tz: 'Europe/Berlin' },
        { name: 'فرانکفورت', en: 'Frankfurt', lat: 50.1109, lon: 8.6821, tz: 'Europe/Berlin' },
        { name: 'کارلسروهه', en: 'Karlsruhe', lat: 49.0069, lon: 8.4037, tz: 'Europe/Berlin' },
        { name: 'مونیخ', en: 'Munich', lat: 48.1351, lon: 11.5820, tz: 'Europe/Berlin' },
        { name: 'هامبورگ', en: 'Hamburg', lat: 53.5511, lon: 9.9937, tz: 'Europe/Berlin' },
        { name: 'وین', en: 'Vienna', lat: 48.2082, lon: 16.3738, tz: 'Europe/Vienna' },
        { name: 'آمستردام', en: 'Amsterdam', lat: 52.3676, lon: 4.9041, tz: 'Europe/Amsterdam' },
        { name: 'استکهلم', en: 'Stockholm', lat: 59.3293, lon: 18.0686, tz: 'Europe/Stockholm' },
        { name: 'نیویورک', en: 'New York', lat: 40.7128, lon: -74.0060, tz: 'America/New_York' },
        { name: 'لس‌آنجلس', en: 'Los Angeles', lat: 34.0522, lon: -118.2437, tz: 'America/Los_Angeles' },
        { name: 'شیکاگو', en: 'Chicago', lat: 41.8781, lon: -87.6298, tz: 'America/Chicago' },
        { name: 'تورنتو', en: 'Toronto', lat: 43.6532, lon: -79.3832, tz: 'America/Toronto' },
        { name: 'ونکوور', en: 'Vancouver', lat: 49.2827, lon: -123.1207, tz: 'America/Vancouver' },
        { name: 'توکیو', en: 'Tokyo', lat: 35.6762, lon: 139.6503, tz: 'Asia/Tokyo' },
        { name: 'سیدنی', en: 'Sydney', lat: -33.8688, lon: 151.2093, tz: 'Australia/Sydney' },
        { name: 'ملبورن', en: 'Melbourne', lat: -37.8136, lon: 144.9631, tz: 'Australia/Melbourne' }
    ];

    const PRAYER_NAMES_FA = {
        fajr: 'صبح',
        sunrise: 'طلوع',
        dhuhr: 'ظهر',
        asr: 'عصر',
        maghrib: 'مغرب',
        isha: 'عشاء'
    };

    // Prayer slots shown in the widget grid (in order)
    const WIDGET_PRAYER_ORDER = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

    // ============== State ==============
    let state = {
        city: null,
        solarTimes: null,
        prayerTimes: null,
        pendingDetectedCity: null,
        detectInProgress: false
    };

    // ============== Persian digits ==============
    function toPersianDigits(s) {
        const map = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
        return String(s).replace(/\d/g, d => map[d]);
    }

    function pad2(n) { return String(n).padStart(2, '0'); }

    // ============== Persian (Jalali) calendar ==============
    function toJalali(gy, gm, gd) {
        const gDays = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        const jDays = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29];
        let jy = (gy <= 1600) ? 0 : gy - 621;
        let gyCalc = (gy <= 1600) ? gy - 621 : gy;
        let days = 365 * gyCalc + Math.floor((gyCalc + 3) / 4) - Math.floor((gyCalc + 99) / 100) +
            Math.floor((gyCalc + 399) / 400) - 80 + gd +
            gDays.slice(0, gm - 1).reduce((a, b) => a + b, 0);
        jy += 33 * Math.floor(days / 12053);
        days %= 12053;
        jy += 4 * Math.floor(days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += Math.floor((days - 1) / 365);
            days = (days - 1) % 365;
        }
        let jm, jd;
        for (let i = 0; i < 12; i++) {
            if (days < jDays[i]) { jm = i + 1; jd = days + 1; break; }
            days -= jDays[i];
        }
        return { jy, jm, jd };
    }

    function persianWeekdayName(jsWd) {
        const persianWd = (jsWd + 1) % 7;
        return ['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه'][persianWd];
    }

    function persianMonthName(m) {
        return ['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور',
        'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'][m - 1];
    }

    function formatPersianDate(date, tz) {
        const fmt = new Intl.DateTimeFormat('en-US', {
            timeZone: tz, year: 'numeric', month: 'numeric', day: 'numeric', weekday: 'short'
        });
        const parts = fmt.formatToParts(date);
        const y = parseInt(parts.find(p => p.type === 'year').value);
        const m = parseInt(parts.find(p => p.type === 'month').value);
        const d = parseInt(parts.find(p => p.type === 'day').value);
        const j = toJalali(y, m, d);
        const jsWd = date.getDay();
        return `${persianWeekdayName(jsWd)} ${toPersianDigits(j.jd)} ${persianMonthName(j.jm)} ${toPersianDigits(j.jy)}`;
    }

    // ============== Sunrise/sunset (NOAA) ==============
    function dayOfYear(date) {
        const start = new Date(Date.UTC(date.getUTCFullYear(), 0, 0));
        return Math.floor((date - start) / 86400000);
    }

    function isLeapYear(y) {
        return (y % 4 === 0 && y % 100 !== 0) || (y % 400 === 0);
    }

    function equationOfTime(gamma) {
        return 229.18 * (0.000075 +
            0.001868 * Math.cos(gamma) -
            0.032077 * Math.sin(gamma) -
            0.014615 * Math.cos(2 * gamma) -
            0.040849 * Math.sin(2 * gamma));
    }

    function declination(gamma) {
        return 0.006918 -
            0.399912 * Math.cos(gamma) +
            0.070257 * Math.sin(gamma) -
            0.006758 * Math.cos(2 * gamma) +
            0.000907 * Math.sin(2 * gamma) -
            0.002697 * Math.cos(3 * gamma) +
            0.001480 * Math.sin(3 * gamma);
    }

    function hourAngleForZenith(latDeg, declRad, zenithDeg) {
        const latRad = (Math.PI / 180) * latDeg;
        const zenithRad = (Math.PI / 180) * zenithDeg;
        const cosH = (Math.cos(zenithRad) - Math.sin(latRad) * Math.sin(declRad)) /
            (Math.cos(latRad) * Math.cos(declRad));
        if (cosH > 1) return NaN;
        if (cosH < -1) return NaN;
        return Math.acos(cosH);
    }

    function computeSolarTimes(dateUtc, lat, lon) {
        const year = dateUtc.getUTCFullYear();
        const doy = dayOfYear(dateUtc) + (dateUtc.getUTCHours() + dateUtc.getUTCMinutes() / 60) / 24;
        const denom = isLeapYear(year) ? 366 : 365;
        const gamma = 2 * Math.PI / denom * (doy - 1);
        const eqTime = equationOfTime(gamma);
        const decl = declination(gamma);

        const solarNoonMinutes = 720 - 4 * lon - eqTime;
        const dhuhrMinutes = solarNoonMinutes + 1;

        function minutesForZenith(zenith, sign) {
            const H = hourAngleForZenith(lat, decl, zenith);
            if (isNaN(H)) return NaN;
            return 720 - 4 * (lon + sign * H * 180 / Math.PI) - eqTime;
        }

        const sunriseMin = minutesForZenith(ZENITH_OFFICIAL, +1);
        const sunsetMin = minutesForZenith(ZENITH_OFFICIAL, -1);
        const fajrMin = minutesForZenith(ZENITH_FAJR, +1);
        const ishaMin = minutesForZenith(ZENITH_ISHA, -1);

        const latRad = (Math.PI / 180) * lat;
        const asrAlt = Math.atan(1 + Math.tan(Math.abs(latRad - decl)));
        const cosHAsr = (Math.sin(-asrAlt) - Math.sin(latRad) * Math.sin(decl)) /
            (Math.cos(latRad) * Math.cos(decl));
        let asrMin = NaN;
        if (cosHAsr >= -1 && cosHAsr <= 1) {
            const HAsr = Math.acos(cosHAsr);
            asrMin = solarNoonMinutes + (HAsr * 180 / Math.PI) / 15 * 60;
        }

        const dayStart = Date.UTC(year, dateUtc.getUTCMonth(), dateUtc.getUTCDate(), 0, 0, 0);
        const toUtcDate = (m) => isNaN(m) ? null : new Date(dayStart + m * 60000);

        return {
            sunrise: toUtcDate(sunriseMin),
            sunset: toUtcDate(sunsetMin),
            solarNoon: toUtcDate(solarNoonMinutes),
            fajr: toUtcDate(fajrMin),
            dhuhr: toUtcDate(dhuhrMinutes),
            asr: toUtcDate(asrMin),
            maghrib: toUtcDate(sunsetMin),
            isha: toUtcDate(ishaMin),
            dayLengthMinutes: (!isNaN(sunriseMin) && !isNaN(sunsetMin)) ? Math.round(sunsetMin - sunriseMin) : 0
        };
    }

    function lastSunrise(now, lat, lon) {
        const todaySolar = computeSolarTimes(now, lat, lon);
        if (todaySolar.sunrise && now >= todaySolar.sunrise) return todaySolar.sunrise;
        const yesterday = new Date(now.getTime() - 24 * 3600 * 1000);
        const ySolar = computeSolarTimes(yesterday, lat, lon);
        return ySolar.sunrise || todaySolar.sunrise;
    }

    // ============== Life clock math ==============
    function lifeOffsetMillis(now, tz, sunriseUtc) {
        if (!sunriseUtc) return 0;
        const localSec = localSecondsOfDay(sunriseUtc, tz);
        return (SUNRISE_LIFE_SECONDS - localSec) * 1000;
    }

    function localSecondsOfDay(date, tz) {
        const fmt = new Intl.DateTimeFormat('en-US', {
            timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
        });
        const parts = fmt.formatToParts(date);
        const h = parseInt(parts.find(p => p.type === 'hour').value) % 24;
        const m = parseInt(parts.find(p => p.type === 'minute').value);
        const s = parseInt(parts.find(p => p.type === 'second').value);
        return h * 3600 + m * 60 + s;
    }

    function formatHM(date, tz) {
        if (!date) return '—';
        const fmt = new Intl.DateTimeFormat('en-US', {
            timeZone: tz, hour: '2-digit', minute: '2-digit', hour12: false
        });
        return fmt.format(date);
    }

    function formatHMS(date, tz) {
        if (!date) return '—';
        const fmt = new Intl.DateTimeFormat('en-US', {
            timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
        });
        return fmt.format(date);
    }

    /**
     * Format a Date as life-clock HH:MM:SS.
     * Life clock = (official_local_seconds + offset_seconds) mod 24h
     */
    function formatLifeHMS(officialDate, tz, sunriseUtc) {
        if (!officialDate) return '—';
        const localSec = localSecondsOfDay(officialDate, tz);
        const offsetSec = lifeOffsetMillis(officialDate, tz, sunriseUtc) / 1000;
        const lifeSec = ((localSec + offsetSec) % 86400 + 86400) % 86400;
        const lh = Math.floor(lifeSec / 3600);
        const lm = Math.floor((lifeSec % 3600) / 60);
        const ls = Math.floor(lifeSec % 60);
        return `${pad2(lh)}:${pad2(lm)}:${pad2(ls)}`;
    }

    function formatLifeHM(officialDate, tz, sunriseUtc) {
        const hms = formatLifeHMS(officialDate, tz, sunriseUtc);
        return hms === '—' ? '—' : hms.substring(0, 5);
    }

    // ============== Next prayer ==============
    function nextPrayer(now, prayers) {
        const list = [
            { name: 'fajr', t: prayers.fajr },
            { name: 'sunrise', t: prayers.sunrise },
            { name: 'dhuhr', t: prayers.dhuhr },
            { name: 'asr', t: prayers.asr },
            { name: 'maghrib', t: prayers.maghrib },
            { name: 'isha', t: prayers.isha }
        ].filter(p => p.t).sort((a, b) => a.t - b.t);

        const next = list.find(p => p.t > now);
        if (next) {
            const passed = [...list].reverse().find(p => p.t <= now);
            return { current: passed?.name, next: next.name, nextTime: next.t };
        }
        const tomorrowFajr = new Date(now.getTime() + 24 * 3600 * 1000);
        const tomorrowSolar = computeSolarTimes(tomorrowFajr, state.city.lat, state.city.lon);
        return {
            current: list[list.length - 1]?.name,
            next: 'fajr',
            nextTime: tomorrowSolar.fajr
        };
    }

    // ============== Auto city detection ==============
    /**
     * Detection strategy:
     *   1. Try GPS with 3s timeout. If granted and accurate → reverse-geocode.
     *   2. If GPS denied or timed out → fall back to IP-based detection.
     *   3. Never auto-save — caller must show confirmation modal.
     */
    async function autoDetectCity() {
        // Step 1: try GPS first (one-shot, low-power, short timeout)
        const gpsResult = await tryGpsDetection();
        if (gpsResult) return gpsResult

        // Step 2: fall back to IP
        return await ipBasedDetect()
    }

    async function tryGpsDetection() {
        if (!navigator.geolocation) return null
        try {
            const pos = await new Promise((resolve, reject) => {
                navigator.geolocation.getCurrentPosition(resolve, reject, {
                    timeout: 3000,            // short — don't make the user wait
                    maximumAge: 5 * 60 * 1000, // accept a 5-min cached position
                    enableHighAccuracy: false  // low-power, city-level accuracy is enough
                })
            })
            const { latitude, longitude } = pos.coords
            // Reverse-geocode via BigDataCloud (no API key needed)
            const city = await reverseGeocode(latitude, longitude)
            if (city) {
                city.source = 'gps'
                return city
            }
        } catch (e) {
            // Permission denied, or timed out, or position unavailable
            return null
        }
        return null
    }

    async function reverseGeocode(lat, lon) {
        try {
            const r = await fetch(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=fa`)
            const data = await r.json()
            const cityName = data.city || data.locality || data.principalSubdivision || data.countryName || 'موقعیت من'
            // Try to find the closest preset city by distance — gives us a proper timezone
            const closest = findClosestPresetCity(lat, lon)
            if (closest) {
                // Use the closest preset city's name in Persian if it's within ~50km
                const dist = haversineKm(lat, lon, closest.lat, closest.lon)
                if (dist < 50) {
                    return { name: closest.name, lat: closest.lat, lon: closest.lon, tz: closest.tz, source: 'gps' }
                }
            }
            // Otherwise use the reverse-geocoded name + device tz
            const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Tehran'
            return { name: cityName, lat, lon, tz, source: 'gps' }
        } catch (e) {
            return null
        }
    }

    /** Returns the preset city closest to the given lat/lon, by haversine distance. */
    function findClosestPresetCity(lat, lon) {
        let best = null
        let bestDist = Infinity
        for (const c of PRESET_CITIES) {
            const d = haversineKm(lat, lon, c.lat, c.lon)
            if (d < bestDist) { bestDist = d; best = c }
        }
        return best
    }

    function haversineKm(lat1, lon1, lat2, lon2) {
        const R = 6371 // km
        const dLat = (lat2 - lat1) * Math.PI / 180
        const dLon = (lon2 - lon1) * Math.PI / 180
        const a = Math.sin(dLat / 2) ** 2 +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) ** 2
        return 2 * R * Math.asin(Math.sqrt(a))
    }

    async function ipBasedDetect() {
        // Try ipapi.co first — returns city name + lat/lon + timezone
        try {
            const r = await fetch('https://ipapi.co/json/')
            const data = await r.json()
            if (data && data.latitude && data.longitude) {
                // Snap to the closest preset city if within ~100km — this gives us
                // a proper Persian name and known timezone
                const closest = findClosestPresetCity(data.latitude, data.longitude)
                if (closest) {
                    const dist = haversineKm(data.latitude, data.longitude, closest.lat, closest.lon)
                    if (dist < 100) {
                        return { name: closest.name, lat: closest.lat, lon: closest.lon, tz: closest.tz, source: 'ip' }
                    }
                }
                // Otherwise use the IP-detected city as-is
                return {
                    name: data.city || data.region || 'موقعیت من',
                    lat: data.latitude,
                    lon: data.longitude,
                    tz: data.timezone || 'Asia/Tehran',
                    source: 'ip'
                }
            }
        } catch (e) {}
        // Final fallback — Tehran
        return { ...PRESET_CITIES[0], source: 'fallback' }
    }

    // ============== Persistence ==============
    function saveCity(city) {
        try { localStorage.setItem(STORAGE_KEY, JSON.stringify(city)); } catch (e) {}
    }

    function loadCity() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return null;
            return JSON.parse(raw);
        } catch (e) { return null; }
    }

    function saveTheme(theme) {
        try { localStorage.setItem(THEME_KEY, theme); } catch (e) {}
    }

    function loadTheme() {
        try { return localStorage.getItem(THEME_KEY) || 'dark'; }
        catch (e) { return 'dark'; }
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        saveTheme(theme);
        document.querySelectorAll('.theme-chip').forEach(chip => {
            chip.classList.toggle('active', chip.dataset.theme === theme);
        });
    }

    // ============== UI rendering ==============
    function renderAll() {
        if (!state.city) return;
        const now = new Date();
        const sunrise = lastSunrise(now, state.city.lat, state.city.lon);
        state.solarTimes = computeSolarTimes(now, state.city.lat, state.city.lon);
        state.prayerTimes = state.solarTimes;

        document.getElementById('lifeClockBig').textContent =
            toPersianDigits(formatLifeHMS(now, state.city.tz, sunrise));
        document.getElementById('officialTime').textContent =
            toPersianDigits(formatHM(now, state.city.tz));
        document.getElementById('persianDate').textContent =
            formatPersianDate(now, state.city.tz);
        document.getElementById('currentCity').textContent = state.city.name;

        renderPrayerGrid();
        renderNextPrayer(now);

        document.getElementById('officialInput').value = formatHMS(now, state.city.tz);
        document.getElementById('lifeInput').value = formatLifeHMS(now, state.city.tz, sunrise);
        document.getElementById('currentOffset').textContent =
            toPersianDigits(formatOffsetMinutes(lifeOffsetMillis(now, state.city.tz, sunrise) / 60000));
        document.getElementById('sunriseToday').textContent =
            toPersianDigits(formatHM(state.solarTimes.sunrise, state.city.tz));
        document.getElementById('sunsetToday').textContent =
            toPersianDigits(formatHM(state.solarTimes.sunset, state.city.tz));

        renderDiff(now, sunrise);
        renderPrayerTable(now);
    }

    function formatOffsetMinutes(minutes) {
        const sign = minutes >= 0 ? '+' : '-';
        const abs = Math.abs(minutes);
        const h = Math.floor(abs / 60);
        const m = Math.round(abs % 60);
        return `${sign}${pad2(h)}:${pad2(m)}`;
    }

    /**
     * Render the prayer grid in the widget.
     *
     * Layout: two rows
     *   Row 1: names (صبح، طلوع، ظهر، عصر، مغرب، عشاء)
     *   Row 2: times in LIFE CLOCK format (not official time)
     *
     * Only the NEXT upcoming prayer is colored differently; everything else
     * uses the default muted color.
     */
    function renderPrayerGrid() {
        const grid = document.getElementById('prayerGrid');
        const now = new Date();
        const next = nextPrayer(now, state.prayerTimes);
        const sunrise = state.solarTimes.sunrise;

        let namesHtml = '';
        let timesHtml = '';
        WIDGET_PRAYER_ORDER.forEach(key => {
            const isNext = (next.next === key);
            namesHtml += `<div class="pg-name ${isNext ? 'next' : ''}">${PRAYER_NAMES_FA[key]}</div>`;
            const time = state.prayerTimes[key];
            const lifeTime = time ? formatLifeHM(time, state.city.tz, sunrise) : '—';
            timesHtml += `<div class="pg-time ${isNext ? 'next' : ''}">${time ? toPersianDigits(lifeTime) : '—'}</div>`;
        });

        grid.innerHTML = `
            <div class="pg-row">${namesHtml}</div>
            <div class="pg-row">${timesHtml}</div>
        `;
    }

    function renderNextPrayer(now) {
        const next = nextPrayer(now, state.prayerTimes);
        document.getElementById('nextPrayerName').textContent = PRAYER_NAMES_FA[next.next] || '—';
        const remaining = next.nextTime - now;
        if (remaining > 0) {
            const h = Math.floor(remaining / 3600000);
            const m = Math.floor((remaining % 3600000) / 60000);
            const s = Math.floor((remaining % 60000) / 1000);
            document.getElementById('nextPrayerCountdown').textContent =
                toPersianDigits(`${pad2(h)}:${pad2(m)}:${pad2(s)}`);
        } else {
            document.getElementById('nextPrayerCountdown').textContent = '۰۰:۰۰';
        }
        // "منقضی شد" text removed per user request — status label is now empty
        const statusEl = document.getElementById('currentPeriod');
        if (statusEl) statusEl.textContent = '';
    }

    function renderDiff(now, sunrise) {
        document.getElementById('diffLifeTime').textContent =
            toPersianDigits(formatLifeHM(now, state.city.tz, sunrise));
        document.getElementById('diffOfficialTime').textContent =
            toPersianDigits(formatHM(now, state.city.tz));

        const offsetMin = lifeOffsetMillis(now, state.city.tz, sunrise) / 60000;
        const sign = offsetMin >= 0 ? '+' : '-';
        const abs = Math.abs(offsetMin);
        const h = Math.floor(abs / 60);
        const m = Math.round(abs % 60);
        document.getElementById('diffNumber').textContent =
            toPersianDigits(`${sign}${pad2(h)}:${pad2(m)}`);

        const sunriseStr = formatHM(sunrise, state.city.tz);
        const dir = offsetMin >= 0 ? 'جلوتر' : 'عقب‌تر';
        const explanation = `طلوع امروز در ${state.city.name} ساعت ${toPersianDigits(sunriseStr)} به وقت رسمی بوده.
ساعت زندگی، ${toPersianDigits(String(Math.abs(Math.round(offsetMin))))} دقیقه ${dir} از ساعت رسمی است.
این اختلاف هر روز با تغییر زمان طلوع، کمی متفاوت می‌شه — در تابستان کمتر و در زمستان بیشتر.`;
        document.getElementById('diffExplanation').innerHTML = `<p>${explanation}</p>`;
    }

    function renderPrayerTable(now) {
        const rows = document.querySelectorAll('.prayer-row[data-prayer]');
        const next = nextPrayer(now, state.prayerTimes);
        rows.forEach(row => {
            const key = row.dataset.prayer;
            const time = state.prayerTimes[key];
            const officialCell = row.querySelector('.prayer-official');
            const lifeCell = row.querySelector('.prayer-life');
            const statusCell = row.querySelector('.prayer-status');

            if (!time) {
                officialCell.textContent = '—';
                lifeCell.textContent = '—';
                statusCell.textContent = '—';
                return;
            }

            officialCell.textContent = toPersianDigits(formatHM(time, state.city.tz));
            lifeCell.textContent = toPersianDigits(formatLifeHM(time, state.city.tz, state.solarTimes.sunrise));

            row.classList.remove('current');
            if (time <= now) {
                statusCell.textContent = 'گذشته';
                statusCell.className = 'prayer-status passed';
            } else {
                statusCell.textContent = 'بعدی';
                statusCell.className = 'prayer-status upcoming';
                if (next.next === key) {
                    row.classList.add('current');
                    statusCell.textContent = 'اذان بعدی';
                    statusCell.className = 'prayer-status current';
                }
            }
        });
    }

    // ============== Converter (two-way instant) ==============
    let converterSource = null;

    function setupConverter() {
        const officialInput = document.getElementById('officialInput');
        const lifeInput = document.getElementById('lifeInput');

        officialInput.addEventListener('input', () => {
            if (converterSource === 'life') return;
            converterSource = 'official';
            const val = officialInput.value;
            if (!val) { converterSource = null; return; }
            const life = convertInputToLife(val);
            if (life) lifeInput.value = life;
            converterSource = null;
        });

        lifeInput.addEventListener('input', () => {
            if (converterSource === 'official') return;
            converterSource = 'life';
            const val = lifeInput.value;
            if (!val) { converterSource = null; return; }
            const official = convertInputToOfficial(val);
            if (official) officialInput.value = official;
            converterSource = null;
        });
    }

    function convertInputToLife(officialHMSStr) {
        if (!state.city) return null;
        const sunrise = lastSunrise(new Date(), state.city.lat, state.city.lon);
        const parts = officialHMSStr.split(':').map(p => parseInt(p) || 0);
        const [h, m, s] = [parts[0] || 0, parts[1] || 0, parts[2] || 0];
        const officialSec = h * 3600 + m * 60 + s;
        const offsetSec = lifeOffsetMillis(new Date(), state.city.tz, sunrise) / 1000;
        const lifeSec = ((officialSec + offsetSec) % 86400 + 86400) % 86400;
        const lh = Math.floor(lifeSec / 3600);
        const lm = Math.floor((lifeSec % 3600) / 60);
        const ls = Math.floor(lifeSec % 60);
        return `${pad2(lh)}:${pad2(lm)}:${pad2(ls)}`;
    }

    function convertInputToOfficial(lifeHMSStr) {
        if (!state.city) return null;
        const sunrise = lastSunrise(new Date(), state.city.lat, state.city.lon);
        const parts = lifeHMSStr.split(':').map(p => parseInt(p) || 0);
        const [h, m, s] = [parts[0] || 0, parts[1] || 0, parts[2] || 0];
        const lifeSec = h * 3600 + m * 60 + s;
        const offsetSec = lifeOffsetMillis(new Date(), state.city.tz, sunrise) / 1000;
        const officialSec = ((lifeSec - offsetSec) % 86400 + 86400) % 86400;
        const oh = Math.floor(officialSec / 3600);
        const om = Math.floor((officialSec % 3600) / 60);
        const os = Math.floor(officialSec % 60);
        return `${pad2(oh)}:${pad2(om)}:${pad2(os)}`;
    }

    // ============== City picker panel (easy UX) ==============
    function setupSettings() {
        const btn = document.getElementById('settingsBtn');
        const panel = document.getElementById('settingsPanel');
        const search = document.getElementById('citySearch');
        const useAuto = document.getElementById('useAutoLocation');
        const close = document.getElementById('closeSettings');
        const cityLabelBtn = document.getElementById('currentCityBtn');
        const cityListEl = document.getElementById('cityList');

        // Initial fill of city list
        renderCityList('');

        btn.addEventListener('click', () => togglePanel());
        cityLabelBtn.addEventListener('click', () => togglePanel());
        close.addEventListener('click', () => panel.classList.remove('open'));

        // Live search: filter as user types
        search.addEventListener('input', () => {
            renderCityList(search.value.trim());
        });

        // Enter key — pick the first matching city
        search.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const first = PRESET_CITIES.find(c => cityMatches(c, search.value.trim()));
                if (first) {
                    selectCity({ ...first });
                    search.value = '';
                    renderCityList('');
                }
            }
        });

        useAuto.addEventListener('click', async () => {
            panel.classList.remove('open');
            await detectAndConfirm();
        });

        function togglePanel() {
            panel.classList.toggle('open');
            if (panel.classList.contains('open')) {
                setTimeout(() => search.focus(), 50);
            }
        }
    }

    function cityMatches(city, query) {
        if (!query) return true;
        const q = query.toLowerCase();
        return city.name.toLowerCase().includes(q) ||
               city.en.toLowerCase().includes(q);
    }

    function renderCityList(query) {
        const list = document.getElementById('cityList');
        const filtered = PRESET_CITIES.filter(c => cityMatches(c, query));
        if (filtered.length === 0) {
            list.innerHTML = '<div class="city-empty">شهری پیدا نشد. می‌تونید روی «تشخیص خودکار» بزنید.</div>';
            return;
        }
        list.innerHTML = filtered.map(c => `
            <button class="city-item" data-name="${c.name}">
                <span class="city-fa">${c.name}</span>
                <span class="city-en">${c.en}</span>
            </button>
        `).join('');
        // Attach click handlers
        list.querySelectorAll('.city-item').forEach(item => {
            item.addEventListener('click', () => {
                const match = PRESET_CITIES.find(c => c.name === item.dataset.name);
                if (match) selectCity({ ...match });
            });
        });
    }

    function selectCity(city) {
        state.city = city;
        saveCity(state.city);
        document.getElementById('settingsPanel').classList.remove('open');
        document.getElementById('citySearch').value = '';
        renderCityList('');
        renderAll();
    }

    // ============== Auto detection + confirmation modal ==============
    async function detectAndConfirm() {
        if (state.detectInProgress) return;
        state.detectInProgress = true;
        document.getElementById('currentCity').textContent = 'در حال تشخیص...';

        const city = await autoDetectCity();
        state.detectInProgress = false;

        if (city) {
            state.pendingDetectedCity = city;
            document.getElementById('detectedCity').textContent = city.name;
            const modal = document.getElementById('confirmModal');
            modal.classList.add('open');

            // Add a small note about the detection source
            const sourceLabel = city.source === 'gps' ? '(تشخیص دقیق از GPS)' :
                                city.source === 'ip' ? '(تشخیص حدودی از IP)' :
                                '(پیش‌فرض)';
            const detectedEl = document.getElementById('detectedCity');
            detectedEl.textContent = `${city.name} ${sourceLabel}`;
        } else {
            document.getElementById('currentCity').textContent = 'تشخیص ناموفق بود';
            // Open settings so the user can pick manually
            document.getElementById('settingsPanel').classList.add('open');
            setTimeout(() => document.getElementById('citySearch').focus(), 50);
        }
    }

    function setupConfirmModal() {
        const modal = document.getElementById('confirmModal');
        const confirmBtn = document.getElementById('confirmCity');
        const rejectBtn = document.getElementById('rejectCity');

        confirmBtn.addEventListener('click', () => {
            if (state.pendingDetectedCity) {
                selectCity(state.pendingDetectedCity);
            }
            modal.classList.remove('open');
            state.pendingDetectedCity = null;
        });

        rejectBtn.addEventListener('click', () => {
            modal.classList.remove('open');
            // Open the city picker panel
            document.getElementById('settingsPanel').classList.add('open');
            setTimeout(() => document.getElementById('citySearch').focus(), 50);
            state.pendingDetectedCity = null;
        });
    }

    // ============== Theme switcher ==============
    function setupThemes() {
        const chips = document.querySelectorAll('.theme-chip');
        chips.forEach(chip => {
            chip.addEventListener('click', () => applyTheme(chip.dataset.theme));
        });
    }

    // ============== PWA install prompt ==============
    let deferredInstallPrompt = null;
    function setupInstallPrompt() {
        const installBtn = document.getElementById('installAppBtn');
        if (!installBtn) return;
        installBtn.style.display = 'none';

        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            deferredInstallPrompt = e;
            installBtn.style.display = 'inline-block';
        });

        installBtn.addEventListener('click', async () => {
            if (!deferredInstallPrompt) return;
            deferredInstallPrompt.prompt();
            const choice = await deferredInstallPrompt.userChoice;
            if (choice.outcome === 'accepted') {
                installBtn.style.display = 'none';
            }
            deferredInstallPrompt = null;
        });

        window.addEventListener('appinstalled', () => {
            installBtn.style.display = 'none';
        });
    }

    // ============== Init ==============
    async function init() {
        // Apply saved theme first
        applyTheme(loadTheme());

        setupConverter();
        setupSettings();
        setupConfirmModal();
        setupThemes();
        setupInstallPrompt();

        // Register service worker
        if ('serviceWorker' in navigator) {
            try {
                await navigator.serviceWorker.register('sw.js');
            } catch (e) {}
        }

        // Load saved city, or auto-detect on first visit
        const saved = loadCity();
        if (saved) {
            state.city = saved;
            renderAll();
        } else {
            await detectAndConfirm();
            // If user closed the modal without confirming, render with the detected city
            // anyway (better than showing "--:--" forever).
            if (!state.city && state.pendingDetectedCity) {
                state.city = state.pendingDetectedCity;
                saveCity(state.city);
                renderAll();
            }
            // Last resort fallback
            if (!state.city) {
                state.city = PRESET_CITIES[0];
                saveCity(state.city);
                renderAll();
            }
        }

        // Tick every second
        setInterval(renderAll, 1000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
