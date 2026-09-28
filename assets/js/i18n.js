/* ============================================================
 * Life Clock — i18n (Internationalization) System
 *
 * Supports: Persian (fa), Arabic (ar), English (en)
 *
 * Language detection:
 *   URL path /en/ → English
 *   URL path /ar/ → Arabic
 *   Default (no path) → Persian
 *
 * Prayer time defaults per language:
 *   fa → Shia (Iranian convention)
 *   ar → Sunni (MWL convention)
 *   en → None (hidden by default, user can enable)
 *
 * User can override prayer method in settings (localStorage).
 * ============================================================ */

const I18N = {
    // Language metadata
    languages: {
        fa: {
            name: 'فارسی',
            en: 'Persian',
            dir: 'rtl',
            code: 'fa-IR',
            prayerDefault: 'shia',
            prayerLabel: 'اوقات شرعی'
        },
        ar: {
            name: 'العربية',
            en: 'Arabic',
            dir: 'rtl',
            code: 'ar-SA',
            prayerDefault: 'sunni',
            prayerLabel: 'أوقات الصلاة'
        },
        en: {
            name: 'English',
            en: 'English',
            dir: 'ltr',
            code: 'en-US',
            prayerDefault: 'none',
            prayerLabel: 'Prayer Times'
        }
    },

    // Prayer calculation methods
    prayerMethods: {
        shia: {
            name: { fa: 'شیعه (ایرانی)', ar: 'الشيعة (الإيرانية)', en: 'Shia (Iranian)' },
            fajrAngle: 17.7,
            ishaAngle: 14.0,
            asrFactor: 1,
            maghribOffset: 0  // sunset = maghrib
        },
        sunni: {
            name: { fa: 'سنی (MWL)', ar: 'السنة (MWL)', ar_short: 'السنة', en: 'Sunni (MWL)' },
            fajrAngle: 15.0,
            ishaAngle: 15.0,
            asrFactor: 1,  // Shafi'i (standard in most Arab countries)
            maghribOffset: 0
        },
        none: {
            name: { fa: 'بدون اوقات شرعی', ar: 'بدون أوقات الصلاة', en: 'No prayer times' },
            fajrAngle: null,
            ishaAngle: null,
            asrFactor: null,
            maghribOffset: null
        }
    },

    // Translation strings
    strings: {
        fa: {
            // Nav
            brand: '⚡ ساعت زندگی',
            nav_widget: 'ویجت',
            nav_definition: 'تعریف',
            nav_converter: 'مبدل',
            nav_diff: 'اختلاف',
            nav_prayer: 'اوقات شرعی',
            nav_blog: 'بلاگ',
            nav_faq: 'سؤالات متداول',
            nav_about: 'درباره',

            // Widget
            life_clock: 'ساعت زندگی',
            official_time: 'ساعت رسمی',
            sunrise: 'طلوع',
            sunset: 'غروب',
            day_progress: 'پیشرفت روز',
            day_length: 'طول روز',
            next_prayer: 'تا اذان بعدی',
            city_label: 'برای تغییر شهر کلیک کنید',
            detecting: 'در حال تشخیص...',
            prayer_fajr: 'صبح',
            prayer_sunrise: 'طلوع',
            prayer_dhuhr: 'ظهر',
            prayer_asr: 'عصر',
            prayer_maghrib: 'مغرب',
            prayer_isha: 'عشاء',

            // Definition
            definition_title: '🌟 ساعت زندگی چیست؟',
            definition_text: '<strong>ساعتی که مناسب زندگی است</strong> و ساعت ۹ صبح آن همیشه طلوع آفتاب است. با زندگی با این ساعت می‌توانیم حدود ساعت ۱۲ بخوابیم و ساعت ۹ طلوع آفتاب را ببینیم، و به جای زندگی در شب، در صبح زندگی کنیم.',
            slogan_1: '«زندگی سیاه و سفید را رنگی کنیم»',
            slogan_2: 'صبح‌ها زندگی رنگ تازه‌ای دارد. به زندگی رنگ تازه‌ای بدهیم.',
            theme_label: '🎨 تم:',

            // Converter
            converter_title: '🔁 مبدل ساعت رسمی ⇄ ساعت زندگی',
            converter_hint: 'زمان فعلی پر شده. هر عددی تایپ کنید، در لحظه تبدیل می‌شه.',
            converter_official: 'ساعت رسمی',
            converter_life: 'ساعت زندگی',
            converter_official_sub: 'به وقت شهر شما',
            converter_life_sub: 'بر اساس طلوع امروز',
            converter_diff: 'اختلاف',
            converter_sunrise: 'طلوع',
            converter_sunset: 'غروب',

            // Diff
            diff_title: '⏱ اختلاف زمانی',
            diff_hint: 'اختلاف هر روز با طلوع آفتاب تغییر می‌کنه — در تابستان کمتر، در زمستان بیشتر.',
            diff_life: 'ساعت زندگی',
            diff_official: 'ساعت رسمی',
            diff_minutes: 'دقیقه',

            // Prayer table
            prayer_title: '🕌 اوقات شرعی به همراه ساعت زندگی',
            prayer_hint: 'اوقات شرعی شهرتون به همراه معادل ساعت زندگی هر کدوم.',
            prayer_name: 'نام',
            prayer_official_h: 'رسمی',
            prayer_life_h: 'زندگی',
            prayer_status: 'وضعیت',
            status_passed: 'گذشته',
            status_upcoming: 'بعدی',
            status_current: 'اذان بعدی',
            prayer_fajr_full: 'اذان صبح',
            prayer_dhuhr_full: 'اذان ظهر',
            prayer_asr_full: 'اذان عصر',
            prayer_maghrib_full: 'اذان مغرب',
            prayer_isha_full: 'اذان عشاء',

            // Blog
            blog_title: '📚 بلاگ و تحقیقات',
            blog_hint: 'مقالات و تحقیقات درباره ساعت زندگی، فواید استفاده از آن، و مبانی علمی.',

            // About
            about_title: 'ℹ️ درباره ساعت زندگی',
            about_download: '📲 دانلود اپ اندروید',
            about_download_hint: 'نسخه نصبی اندروید (APK) رو مستقیم دانلود کنید — بدون نیاز به Google Play.',
            about_download_btn: 'دانلود APK',
            about_download_note: 'برای نصب، اجازه «نصب از منابع ناشناس» رو به مرورگرتون بدید.',

            // Settings
            settings_title: 'انتخاب شهر',
            settings_search: 'نام شهر را تایپ کنید...',
            settings_auto: '📍 تشخیص خودکار از روی موقعیت',
            settings_close: 'بستن',

            // City detection
            detected_city: 'شهر شما',
            detected_correct: 'آیا این درست است؟',
            confirm_yes: '✓ بله، درست است',
            confirm_no: '✗ انتخاب دستی',
            gps_label: '(تشخیص دقیق از GPS)',
            ip_label: '(تشخیص حدودی از IP)',
            fallback_label: '(پیش‌فرض)',

            // Footer
            footer_made: 'ساخته‌شده با ❤️ —',
            footer_copy: '© ۲۰۲۴ — ساعت زندگی. محاسبات با فرمول NOAA، دقت ±۲ دقیقه.',

            // Prayer method settings
            prayer_method_label: 'روش محاسبه اوقات شرعی',
            prayer_method_shia: 'شیعه (ایرانی)',
            prayer_method_sunni: 'سنی (MWL)',
            prayer_method_none: 'بدون اوقات شرعی',

            // Misc
            loading: 'در حال محاسبه...',
            no_prayer: 'اوقات شرعی غیرفعال است',
            enable_prayer: 'فعال‌سازی اوقات شرعی'
        },

        ar: {
            brand: '⚡ ساعة الحياة',
            nav_widget: 'الأداة',
            nav_definition: 'التعريف',
            nav_converter: 'المحول',
            nav_diff: 'الفرق',
            nav_prayer: 'أوقات الصلاة',
            nav_blog: 'المدونة',
            nav_faq: 'الأسئلة الشائعة',
            nav_about: 'حول',

            life_clock: 'ساعة الحياة',
            official_time: 'الوقت الرسمي',
            sunrise: 'الشروق',
            sunset: 'الغروب',
            day_progress: 'تقدم اليوم',
            day_length: 'طول اليوم',
            next_prayer: 'حتى الصلاة التالية',
            city_label: 'انقرض لتغيير المدينة',
            detecting: 'جاري الكشف...',
            prayer_fajr: 'الفجر',
            prayer_sunrise: 'الشروق',
            prayer_dhuhr: 'الظهر',
            prayer_asr: 'العصر',
            prayer_maghrib: 'المغرب',
            prayer_isha: 'العشاء',

            definition_title: '🌟 ما هي ساعة الحياة؟',
            definition_text: '<strong>ساعة مناسبة للحياة</strong> حيث الساعة 9 صباحاً فيها تساوي دائماً شروق الشمس الحقيقي. مع العيش بهذه الساعة، يمكننا النوم حوالي الساعة 12 ورؤية شروق الشمس في الساعة 9، والعيش في الصباح بدلاً من الليل.',
            slogan_1: '«لنلوّن الحياة بالأبيض والأسود»',
            slogan_2: 'الصباح يمنح الحياة لوناً جديداً. لنمنح الحياة لوناً جديداً.',
            theme_label: '🎨 السمة:',

            converter_title: '🔁 محول الوقت الرسمي ⇄ ساعة الحياة',
            converter_hint: 'الوقت الحالي مُعبأ. اكتب أي رقم للتحويل الفوري.',
            converter_official: 'الوقت الرسمي',
            converter_life: 'ساعة الحياة',
            converter_official_sub: 'بتوقيت مدينتك',
            converter_life_sub: 'بناءً على شروق اليوم',
            converter_diff: 'الفرق',
            converter_sunrise: 'الشروق',
            converter_sunset: 'الغروب',

            diff_title: '⏱ فرق الوقت',
            diff_hint: 'الفرق يتغير كل يوم مع شروق الشمس — أقل في الصيف، أكثر في الشتاء.',
            diff_life: 'ساعة الحياة',
            diff_official: 'الوقت الرسمي',
            diff_minutes: 'دقيقة',

            prayer_title: '🕌 أوقات الصلاة مع ساعة الحياة',
            prayer_hint: 'أوقات الصلاة في مدينتك مع ما يعادلها في ساعة الحياة.',
            prayer_name: 'الاسم',
            prayer_official_h: 'رسمي',
            prayer_life_h: 'حياة',
            prayer_status: 'الحالة',
            status_passed: 'انتهى',
            status_upcoming: 'التالي',
            status_current: 'الصلاة التالية',
            prayer_fajr_full: 'أذان الفجر',
            prayer_dhuhr_full: 'أذان الظهر',
            prayer_asr_full: 'أذان العصر',
            prayer_maghrib_full: 'أذان المغرب',
            prayer_isha_full: 'أذان العشاء',

            blog_title: '📚 المدونة والأبحاث',
            blog_hint: 'مقالات وأبحاث عن ساعة الحياة وفوائد استخدامها والأسس العلمية.',

            about_title: 'ℹ️ حول ساعة الحياة',
            about_download: '📲 تحميل تطبيق أندرويد',
            about_download_hint: 'حمّل نسخة أندرويد (APK) مباشرة — بدون Google Play.',
            about_download_btn: 'تحميل APK',
            about_download_note: 'للتثبيت، اسمح بـ "التثبيت من مصادر غير معروفة" للمتصفح.',

            settings_title: 'اختر المدينة',
            settings_search: 'اكتب اسم المدينة...',
            settings_auto: '📍 الكشف التلقائي عن الموقع',
            settings_close: 'إغلاق',

            detected_city: 'مدينتك',
            detected_correct: 'هل هذا صحيح؟',
            confirm_yes: '✓ نعم، صحيح',
            confirm_no: '✗ اختيار يدوي',
            gps_label: '(كشف دقيق بـ GPS)',
            ip_label: '(كشف تقريبي بـ IP)',
            fallback_label: '(افتراضي)',

            footer_made: 'صُنع بـ ❤️ —',
            footer_copy: '© ۲۰۲۴ — ساعة الحياة. الحسابات بصيغة NOAA، دقة ±۲ دقيقة.',

            prayer_method_label: 'طريقة حساب أوقات الصلاة',
            prayer_method_shia: 'الشيعة (الإيرانية)',
            prayer_method_sunni: 'السنة (MWL)',
            prayer_method_none: 'بدون أوقات الصلاة',

            loading: 'جاري الحساب...',
            no_prayer: 'أوقات الصلاة معطلة',
            enable_prayer: 'تفعيل أوقات الصلاة'
        },

        en: {
            brand: '⚡ Life Clock',
            nav_widget: 'Widget',
            nav_definition: 'Definition',
            nav_converter: 'Converter',
            nav_diff: 'Difference',
            nav_prayer: 'Prayer Times',
            nav_blog: 'Blog',
            nav_faq: 'FAQ',
            nav_about: 'About',

            life_clock: 'Life Clock',
            official_time: 'Official Time',
            sunrise: 'Sunrise',
            sunset: 'Sunset',
            day_progress: 'Day Progress',
            day_length: 'Day Length',
            next_prayer: 'Until next prayer',
            city_label: 'Click to change city',
            detecting: 'Detecting...',
            prayer_fajr: 'Fajr',
            prayer_sunrise: 'Sunrise',
            prayer_dhuhr: 'Dhuhr',
            prayer_asr: 'Asr',
            prayer_maghrib: 'Maghrib',
            prayer_isha: 'Isha',

            definition_title: '🌟 What is Life Clock?',
            definition_text: '<strong>A clock suited for life</strong> where 9 AM always equals the real sunrise. With this clock, we can sleep around 12 and see the sunrise at 9, living in the morning instead of the night.',
            slogan_1: '“Let\'s color the black-and-white life”',
            slogan_2: 'Mornings give life a fresh color. Let\'s give life a new color.',
            theme_label: '🎨 Theme:',

            converter_title: '🔁 Official ⇄ Life Clock Converter',
            converter_hint: 'Current time is pre-filled. Type any number to convert instantly.',
            converter_official: 'Official Time',
            converter_life: 'Life Clock',
            converter_official_sub: 'Your city\'s local time',
            converter_life_sub: 'Based on today\'s sunrise',
            converter_diff: 'Offset',
            converter_sunrise: 'Sunrise',
            converter_sunset: 'Sunset',

            diff_title: '⏱ Time Difference',
            diff_hint: 'The difference changes daily with sunrise — less in summer, more in winter.',
            diff_life: 'Life Clock',
            diff_official: 'Official Time',
            diff_minutes: 'minutes',

            prayer_title: '🕌 Prayer Times with Life Clock',
            prayer_hint: 'Prayer times for your city with their Life Clock equivalents.',
            prayer_name: 'Name',
            prayer_official_h: 'Official',
            prayer_life_h: 'Life',
            prayer_status: 'Status',
            status_passed: 'Passed',
            status_upcoming: 'Next',
            status_current: 'Next prayer',
            prayer_fajr_full: 'Fajr (Dawn)',
            prayer_dhuhr_full: 'Dhuhr (Noon)',
            prayer_asr_full: 'Asr (Afternoon)',
            prayer_maghrib_full: 'Maghrib (Sunset)',
            prayer_isha_full: 'Isha (Night)',

            blog_title: '📚 Blog & Research',
            blog_hint: 'Articles and research about Life Clock, its benefits, and scientific foundations.',

            about_title: 'ℹ️ About Life Clock',
            about_download: '📲 Download Android App',
            about_download_hint: 'Download the Android APK directly — no Google Play needed.',
            about_download_btn: 'Download APK',
            about_download_note: 'To install, allow "Install from unknown sources" in your browser.',

            settings_title: 'Select City',
            settings_search: 'Type city name...',
            settings_auto: '📍 Auto-detect from location',
            settings_close: 'Close',

            detected_city: 'Your city',
            detected_correct: 'Is this correct?',
            confirm_yes: '✓ Yes, correct',
            confirm_no: '✗ Manual selection',
            gps_label: '(accurate GPS detection)',
            ip_label: '(approximate IP detection)',
            fallback_label: '(default)',

            footer_made: 'Made with ❤️ —',
            footer_copy: '© 2024 — Life Clock. Calculations via NOAA formula, ±2 min accuracy.',

            prayer_method_label: 'Prayer time calculation method',
            prayer_method_shia: 'Shia (Iranian)',
            prayer_method_sunni: 'Sunni (MWL)',
            prayer_method_none: 'No prayer times',

            loading: 'Calculating...',
            no_prayer: 'Prayer times are disabled',
            enable_prayer: 'Enable prayer times'
        }
    },

    // ============== API ==============

    /**
     * Detect language from URL path.
     * /en/... → en, /ar/... → ar, / → fa (default)
     */
    detectLanguage() {
        const path = window.location.pathname;
        // Remove the base path (e.g., /lifeclock/)
        const baseRegex = /\/lifeclock\//;
        const cleanPath = path.replace(baseRegex, '/');

        if (cleanPath.startsWith('/en/') || cleanPath === '/en' || cleanPath === '/en/index.html') {
            return 'en';
        }
        if (cleanPath.startsWith('/ar/') || cleanPath === '/ar' || cleanPath === '/ar/index.html') {
            return 'ar';
        }
        return 'fa'; // Default
    },

    /**
     * Get translation string for current language.
     */
    t(key, lang) {
        const l = lang || this.detectLanguage();
        return (this.strings[l] && this.strings[l][key]) || this.strings.en[key] || key;
    },

    /**
     * Get prayer method for current language (or user override).
     */
    getPrayerMethod() {
        // Check user override first
        try {
            const override = localStorage.getItem('lifeclock.prayerMethod');
            if (override && this.prayerMethods[override]) return override;
        } catch (e) {}

        // Default based on language
        const lang = this.detectLanguage();
        return this.languages[lang].prayerDefault;
    },

    /**
     * Set prayer method (user override).
     */
    setPrayerMethod(method) {
        try { localStorage.setItem('lifeclock.prayerMethod', method); } catch (e) {}
    },

    /**
     * Get prayer calculation parameters.
     */
    getPrayerParams() {
        const method = this.getPrayerMethod();
        return this.prayerMethods[method] || this.prayerMethods.none;
    },

    /**
     * Get language metadata.
     */
    getLangMeta(lang) {
        const l = lang || this.detectLanguage();
        return this.languages[l] || this.languages.fa;
    },

    /**
     * Apply translations to all elements with data-i18n attribute.
     */
    applyTranslations(lang) {
        const l = lang || this.detectLanguage();
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const text = this.t(key, l);
            if (el.tagName === 'INPUT' && el.type === 'text') {
                el.placeholder = text;
            } else {
                el.innerHTML = text;
            }
        });

        // Set html lang and dir
        const meta = this.getLangMeta(l);
        document.documentElement.lang = meta.code;
        document.documentElement.dir = meta.dir;
    }
};

// Export for use in app.js
if (typeof window !== 'undefined') {
    window.I18N = I18N;
}
