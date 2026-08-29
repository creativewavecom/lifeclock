# Life Clock — ساعت زندگی

A privacy-first, offline-capable web app that shows the **Life Clock** (ساعت زندگی) —
a personal timekeeping system where **9 AM always equals real sunrise** for the
user's location.

**Live site:** <https://creativewavecom.github.io/lifeclock/>

## Features

- 🌅 **Live widget** — current life clock + prayer times for your auto-detected city
- 🔁 **Two-way converter** — official ⇄ life clock, instant conversion on input
- ⏱ **Time difference** visualization — see exactly how many minutes ahead/behind
- 🕌 **Prayer times table** — five daily prayers with both official and life-clock times
- 📚 **Blog & research** — six articles on the science, math, and benefits
- 📍 **Auto city detection** — uses browser geolocation (first time) or IP fallback
- 🔒 **Privacy-first** — no tracking, no analytics, no external API calls after page load
- 🌐 **Bilingual** — Persian (RTL) UI with Vazirmatn font

## Tech

- Vanilla HTML + CSS + JavaScript (no build step, no dependencies)
- 100% offline after first load — all math runs in the browser
- Sunrise calculation uses the NOAA Solar Position algorithm (±2 min accuracy)
- Persian (Jalali) calendar implemented from scratch

## Structure

```
.
├── index.html                 # Main SPA
├── assets/
│   ├── css/style.css         # All styles
│   ├── js/app.js             # All logic
│   └── img/favicon.svg
└── blog/
    ├── what-is-life-clock.html
    ├── benefits.html
    ├── circadian-rhythm.html
    ├── sun-time-vs-clock-time.html
    ├── prayer-times-and-life-clock.html
    └── implementing-offline.html
```

## Development

Just open `index.html` in a browser. No server needed.

For local development with hot reload:

```bash
python3 -m http.server 8000
# open http://localhost:8000
```

## License

MIT — see source files.
