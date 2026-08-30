# Life Clock — Android App

Native Android widget app for **Life Clock** (ساعت زندگی) — a personal clock
where 9 AM always equals real sunrise.

## Features

- 📱 **5 widget sizes**: Small (2×2), Bar (4×1), Medium (4×2), Large (4×4), Prayer (4×3)
- 🎨 **6 themes**: Digital, Analog, Nature, Persian, Glass, Gradient
- 🔔 **Persistent notification** (optional)
- 🌍 **Multi-city** with auto-detection
- 🕌 **Prayer times** (Shia convention)
- 📅 **Persian (Jalali) calendar**
- 🔋 **Battery-efficient** (TextClock + WorkManager)
- 📶 **Offline-first** — NOAA sunrise formula, no API needed

## Build

Requirements:
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

```bash
cd android
./gradlew assembleRelease
```

The release APK will be at `app/build/outputs/apk/release/app-release.apk`.

## Signing

The app is signed with `lifeclock-release.keystore` (alias: `lifeclock`).

**For local builds**: place the keystore at `app/lifeclock-release.keystore`.
See `keystore-info.md` (in the keystore zip download) for credentials.

**For CI builds**: the keystore is stored as a GitHub secret (`KEYSTORE_BASE64`).
The workflow in `.github/workflows/build-apk.yml` decodes it automatically.

## Architecture

```
com.lifeclock
├── LifeClockApp                — Application init
├── MainActivity                 — Compose UI host
├── domain/                      — Pure-Kotlin logic
│   ├── LifeClockCalculator      — Core: lifeClock = localTime + (9h - sunrise)
│   ├── SunriseCalculator         — NOAA solar position algorithm
│   ├── IslamicPrayerCalculator   — 5 daily prayer times (Shia)
│   ├── PersianCalendar           — Jalali calendar
│   └── ...
├── data/                        — Repositories (DataStore, Location, Sunrise API)
├── widget/                      — 5 widget receivers + renderer
├── service/                     — WorkManager + notification service
└── ui/                          — Jetpack Compose UI
```

## Live Website

Visit <https://creativewavecom.github.io/lifeclock/> for the web version.

## License

Personal use. See repository root for details.
