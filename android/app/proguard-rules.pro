# Project-specific ProGuard rules for Life Clock

# Keep all widget receivers — Android instantiates them via reflection.
-keep class com.lifeclock.widget.* extends android.appwidget.AppWidgetProvider { *; }
-keep class com.lifeclock.widget.WidgetPinReceiver { *; }
-keep class com.lifeclock.service.BootReceiver { *; }
-keep class com.lifeclock.service.LifeClockNotificationService { *; }
-keep class com.lifeclock.MainActivity { *; }
-keep class com.lifeclock.LifeClockApp { *; }

# Keep enum methods (valueOf, values) — used in DataStore serialization.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Joda-Time — uses reflection for its chronology registry.
-keep class org.joda.time.** { *; }
-dontwarn org.joda.time.**

# Keep RemoteViews actions (called via reflection on the system side).
-keepclassmembers class androidx.widget.RemoteViewsCompat { *; }

# Keep AppCompat delegate (used for locale switching).
-keep class androidx.appcompat.app.AppCompatDelegate { *; }
-keep class androidx.core.os.LocaleListCompat { *; }

# Kotlin coroutines — keep continuation classes for suspend functions.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Play Services location — keep interfaces for runtime reflection.
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Compose — keep runtime metadata (already mostly preserved by Compose's own rules).
-keep class androidx.compose.runtime.** { *; }
