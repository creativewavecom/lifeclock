/* ============================================================
 * Life Clock website — single-file vanilla JS
 *
 * Definition (per spec):
 *   Life Clock 09:00 always equals real sunrise for the user's city.
 *   So:  lifeClock = officialLocalTimeOfDay + (9h - sunriseLocalSecondsOfDay)
 *
 * All math runs in the browser, fully offline after first page load.
 * City auto-detection uses free public APIs (geolocation or IP-based).
 * ============================================================ */

(function () {
    'use strict';

    // ============== Constants ==============
    const SUNRISE_LIFE_HOUR = 9; // life-clock hour at real sunrise
    const SUNRISE_LIFE_SECONDS = SUNRISE_LIFE_HOUR * 3600;
    const ZENITH_OFFICIAL = 90.833; // sunrise/sunset zenith (with refraction)
    const ZENITH_FAJR = 108.0;     // Fajr: sun 18° below horizon (Shia)
    const ZENITH_ISHA = 107.0;     // Isha: sun 17° below horizon (Shia)
    const STORAGE_KEY = 'lifeclock.city';
    const THEME_KEY = 'lifeclock.theme';

    // Preset cities (lat, lon, timezone)
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
        { name: 'کابل', en: 'Kabul', lat: 34.5553, lon: 69.2075, tz: 'Asia/Kabul' },
        { name: 'بغداد', en: 'Baghdad', lat: 33.3152, lon: 44.3661, tz: 'Asia/Baghdad' },
        { name: 'استانبول', en: 'Istanbul', lat: 41.0082, lon: 28.9784, tz: 'Europe/Istanbul' },
        { name: 'دبی', en: 'Dubai', lat: 25.2048, lon: 55.2708, tz: 'Asia/Dubai' },
        { name: 'مکه', en: 'Mecca', lat: 21.3891, lon: 39.8579, tz: 'Asia/Riyadh' },
        { name: 'مدینه', en: 'Medina', lat: 24.5247, lon: 39.5692, tz: 'Asia/Riyadh' },
        { name: 'لندن', en: 'London', lat: 51.5074, lon: -0.1278, tz: 'Europe/London' },
        { name: 'پاریس', en: 'Paris', lat: 48.8566, lon: 2.3522, tz: 'Europe/Paris' },
        { name: 'برلین', en: 'Berlin', lat: 52.5200, lon: 13.4050, tz: 'Europe/Berlin' },
        { name: 'نیویورک', en: 'New York', lat: 40.7128, lon: -74.0060, tz: 'America/New_York' },
        { name: 'لس‌آنجلس', en: 'Los Angeles', lat: 34.0522, lon: -118.2437, tz: 'America/Los_Angeles' },
        { name: 'تورنتو', en: 'Toronto', lat: 43.6532, lon: -79.3832, tz: 'America/Toronto' },
        { name: 'توکیو', en: 'Tokyo', lat: 35.6762, lon: 139.6503, tz: 'Asia/Tokyo' },
        { name: 'سیدنی', en: 'Sydney', lat: -33.8688, lon: 151.2093, tz: 'Australia/Sydney' }
    ];

    const PRAYER_NAMES_FA = {
        fajr: 'اذان صبح',
        sunrise: 'طلوع',
        dhuhr: 'اذان ظهر',
        asr: 'اذان عصر',
        maghrib: 'اذان مغرب',
        isha: 'اذان عشاء'
    };

    // ============== State ==============
    let state = {
        city: null,
        solarTimes: null,
        prayerTimes: null,
        pendingDetectedCity: null  // for confirmation modal
    };

    // ============== Utility: Persian digits ==============
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
        // jsWd: 0=Sun..6=Sat → Persian: 0=Sat..6=Fri
        const persianWd = (jsWd + 1) % 7;
        return ['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه'][persianWd];
    }

    function persianMonthName(m) {
        return ['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور',
        'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'][m - 1];
    }

    function formatPersianDate(date, tz) {
        const fmt = new Intl.DateTimeFormat('en-US', {
            timeZone: tz, year: 'numeric', month: 'numeric', day: 'numeric',
            weekday: 'short'
        });
        const parts = fmt.formatToParts(date);
        const y = parseInt(parts.find(p => p.type === 'year').value);
        const m = parseInt(parts.find(p => p.type === 'month').value);
        const d = parseInt(parts.find(p => p.type === 'day').value);
        const j = toJalali(y, m, d);
        const jsWd = date.getDay();
        return `${persianWeekdayName(jsWd)} ${toPersianDigits(j.jd)} ${persianMonthName(j.jm)} ${toPersianDigits(j.jy)}`;
    }

    // ============== Sunrise/sunset calculator (NOAA) ==============
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
    /**
     * Returns the life-clock offset in MILLISECONDS for a city at a given instant.
     * lifeOffset = (9h - sunriseLocalSecondsOfDay) * 1000
     */
    function lifeOffsetMillis(now, tz, sunriseUtc) {
        if (!sunriseUtc) return 0;
        const fmt = new Intl.DateTimeFormat('en-US', {
            timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit',
            hour12: false
        });
        const parts = fmt.formatToParts(sunriseUtc);
        const h = parseInt(parts.find(p => p.type === 'hour').value) % 24;
        const m = parseInt(parts.find(p => p.type === 'minute').value);
        const s = parseInt(parts.find(p => p.type === 'second').value);
        const sunriseLocalSeconds = h * 3600 + m * 60 + s;
        return (SUNRISE_LIFE_SECONDS - sunriseLocalSeconds) * 1000;
    }

    /**
     * Returns the local time-of-day (seconds since midnight) for a Date in a given tz.
     */
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

    /**
     * Format a Date as official-time HH:MM in the given timezone.
     */
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

    // ============== Next prayer calculator ==============
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
        // All today's prayers passed — next is tomorrow's Fajr
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
     * Try geolocation (one-shot, low-power). On failure or denial, fall back to IP.
     * Returns the detected city or null.
     */
    async function autoDetectCity() {
        if (navigator.geolocation) {
            try {
                const pos = await new Promise((resolve, reject) => {
                    navigator.geolocation.getCurrentPosition(resolve, reject, {
                        timeout: 8000,
                        maximumAge: 0,           // force a fresh (but approximate) read
                        enableHighAccuracy: false // use low-power mode
                    });
                });
                const { latitude, longitude } = pos.coords;
                const city = await reverseGeocode(latitude, longitude);
                if (city) return city;
            } catch (e) {
                // User denied, or timed out — fall through to IP-based detection.
            }
        }
        // IP-based fallback (always succeeds unless network is down)
        return await ipBasedDetect();
    }

    async function reverseGeocode(lat, lon) {
        try {
            const r = await fetch(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=fa`);
            const data = await r.json();
            const cityName = data.city || data.locality || data.principalSubdivision || data.countryName || 'موقعیت من';
            const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Tehran';
            return { name: cityName, lat, lon, tz };
        } catch (e) {
            return null;
        }
    }

    async function ipBasedDetect() {
        try {
            const r = await fetch('https://ipapi.co/json/');
            const data = await r.json();
            if (data && data.latitude && data.longitude) {
                return {
                    name: data.city || data.region || 'موقعیت من',
                    lat: data.latitude,
                    lon: data.longitude,
                    tz: data.timezone || 'Asia/Tehran'
                };
            }
        } catch (e) {}
        return PRESET_CITIES[0]; // Tehran fallback
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
        // Highlight the active theme chip
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

    function renderPrayerGrid() {
        const grid = document.getElementById('prayerGrid');
        const prayers = [
            { key: 'fajr', label: 'صبح' },
            { key: 'sunrise', label: 'طلوع' },
            { key: 'dhuhr', label: 'ظهر' },
            { key: 'asr', label: 'عصر' },
            { key: 'maghrib', label: 'مغرب' },
            { key: 'isha', label: 'عشاء' }
        ];
        const now = new Date();
        const next = nextPrayer(now, state.prayerTimes);
        let html = '';
        prayers.forEach(p => {
            const isNext = (next.next === p.key);
            html += `<div class="grid-item name ${isNext ? 'active' : ''}">${p.label}</div>`;
        });
        prayers.forEach(p => {
            const time = state.prayerTimes[p.key];
            const isActive = time && time <= now;
            const isNext = (next.next === p.key);
            html += `<div class="grid-item time ${isActive ? 'active' : ''} ${isNext ? 'next' : ''}">${time ? toPersianDigits(formatHM(time, state.city.tz)) : '—'}</div>`;
        });
        grid.innerHTML = html;
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
        if (next.current) {
            document.getElementById('currentPeriod').textContent =
                `${PRAYER_NAMES_FA[next.current]} منقضی شد`;
        } else {
            document.getElementById('currentPeriod').textContent = 'قبل از اذان صبح';
        }
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

    // ============== Converter (instant two-way binding) ==============
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

    // ============== Settings + confirmation modal ==============
    function setupSettings() {
        const btn = document.getElementById('settingsBtn');
        const panel = document.getElementById('settingsPanel');
        const search = document.getElementById('citySearch');
        const datalist = document.getElementById('cityList');
        const useAuto = document.getElementById('useAutoLocation');
        const close = document.getElementById('closeSettings');

        // Make city name clickable too
        const cityLabel = document.getElementById('currentCity');
        cityLabel.style.cursor = 'pointer';
        cityLabel.addEventListener('click', () => panel.classList.toggle('open'));

        PRESET_CITIES.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.name;
            datalist.appendChild(opt);
        });

        btn.addEventListener('click', () => panel.classList.toggle('open'));
        close.addEventListener('click', () => panel.classList.remove('open'));

        search.addEventListener('change', () => {
            const match = PRESET_CITIES.find(c =>
                c.name === search.value || c.en.toLowerCase() === search.value.toLowerCase()
            );
            if (match) {
                state.city = { ...match };
                saveCity(state.city);
                panel.classList.remove('open');
                renderAll();
            }
        });

        useAuto.addEventListener('click', async () => {
            panel.classList.remove('open');
            cityLabel.textContent = 'در حال تشخیص...';
            const city = await autoDetectCity();
            if (city) {
                showConfirmModal(city);
            } else {
                cityLabel.textContent = 'تشخیص ناموفق';
            }
        });
    }

    function showConfirmModal(city) {
        state.pendingDetectedCity = city;
        document.getElementById('detectedCity').textContent = city.name;
        document.getElementById('confirmModal').classList.add('open');
    }

    function setupConfirmModal() {
        const modal = document.getElementById('confirmModal');
        const confirmBtn = document.getElementById('confirmCity');
        const rejectBtn = document.getElementById('rejectCity');

        confirmBtn.addEventListener('click', () => {
            if (state.pendingDetectedCity) {
                state.city = state.pendingDetectedCity;
                saveCity(state.city);
                renderAll();
            }
            modal.classList.remove('open');
            state.pendingDetectedCity = null;
        });

        rejectBtn.addEventListener('click', () => {
            modal.classList.remove('open');
            // Open the settings panel so the user can pick a city manually
            document.getElementById('settingsPanel').classList.add('open');
            document.getElementById('citySearch').focus();
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
        // Hide initially — show only when install is available
        installBtn.style.display = 'none';

        window.addEventListener('beforeinstallprompt', (e) => {
            // Prevent the mini-infobar from appearing on mobile
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
        // Apply saved theme first (before any rendering)
        applyTheme(loadTheme());

        setupConverter();
        setupSettings();
        setupConfirmModal();
        setupThemes();
        setupInstallPrompt();

        // Register service worker for offline support
        if ('serviceWorker' in navigator) {
            try {
                await navigator.serviceWorker.register('sw.js');
            } catch (e) {
                // SW registration failed — site still works online
            }
        }

        // Load saved city, or auto-detect on first visit
        const saved = loadCity();
        if (saved) {
            state.city = saved;
            renderAll();
        } else {
            document.getElementById('currentCity').textContent = 'در حال تشخیص خودکار...';
            const city = await autoDetectCity();
            if (city) {
                showConfirmModal(city);
            } else {
                state.city = PRESET_CITIES[0];
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
