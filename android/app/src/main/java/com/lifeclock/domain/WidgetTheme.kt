package com.lifeclock.domain

/**
 * Visual themes supported by the widgets.
 *
 * Each theme has an identifier (used in DataStore) and a default color
 * scheme that is consumed both by the RemoteViews widget layouts and by
 * the in-app preview (Jetpack Compose).
 */
enum class WidgetTheme(val id: String) {
    DIGITAL_MINIMAL("digital_minimal"),
    ANALOG("analog"),
    NATURE_SUN("nature_sun"),
    PERSIAN_TRADITIONAL("persian_traditional"),
    GLASS_DARK("glass_dark"),
    MODERN_GRADIENT("modern_gradient");

    companion object {
        fun fromId(id: String?): WidgetTheme =
            entries.firstOrNull { it.id == id } ?: DIGITAL_MINIMAL
    }
}

/**
 * Update frequency for widget refresh and notification refresh.
 *
 * 30s gives near-live seconds on the clock but uses WorkManager
 * expedited requests to keep battery cost minimal.
 * 5m is the recommended default for battery savings.
 */
enum class UpdateFrequency(val id: String, val seconds: Long) {
    EVERY_30_SECONDS("30s", 30),
    EVERY_1_MINUTE("1m", 60),
    EVERY_5_MINUTES("5m", 300);

    companion object {
        fun fromId(id: String?): UpdateFrequency =
            entries.firstOrNull { it.id == id } ?: EVERY_1_MINUTE
    }
}

/**
 * Sunrise data source — controls whether to call the sunrise-sunset.org API.
 */
enum class SunriseSource(val id: String) {
    OFFLINE("offline"),
    API("api"),
    HYBRID("hybrid");

    companion object {
        fun fromId(id: String?): SunriseSource =
            entries.firstOrNull { it.id == id } ?: HYBRID
    }
}

/**
 * UI language.
 */
enum class AppLanguage(val id: String, val tag: String) {
    ENGLISH("en", "en"),
    PERSIAN("fa", "fa"),
    SYSTEM("system", "");

    companion object {
        fun fromId(id: String?): AppLanguage =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
