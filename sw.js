// Life Clock Service Worker
// Caches the entire app shell for offline use.

const CACHE_VERSION = 8;
const CACHE_NAME = `lifeclock-v${CACHE_VERSION}`;

const ASSETS_TO_CACHE = [
    './',
    './index.html',
    './manifest.json',
    './robots.txt',
    './sitemap.xml',
    './assets/css/style.css',
    './assets/js/app.js',
    './assets/img/favicon.svg',
    './assets/img/icon-192.png',
    './assets/img/icon-512.png',
    './assets/img/icon-maskable-192.png',
    './assets/img/icon-maskable-512.png',
    './blog/what-is-life-clock.html',
    './blog/benefits.html',
    './blog/circadian-rhythm.html',
    './blog/sun-time-vs-clock-time.html',
    './blog/prayer-times-and-life-clock.html',
    './blog/implementing-offline.html'
    // NOTE: APK is intentionally NOT cached — it's a large binary that should
    // always be downloaded fresh.
];

// Install: pre-cache the app shell
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => cache.addAll(ASSETS_TO_CACHE))
            .then(() => self.skipWaiting())
            .catch(() => {}) // tolerate missing icons on first install
    );
});

// Activate: clean up old caches
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
            );
        }).then(() => self.clients.claim())
    );
});

// Fetch: network-first for HTML (so updates are visible immediately),
// cache-first for everything else (fast load).
self.addEventListener('fetch', (event) => {
    const req = event.request;

    // Only handle GET
    if (req.method !== 'GET') return;

    // Skip cross-origin requests (e.g. Google Fonts, ipapi.co, bigdatacloud)
    const url = new URL(req.url);
    if (url.origin !== self.location.origin) return;

    // Network-first for navigation (HTML)
    if (req.mode === 'navigate' || req.headers.get('accept')?.includes('text/html')) {
        event.respondWith(
            fetch(req).then((res) => {
                const clone = res.clone();
                caches.open(CACHE_NAME).then((cache) => cache.put('./index.html', clone));
                return res;
            }).catch(() => caches.match('./index.html'))
        );
        return;
    }

    // Cache-first for everything else
    event.respondWith(
        caches.match(req).then((cached) => {
            if (cached) return cached;
            return fetch(req).then((res) => {
                if (res.ok) {
                    const clone = res.clone();
                    caches.open(CACHE_NAME).then((cache) => cache.put(req, clone));
                }
                return res;
            });
        })
    );
});
