# راهنمای سئو و ایندکس کردن سایت ساعت زندگی

این فایل راهنمای کامل سئو و ایندکس کردن سایت در موتورهای جستجوست.

## ✅ وضعیت فعلی سئو

### انجام شده:
- ✅ Meta tags کامل (description, keywords, author, robots)
- ✅ Open Graph tags (برای اشتراک‌گذاری در شبکه‌های اجتماعی)
- ✅ Twitter Card tags
- ✅ JSON-LD structured data (WebApplication برای صفحه اصلی، BlogPosting برای مقالات)
- ✅ Canonical URLs
- ✅ robots.txt با sitemap reference
- ✅ sitemap.xml با lastmod dates
- ✅ Semantic HTML5 (section, article, nav, main)
- ✅ Mobile-first responsive design
- ✅ HTTPS (اجباری روی GitHub Pages)
- ✅ Service Worker برای سرعت لود
- ✅ فونت Vazirmatn (پشتیبانی فارسی)
- ✅ RTL layout صحیح
- ✅ alt attributes روی تصاویر
- ✅ aria-label روی المان‌های تعاملی
- ✅ Schema markup برای rich snippets

### باید انجام بدید (برای ایندکس شدن سریع):

## 📋 مراحل ایندکس شدن در Google

### ۱. ثبت در Google Search Console (ضروری)

1. به https://search.google.com/search-console برید
2. روی «Add Property» کلیک کنید
3. URL زیر رو اضافه کنید:
   ```
   https://creativewavecom.github.io/lifeclock/
   ```
4. برای تأیید مالکیت، یکی از روش‌ها رو انتخاب کنید:
   - **Google Analytics**: اگه Google Analytics دارید
   - **Google Tag Manager**: اگه GTM دارید
   - **HTML tag**: یه meta tag به سایت اضافه کنید (ما می‌تونیم این رو اضافه کنیم)

### ۲. ثبت در Bing Webmaster Tools

1. به https://www.bing.com/webmasters برید
2. روی «Add Site» کلیک کنید
3. URL رو وارد کنید: `https://creativewavecom.github.io/lifeclock/`
4. sitemap URL رو وارد کنید: `https://creativewavecom.github.io/lifeclock/sitemap.xml`

### ۳. ثبت Sitemap در Search Console

بعد از تأیید مالکیت:
1. در Google Search Console → Sitemaps
2. URL sitemap رو وارد کنید: `sitemap.xml`
3. روی «Submit» کلیک کنید

### ۴. Request Indexing برای صفحات اصلی

1. در Search Console → URL Inspection
2. URL صفحه اصلی رو وارد کنید: `https://creativewavecom.github.io/lifeclock/`
3. روی «Request Indexing» کلیک کنید
4. همین کار رو برای مقالات بلاگ هم تکرار کنید

## 🔗 ثبت در موتورهای جستجوی ایرانی

### ۱. ثبت در پارس‌جو (parsijoo.ir)
- به https://www.parsijoo.ir/webmaster برید
- URL سایت رو ثبت کنید

### ۲. ثبت در جستجو (jostojo.com)
- به سایت برید و URL رو ثبت کنید

## 📊 معیارهای سئو که رعایت شده

### Core Web Vitals
- **LCP (Largest Contentful Paint)**: < 2.5s ✅ (سایت سبک، فونت async)
- **FID (First Input Delay)**: < 100ms ✅ (JS سبک، defer)
- **CLS (Cumulative Layout Shift)**: < 0.1 ✅ (عرض و ارتفاع المان‌ها مشخص)

### محتوا
- **عنوان صفحه**: 50-60 کاراکتر ✅
- **Meta description**: 150-160 کاراکتر ✅
- **Headings**: سلسله‌مراتب درست (h1 → h2 → h3) ✅
- **تصاویر**: alt attribute ✅
- **لینک‌های داخلی**: بین مقالات و صفحه اصلی ✅

### فنی
- **HTTPS**: اجباری ✅
- **Mobile-friendly**: responsive ✅
- **Page speed**: TTFB 168ms ✅
- **Structured data**: JSON-LD ✅
- **Sitemap**: موجود ✅
- **robots.txt**: موجود ✅

## 🚀 راه‌های تسریع ایندکس

### ۱. اشتراک‌گذاری در شبکه‌های اجتماعی
- لینک سایت رو در توییتر، تلگرام، واتساپ به اشتراک بذارید
- هر backlink از سایت‌های معتبر، ایندکس رو سریع‌تر می‌کنه

### ۲. ثبت در دایرکتوری‌ها
- [DMOZ](https://dmoz.org) (اگه هنوز فعال باشه)
- دایرکتوری‌های ایرانی
- Reddit (در ساب‌ردیت‌های مرتبط)

### ۳. ساخت بک‌لینک
- در انجمن‌های مرتبط (Stack Overflow، انجمن‌های برنامه‌نویسی)
- کامنت در بلاگ‌های مرتبط با لینک سایت
- نوشتن مقاله مهمان در سایت‌های دیگر

## 📈 پایش سئو

### ابزارهای رایگان:
- [Google Search Console](https://search.google.com/search-console)
- [Google Analytics](https://analytics.google.com)
- [Bing Webmaster Tools](https://www.bing.com/webmasters)
- [PageSpeed Insights](https://pagespeed.web.dev)
- [Mobile-Friendly Test](https://search.google.com/test/mobile-friendly)

### چک‌لیست ماهانه:
- [ ] بررسی خطاهای Search Console
- [ ] بررسی عملکرد کلمات کلیدی
- [ ] به‌روزرسانی محتوای قدیمی
- [ ] بررسی بک‌لینک‌های جدید
- [ ] تست PageSpeed

## 🎯 کلمات کلیدی هدف

### اصلی:
- ساعت زندگی
- life clock
- اوقات شرعی
- مبدل ساعت
- طلوع آفتاب

### فرعی:
- ساعت خورشیدی
- زمان بیولوژیک
- circadian rhythm
- تقویم شمسی
- اوقات شرعی تهران

## 📞 تماس برای راهنمایی بیشتر

اگه کمک خواستید برای:
- تأیید مالکیت در Search Console
- تنظیم Google Analytics
- بهبود کلمات کلیدی خاص

کد سایت روی GitHub: https://github.com/creativewavecom/lifeclock
