# mdbapp — API Discovery Report & Implementation Plan

**Revision 2** — rewritten against the updated engineering brief (v1 scope, no refresh token, MP4-now/HLS-later).  
Revision 1 (which assumed a token-refresh endpoint was needed) is superseded.

|                 |                                                                                                |
| --------------- | ---------------------------------------------------------------------------------------------- |
| Backend repo    | `soldiermr98-commits/mrdb` @ `main`, cloned read-only to `D:\work-webs\0 Build\mrdb-reference` |
| Source of truth | `wordpress-plugin/mrdastbaaf-bridge/includes/*.php` (plugin v1.7.15)                           |
| Base URL        | `https://mrdastbaaf.com/wp-json/mrdastbaaf/v1`                                                 |
| Status          | **Discovery complete. No app code written yet.**                                               |

---

## 0. خلاصهٔ مدیریتی (Persian summary)

پل وردپرس را کامل خواندم. **API فعلی برای v1 اپ کافی است** — ورود پیامکی، پروفایل، وضعیت اشتراک، کاتالوگ، دوره‌های من، جزئیات دوره، پیشرفت، سفارش‌ها، فروشگاه و پخش ویدیو همه موجودند و قرارداد پاسخ‌شان را از دل سورس بیرون کشیدم.

**سه چیز واقعاً کم است (یکی بلاکر است):**

1. **🔴 بلاکر — ذخیرهٔ موقعیت پخش وجود ندارد.** `GET/PUT /me/progress` فقط `completedLessonIds` و `bookmarkedLessonIds` را نگه می‌دارد؛ نه در API و نه در جدول `mrdastbaaf_lessons` هیچ ستون/فیلدی برای «ثانیهٔ آخر» نیست. درخواست شما «resume position (sync with /me/progress)» است — این بدون تغییر سمت سرور قابل انجام نیست.
2. **🟠 تشخیص MP4 در برابر HLS از روی URL ممکن نیست.** تشخیص با پسوند فایل قابل اعتماد نیست: پلاگین فقط `https` + لیست سفید هاست را چک می‌کند (پسوند آزاد است)، و در حالت پروکسی اصلاً URL پسوند ندارد (`/stream?token=…`). یک فیلد `type` لازم است.
3. **🟠 واترمارک اسم کامل ندارد.** بلیط فقط `watermarkText` = `"0912***4567 | ID: 42"` می‌دهد؛ اسم کامل در `/auth/me` است. یعنی اپ باید دو منبع را ترکیب کند (قابل انجام، ولی از یک منبع بهتر است).

**خبر خوب:** «سرور تنها مرجع دسترسی است» ✅ همین حالا درست پیاده شده، و پیام‌های فارسی خطای ۴۰۱/۴۰۳ دقیقاً همان چیزی است که برای پیام «دسترسی ندارید» لازم دارید — پس هیچ اندپوینت اضافه‌ای برای این کار لازم نیست.

**تصحیح مهم نسبت به پیش‌نویس قبلی:** تمدید بلیط در جریان فعلی MP4 **لازم نیست** — بلیط ۱۵ دقیقه‌ای فقط برای *گرفتن* URL است؛ بعد از تحویل، پخش قطع نمی‌شود. این نیاز فقط وقتی HLS بیاید واقعی می‌شود.

---

## 1. Sources read

| File                                                                          | What I took from it                                                                 |
| ----------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `includes/class-auth-controller.php` (56 KB)                                  | OTP flow, token format/lifetime, `/auth/me`, logout, bearer resolution, role guard  |
| `includes/class-courses-controller.php` (66 KB)                               | Catalogue, detail, lessons, ticket, playback, stream proxy, access rules, watermark |
| `includes/class-memberships-controller.php`                                   | Plans, my-status, progress GET/PUT, orders                                          |
| `includes/class-shop-controller.php`                                          | Physical product feed                                                               |
| `includes/class-app-ui-controller.php`                                        | Server-driven home config                                                           |
| `includes/class-app-pages-controller.php`                                     | Editorial pages + access filtering                                                  |
| `includes/class-payment-controller.php`                                       | Initiate / verify / manual transfer (out of v1 scope, read for completeness)        |
| `includes/class-database.php`                                                 | Table schemas (lessons, memberships, orders, OTP, grants)                           |
| `mrdastbaaf-bridge.php` (58 KB)                                               | Namespace, route registration, CORS, version endpoint                               |
| `wordpress-plugin/README.md`                                                  | Version history, confirmed security model                                           |
| `FullDoc.MD` §4–§7, `docs/VIDEO_SECURITY.md`, `docs/STREAM_PROXY_DECISION.md` | Intended vs. implemented video security                                             |
| `src/services/*.ts`, `src/types/index.ts`                                     | Reference for flows/wording only (as instructed)                                    |

**Documentation caveat:** `FullDoc.MD` §6 is **stale** in several places — it documents `GET /courses/{id}` (the real routes are `/courses/{slug}` and `/courses/by-id/{id}`), says `verify-otp` "sets a WordPress session cookie" (removed in v1.2.0 — it is Bearer-only now), says the OTP is 5 digits (it is 6), and §7.2 describes HLS segments as the strategy when no HLS exists. **I treated the plugin source as authoritative throughout**, as instructed.

---

## 2. (a) What the API supports — exact shapes

### 2.0 Conventions

- Namespace: `mrdastbaaf/v1`. All responses are JSON with **camelCase keys** — with two documented exceptions called out below.
- Success bodies are **inconsistently wrapped**: some endpoints return a bare array, some a bare object, some `{success, …}`. Every shape below is verbatim from the source.
- Errors are `WP_Error` → HTTP status + `{ "code": "…", "message": "<Persian text>", "data": { "status": <int>, … } }`. **The Persian `message` is safe to show to the user directly.**
- Dates are a mix of **ISO 8601** (`gmdate('c')`, e.g. `2026-10-16T08:00:00+00:00`) and **MySQL local time** (`Y-m-d H:i:s`, no timezone). Noted per field.
- Every response in this namespace carries `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`.

### 2.1 Authentication

#### `POST /auth/request-otp` — public

```jsonc
// Request
{ "phone": "09123456789" }        // server also accepts a leading +98 / 0098 and strips it
                                  // then requires ^09[0-9]{9}$ in ASCII digits

// 200
{
  "success": true,
  "message": "کد تایید پیامکی ارسال گردید. …",
  "expires_in_seconds": 120,       // ⚠️ snake_case — the only snake_case key in the API
  "isRegistered": true,            // camelCase — drives "ورود" vs "ثبتنام" label
  "smsWarning": "…"                // optional; present only when the SMS provider is not configured
}
```

| Error                | Status | Meaning                                                                |
| -------------------- | ------ | ---------------------------------------------------------------------- |
| `invalid_phone`      | 400    | failed `^09[0-9]{9}$` after prefix normalisation                       |
| `rate_limited`       | 429    | four independent quotas: 2 min/phone, 5/h/phone, 15/day/phone, 10/h/IP |
| `role_not_allowed`   | 403    | the phone belongs to a non-student WordPress role                      |
| `otp_storage_failed` | 500    | DB insert failed                                                       |

**Important:** the SMS is sent **asynchronously** in a separate PHP process. This response returns *before* the message leaves the server. The app must not treat "no SMS yet" as an error, and should offer a resend only after the 2-minute window.

#### `POST /auth/verify-otp` — public

```jsonc
// Request
{
  "phone": "09123456789",
  "otp_code": "123456",
  "first_name": "محمدجواد",   // optional
  "last_name":  "فلان"        // optional
}

// 200
{
  "success": true,
  "token": "42.9xK2mQ…",       // opaque, NOT a JWT: "{user_id}.{secret}"
  "user": {                    // ← exactly format_user_profile(), same shape as GET /auth/me
    "id": "42",
    "phoneNumber": "09123456789",
    "maskedPhone": "0912***6789",
    "fullName": "محمدجواد فلان",
    "role": "student",         // "student" | "admin"
    "membership": null         // or the membership object from §2.3
  }
}
```

| Error                   | Status | Notes                                                                                                                          |
| ----------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------ |
| `invalid_otp`           | 400    | includes remaining attempts in the message. **400, not 401** — deliberately, so the client does not treat it as a dead session |
| `otp_locked`            | 429    | 5 wrong attempts burned the code; a new code is required                                                                       |
| `registration_conflict` | 409    | `wp_insert_user` collided on login/email                                                                                       |
| `role_not_allowed`      | 403    | privileged role                                                                                                                |

**Behavioural facts the app must be built around:**

- Token lifetime is **30 days**, checked on every request (`(time() - issued) > 30 * DAY_IN_SECONDS` → invalid). There is **no refresh** and **no `expiresAt`** in the response. Confirmed as the brief states: on 401 → clear the session and go to login.
- **There is exactly one token slot per user** (`update_user_meta(… 'mrdastbaaf_api_token', hash('sha256', $secret))`). Logging in on the phone **invalidates the web PWA session**, and vice versa. This is a product decision the team must accept for v1 (see §3.7).
- Unknown phones are **auto-registered** as `subscriber` with `user_login = phone` and `user_email = {phone}@mrdastbaaf.local`.
- `first_name` / `last_name` are **optional server-side** — see §3.2.
- Server-side name sanitisation keeps only Persian/Arabic letters (`\x{0600}-\x{06FF}`), ZWNJ, and spaces; max 60 chars. **Latin characters are silently dropped.**

#### `GET /auth/me` — Bearer

```jsonc
{
  "id": "42",
  "phoneNumber": "09123456789",
  "maskedPhone": "0912***6789",
  "fullName": "محمدجواد فلان",
  "role": "student",
  "membership": { … } | null
}
```

⚠️ **This is the complete shape.** There is no `completedLessonIds`, `bookmarkedLessonIds`, `avatarUrl`, or `createdAt` — those exist only in the web app's TypeScript type as client-side defaults. The app must call `GET /me/progress` separately. There is also **no token expiry field**, so the client cannot pre-empt a 401.

#### `POST /auth/logout` — Bearer

```jsonc
// 200
{ "success": true, "message": "با موفقیت از حساب کاربری خارج شدید." }
// 401 unauthorized — if the Bearer token is already invalid
```

Revokes the token server-side (deletes both usermeta keys). Always call it, then clear local storage regardless of the outcome.


### 2.2 Content

#### `GET /courses?category={slug}&search={q}` — public, Bearer optional

`category` defaults to the plugin option `mrdastbaaf_course_category` (default `courses`). Max **50** items, WordPress default order (date desc).

```jsonc
// 200 — BARE ARRAY (no wrapper)
[
  {
    "id": "gelim-bafi",                 // product post_name (slug) — the course id used in URLs
    "productId": 123,                   // WooCommerce product id
    "title": "گلیمبافی مقدماتی",
    "description": "…",                 // post_excerpt
    "image": "https://…/x-300x300.jpg", // WP 'medium' size; "" when there is no thumbnail
    "category": "دورههای آموزشی",       // first product_cat term NAME (not slug); "" if none
    "level": "مقدماتی",                  // pa_level attribute, first value; default "مقدماتی"
    "lessonsCount": 24,
    "duration": 480,                    // total minutes across all lessons
    "price": 0,                         // integer Toman
    "freePreviewCount": 3,
    "owned": false                      // ⚠️ true ONLY if a valid Bearer token is attached
  }
]
```

| Error                 | Status |
| --------------------- | ------ |
| `woocommerce_missing` | 503    |

**`owned` requires the Bearer header.** The route itself is public and never rejects an anonymous caller — it just returns `owned: false` for every course. The brief's instruction to send Bearer here is correct and necessary.

#### `GET /courses/by-id/{productId}` and `GET /courses/{slug}` — public, Bearer optional

Both hit the same core. `{slug}` tolerates raw, single-, and double-percent-encoded forms.

```jsonc
// 200 — BARE OBJECT
{
  // …all base fields from the list item above (id, productId, title, description, image,
  //   category, level, duration, price) …
  "lessonsCount": 0,                  // ⚠️ BUG: always 0 on the detail route — see §3.5
  "freePreviewCount": 3,
  "owned": false,
  "lessons": [
    {
      "id": "12",                     // numeric, cast to STRING
      "title": "جلسهٔ اول — معرفی",
      "isFreePreview": false,
      "requiredTier": "gold",         // "bronze" | "silver" | "gold"
      "duration": 22                  // integer minutes, or NULL when 0/unknown
    }
  ]
}
```

**`video_url` is never present in any course or lesson payload** — `get_product_lessons_public()` selects only `id, title, is_free, required_tier, duration_minutes`. This matches the brief and is enforced by design.

| Error                 | Status | Notes                                                                             |
| --------------------- | ------ | --------------------------------------------------------------------------------- |
| `not_found`           | 404    | body carries `data.tried` — up to 8 attempted slug/id forms, useful for debugging |
| `woocommerce_missing` | 503    |                                                                                   |

#### `GET /my/courses` — Bearer required

```jsonc
// 200
{
  "success": true,
  "courses": [ { …same item shape as GET /courses, with "owned": true… } ],
  "count": 2
}
```

Ownership is resolved from paid WooCommerce orders (`wc-completed`, `wc-processing`) matched by `customer_id` **or** by guest `billing_phone` in five normalised phone forms. Ordered by title ASC, capped at 50.

#### `GET /memberships/plans` — public

```jsonc
// 200 — BARE ARRAY (cached 5 minutes server-side)
[
  {
    "id": "gold_4821",
    "tier": "gold",                  // "free" | "bronze" | "silver" | "gold"
    "title": "اشتراک طلایی",
    "description": "…",
    "priceToman": 490000,
    "originalPriceToman": 690000,    // null when not set
    "duration": "365_days",          // ⚠️ "{duration_days}_days" — e.g. "30_days", "365_days"
    "durationLabel": "۱۲ ماه",        // display string, use this in the UI
    "features": ["…", "…"],          // may be []
    "isPopular": true,
    "isActive": true,
    "colorScheme": "amber"           // "slate" | "amber" | "stone" | "yellow"
  }
]
```

#### `GET /memberships/my-status` — Bearer required

```jsonc
// 200 — no active membership
{ "hasActiveMembership": false, "membership": null }

// 200 — active
{
  "hasActiveMembership": true,
  "membership": {
    "tier": "gold",
    "planId": "gold_4821",
    "status": "active",
    "startedAt": "2026-01-01 10:00:00",           // ⚠️ MySQL local time, NOT ISO 8601
    "expiresAt": "2027-01-01 10:00:00",           // ⚠️ MySQL local time
    "formattedExpireDateJalali": "1405/10/11",    // Jalali, pre-formatted by the server
    "daysRemaining": 77                            // integer, ceil, min 0
  }
}
```

The same `membership` object is embedded in `GET /auth/me` and in the `verify-otp` response (with one extra key, `planName`, in those two).

#### `GET /me/progress` / `PUT /me/progress` — Bearer required

```jsonc
// GET 200
{ "completedLessonIds": ["12", "15"], "bookmarkedLessonIds": ["9"] }

// PUT request — BOTH keys optional
{ "completedLessonIds": ["12", "15"], "bookmarkedLessonIds": ["9"] }
// PUT 200 → returns the resulting state (same shape as GET)
```

**Semantics:** each supplied key is **fully replaced**, not merged. An absent key keeps its current value. Sending neither returns the current state without writing. Lesson ids are strings. ⚠️ **There is no playback-position field.** See §4.1.

#### `GET /me/orders` — Bearer required

```jsonc
// 200 — ⚠️ no "success" key, unlike most other endpoints
{
  "orders": [
    {
      "id": "88",
      "orderNumber": "MD-12345",
      "itemTitle": "اشتراک طلایی ۱۲ ماهه",
      "amountToman": 490000,
      "gateway": "zibal",              // "zibal" | "card_to_card"
      "status": "completed",           // pending|processing|verifying|completed|failed|rejected|cancelled
      "createdAt": "2026-10-16 08:00:00",   // ⚠️ MySQL local time
      "verifiedAt": "2026-10-16 08:12:00",  // or null
      "refNumber": "1234567890",            // or null
      "adminNote": "…"                       // ONLY when gateway=card_to_card AND status=rejected
    }
  ]
}
```

#### `GET /shop/products` — public

```jsonc
// 200
{
  "success": true,
  "featured": [ { "id": 71, "title": "…", "price": 850000, "imageUrl": "https://…" | null, "url": "https://mrdastbaaf.com/product/…" } ],
  "categories": [
    { "id": 34, "slug": "handmade", "name": "دستساز", "products": [ { …same product shape… } ] }
  ]
}
```

- `featured` is the slider list (added v1.5.0) and may contain products that are **not** in any category.
- Uncategorised physical products land in a synthetic bucket `{ "id": 0, "slug": "", "name": "سایر" }`.
- `url` is the WooCommerce permalink → open it in the external browser for purchase, exactly as the brief requires.
- Empty selection → `{ success: true, featured: [], categories: [] }`.
- 503 `woocommerce_missing`.

#### `GET /app-ui` — public

```jsonc
// 200
{
  "hero": {
    "badgeText": "…", "headline": "…", "subheadline": "…",
    "ctaPrimaryText": "…", "ctaSecondaryText": "…",
    "stat1Value": "…", "stat1Label": "…",
    "stat2Value": "…", "stat2Label": "…",
    "stat3Value": "…", "stat3Label": "…"
  },
  "sections": [ { "key": "featured_courses", "title": "…", "subtitle": "…", "enabled": true, "sortOrder": 2 } ],
  "menu": {
    "dock":   [ { "key": "home", "label": "خانه", "enabled": true } ],
    "guides": [ { "key": "faq", "label": "سؤالات متداول", "enabled": true, "sortOrder": 4 } ]
  },
  "featuredCourseIds": [123, 456]
}
```

- Section keys: `featured_courses`, `artisan_story`, `membership_plans`, `faq`, `physical_products`. (`categories` is retired and never returned.)
- Dock keys: `home`, `catalogue`, `membership`, `dashboard`.
- Guide keys: `about-us`, `guide-membership`, `faq`, `membership`.
- Unknown stored keys are preserved and passed through — **the Dart model must tolerate unknown enum values** rather than crash.

#### `GET /app-pages` and `GET /app-pages/{id}/content` — public, access-filtered

```jsonc
// GET /app-pages  →  BARE ARRAY
[ { "id": "about", "title": "درباره ما", "access": "all", "sortOrder": 1, "url": "https://…" } ]

// GET /app-pages/{id}/content  →  OBJECT
{
  "id": "about", "title": "درباره ما",
  "html": "<p>…</p>",              // rendered the_content
  "access": "all",                  // "all" | "members" | "admins"
  "updatedAtJalali": "1405/07/24",  // "" when unknown
  "url": "https://…"
}
```

Errors: `not_found` 404 (unknown/inactive record, or the underlying WP page is missing), `forbidden` 403 (access level too high).

#### `GET /version` — public

```jsonc
{
  "pwa_version": "1.5.6",
  "plugin_version": "1.7.15",
  "db_version": "1.5.1",
  "server_time": "2026-10-16 12:00:00",
  "timestamp": 1760625600,
  "content_hash": "…"
}
```

Useful as a cheap reachability/health probe. Note the **snake_case** keys here too.

### 2.3 Playback — the two-step ticket flow

#### Step 1 — `POST /lessons/{id}/playback-ticket` — public, Bearer optional

No request body. `{id}` matches `[a-zA-Z0-9_-]+`.

**Access decision, entirely server-side:**

```
free preview lesson                              → allow (works anonymously, user_id = 0)
admin (manage_options)                           → allow
owns the course product in a paid Woo order      → allow (all lessons of that course)
active membership, tier_rank >= required_tier    → allow   (bronze 1 < silver 2 < gold 3)
otherwise                                        → 403
```

```jsonc
// 200
{
  "ticketId": "tkt_aB3xY9kLmN2pQ4rT",   // 16 random chars after the "tkt_" prefix
  "lessonId": "12",
  "expiresInSeconds": 900,               // 15 minutes
  "issuedAt": "2026-10-16T08:00:00+00:00",   // ISO 8601 UTC
  "expiresAt": "2026-10-16T08:15:00+00:00",  // ISO 8601 UTC
  "watermarkText": "0912***4567 | ID: 42",   // ⚠️ masked phone + numeric id — NO full name
  "watermarkUserId": "42",                   // or "guest" for an anonymous free preview
  "streamType": "hls"                        // ⚠️ hard-coded; currently misleading — see §3.4
}
```

| Error          | Status | `message` (safe to display)                               |
| -------------- | ------ | --------------------------------------------------------- |
| `unauthorized` | 401    | «برای مشاهده این جلسه ابتدا وارد حساب کاربری خود شوید.»   |
| `forbidden`    | 403    | «مشاهده این جلسه نیازمند خرید یا تمدید اشتراک معتبر است.» |
| `rate_limited` | 429    | 30 tickets / hour per (IP + user + lesson)                |

These two Persian messages already satisfy the brief's "clear Persian message when the user has no access" — **no extra endpoint is needed for that.**

#### Step 2 — `POST /lessons/{id}/playback` — public, Bearer optional

```jsonc
// Request
{ "ticketId": "tkt_aB3xY9kLmN2pQ4rT" }

// 200 — proxy mode OFF (the DEFAULT)
{
  "videoUrl": "https://dll.mrdastbaaf.com/FA/lesson-01.mp4",
  "watermarkText": "0912***4567 | ID: 42",
  "expiresAt": "2026-10-16T08:15:00+00:00",
  "qualities": [
    { "label": "1080p", "url": "https://dll.mrdastbaaf.com/FA/lesson-01-1080.mp4" },
    { "label": "720p",  "url": "https://dll.mrdastbaaf.com/FA/lesson-01-720.mp4" }
  ],
  "proxyMode": false
}

// 200 — proxy mode ON (option `mrdastbaaf_stream_proxy_mode`)
{
  "videoUrl": "https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/lessons/12/stream?token=XyZ…",
  "watermarkText": "0912***4567 | ID: 42",
  "expiresAt": "2026-10-16T08:15:00+00:00",
  "qualities": [ { "label": "1080p", "url": "…/lessons/12/stream?token=AbC…" } ],
  "proxyMode": true
}
```

- The ticket must match **both** the current user and the lesson, or the call fails closed.
- `qualities` may be `[]` → the UI shows **"Auto" only**.
- Each `qualities[].url` is validated as `https://` + host in the video allowlist (`dll.mrdastbaaf.com` by default). **The file extension is not validated** — see §3.3.
- ⚠️ In proxy mode the token lives in a **60-second** transient, so playback will die ~60 s into a video. See §3.6.

| Error              | Status                                         |
| ------------------ | ---------------------------------------------- |
| `invalid_ticket`   | 403 (missing or expired)                       |
| `ticket_mismatch`  | 403 (wrong user or lesson)                     |
| `lesson_not_found` | 404 (lesson row missing, or `video_url` empty) |

#### `GET /lessons/{id}/stream?token={token}` — proxy transport

Not JSON. Raw binary with `Content-Type` from the CDN, `Accept-Ranges: bytes`, `Content-Length`, `Content-Range`, `Content-Disposition: inline`, `X-Content-Type-Options: nosniff`, and full `no-store` headers. Supports `Range` → `206`, invalid range → `416`. Errors: `missing_token` 403, `expired_token` 403, `token_mismatch` 403, `no_video` 404, `invalid_host` 403.

### 2.4 Endpoints the app must never call

`POST /payment/zibal/verify` (server-to-server callback), `GET /cron/reconcile?token=` (shared secret in a query string), `POST /auth/otp-dispatch` (internal loopback), all `/admin/*`, and **`GET /auth/health`** — it is public but leaks host internals (DB timings, outbound TCP probe results, SMS provider, registered callback names). Useful on staging; **do not wire it into the production app.**

---

## 3. (b) Mismatches with the brief

### 3.1 ✅ "There is NO refresh token: on 401, clear the session and go to login"

**Matches the implementation exactly.** The token is opaque, 30 days, and there is no refresh path. My earlier P0 recommendation for a refresh endpoint is **withdrawn** — it is not needed for this scope. The dio auth interceptor becomes simpler: attach `Authorization: Bearer`, and on 401 clear `flutter_secure_storage` and redirect to login via a `go_router` guard.

### 3.2 ⚠️ "New users must provide first/last name"

**The server does not enforce this.** `sanitize_person_name()` returns `''` for a missing or invalid value, and a new user is created with `display_name = "هنرجو " . substr($phone, -4)`. Nothing returns an error if both names are omitted.

Consequences for the app:

- The rule becomes a **client-side requirement**. The app should require the name **only when `isRegistered === false`** (i.e. a new account), which is exactly why `request-otp` returns that flag.
- The server's sanitiser accepts **only Persian/Arabic letters, ZWNJ and spaces, max 60 chars**. If the user types Latin characters they are **silently discarded**, and the account ends up as `هنرجو NNNN`. The app's `TextFormField` validators and input formatters must mirror this rule and show a Persian validation message rather than silently losing the name.
- If you want the server to be authoritative here, it needs a change — see §4.5 (optional).

### 3.3 🟠 "detect by URL/extension" (MP4 vs HLS)

**Not reliably possible with the current contract.**

- `sanitize_stored_qualities()` validates only `https://` and the host allowlist. The admin can paste any URL — `.mp4`, `.m3u8`, a path with no extension, or a query-string URL.
- In **proxy mode the URL has no extension at all**: `…/lessons/12/stream?token=XyZ…`.
- The ticket's `streamType` field is **hard-coded to `"hls"`** while an MP4 is actually served. Trusting it today would be wrong.

**Recommendation:** treat `streamType` as advisory, and ask the server to add a truthful per-URL `type` (§4.3). As a client-side fallback, detect by extension **and** fall back to a `HEAD`/`Range: bytes=0-0` content-type probe on the first URL — never assume.

### 3.4 ✅ "Video URLs never appear in course lists"

**Confirmed.** `get_product_lessons_public()` selects `id, title, is_free, required_tier, duration_minutes` only. `video_url` and `qualities_json` never leave the server outside the playback endpoint.

### 3.5 🟠 `lessonsCount` is always `0` on the course-detail route

`serve_single_course()` sets `$course['lessons']` and `$course['freePreviewCount']` but never overwrites `lessonsCount`, which `map_product_to_course()` initialised to `0`. **On `GET /courses/{slug}` and `GET /courses/by-id/{id}`, `lessonsCount` is always `0`** even when `lessons[]` is full. The list route computes it correctly.

→ Client workaround: use `lessons.length` on the detail screen. Server fix is one line (§4.6).

### 3.6 🟠 "renew the ticket before expiry without interrupting playback if possible"

**For the current MP4 flow this is a non-issue, and I want to be precise about why:**

The 15-minute ticket only gates *obtaining* the URL. Once `POST /playback` returns, the URL is in memory and the player keeps playing past the ticket's expiry — nothing re-validates it. So playback is not interrupted, and the app does **not** need a renewal mechanism today. The brief's real requirement — "never persist or cache video URLs or tickets" — is fully client-side and satisfiable: hold them in memory only, request a fresh ticket on every playback entry.

**Two traps to plan for, though:**

- **Proxy mode**: `POST /playback` returns `…/stream?token=…` whose transient lives **60 seconds** and is **not bound to the requesting user** (only `lesson_id` is compared). A long video dies mid-playback, and the URL is shareable within that minute. Do **not** enable proxy mode for the app; keep the CDN direct.
- **When HLS arrives**, segment/key tokens will genuinely expire mid-playback. At that point a session-renewal endpoint becomes a real requirement (§4.2). I recommend designing the Dart player behind a `PlaybackSource` abstraction now so this lands without UI changes.

### 3.7 🔴 "never decide access locally" — satisfied, but one product consequence

Access is decided 100 % server-side (see the §2.3 decision tree), and `isFreePreview` / `requiredTier` are display-only hints. The app should use them for **badges** ("پیشنمایش رایگان", "طلایی") and **never** to gate playback. ✅

The consequence: **one token slot per user** means the Android app and the web PWA cannot be signed in at the same time — whichever logs in last wins. This is fine for v1 (the app is the primary surface) but should be a conscious decision, and it means a web login silently breaks the app until the next 401.

### 3.8 ✅ Other brief statements verified

| Brief statement                                                                           | Verdict                                                                                                                                                                      |
| ----------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST /auth/request-otp` returns `isRegistered`                                           | ✅ exact key, camelCase                                                                                                                                                       |
| Opaque Bearer, ~30 days                                                                   | ✅ exactly 30 days, validated per request                                                                                                                                     |
| Normalise Persian/Arabic digits before sending                                            | ✅ required — the server's `^09[0-9]{9}$` test is ASCII-only. (Its *outbound* `phone_variants()` does handle Persian digits for lookups, but the inbound validator does not.) |
| `POST /lessons/{id}/playback {ticketId}` → `videoUrl` + optional `qualities[{label,url}]` | ✅ exact                                                                                                                                                                      |
| Videos are plain MP4, one per quality, HLS later                                          | ✅ and the `mrdastbaaf_stream_video_url` filter is already reserved for the HLS provider                                                                                      |
| Physical products showcase, opens website to buy                                          | ✅ `url` is the WooCommerce permalink                                                                                                                                         |
| Watermark from `/auth/me` **and** the ticket, not derived locally                         | ⚠️ partial — see §3.9                                                                                                                                                        |
| Payment / admin / offline / iOS / push out of scope                                       | ✅ consistent with the API                                                                                                                                                    |

### 3.9 🟠 Watermark identity is split across two sources

The ticket returns `watermarkText` = `"0912***4567 | ID: 42"` and `watermarkUserId` = `"42"`. It does **not** contain the full name. The full name is only in `/auth/me` (`fullName`).

So "full name + user ID + masked phone, not derived locally" is achievable but requires **two calls** — and `/auth/me` is not called during a free-preview playback by an anonymous user, where the ticket says `watermarkText: "مهمان | مستر دستباف"` and `watermarkUserId: "guest"`.

**Client plan:** build the watermark line from `fullName` (cached from the session, refreshed on foreground) + the ticket's `watermarkText`/`watermarkUserId`, and fall back to the ticket alone when `watermarkUserId == "guest"`. §4.4 asks for a one-field server addition that would make this a single source.

---

## 4. (b) Missing endpoints / changes needed server-side

### 4.1 🔴 BLOCKER — playback position (resume)

The brief requires *"resume position (sync with /me/progress)"* and *"resume from last position"*. **Nothing in the API or the database stores a playback position.** `GET/PUT /me/progress` handles only `completedLessonIds` and `bookmarkedLessonIds`; the `mrdastbaaf_lessons` table has no position column.

**Requested — a dedicated endpoint (preferred):**

```jsonc
// GET /lessons/{id}/progress        Auth: Bearer
// 200
{
  "lessonId": "12",
  "positionSeconds": 431.5,
  "durationSeconds": 1420,
  "completed": false,
  "updatedAt": "2026-10-16T08:00:00+00:00"   // ISO 8601
}
// 200 when never watched:  { "lessonId": "12", "positionSeconds": 0, "durationSeconds": 0,
//                            "completed": false, "updatedAt": null }

// PUT /lessons/{id}/progress        Auth: Bearer
// Request
{ "positionSeconds": 431.5, "durationSeconds": 1420 }
// 200
{ "success": true }
```

Server rules I would expect: clamp `positionSeconds` to `[0, durationSeconds]`; mark `completed` when `positionSeconds >= durationSeconds * 0.95`; ignore writes shorter than ~5 s so an accidental open does not wipe a real position; store per `(user_id, lesson_id)`.

**Alternative (less clean, no new route):** extend `PUT /me/progress` with `lessonPositions: { "12": 431.5 }` using the same replace semantics. Whichever you prefer — but it must exist.

### 4.2 🟠 Needed only when HLS lands — playback session renewal

```jsonc
// POST /lessons/{id}/session        Auth: Bearer + ticketId
{ "ticketId": "tkt_…", "deviceId": "<stable install id>" }
// 200
{
  "sessionId": "sess_…",
  "manifestUrl": "https://…/lessons/12/master.m3u8?token=…",
  "expiresAt": "2026-10-16T08:20:00+00:00",
  "watermark": { "text": "…", "userId": "42", "fullName": "…" },
  "qualities": [ { "label": "1080p", "bandwidth": 5000000, "resolution": "1920x1080", "type": "hls" } ]
}

// POST /playback/session/{sessionId}/heartbeat   → { "allowed": true, "expiresAt": "…" }
//   or { "allowed": false, "reason": "subscription_expired" | "tier_too_low" | "session_revoked" }
// DELETE /playback/session/{sessionId}           → { "success": true }
```

This is also the natural place to add **concurrency limits** and **device binding**. Not required for v1 (MP4 direct), but design the Dart layer so it can be adopted without UI changes.

### 4.3 🟠 Video type discriminator (needed for the player abstraction)

```jsonc
// Add to POST /lessons/{id}/playback — both levels
{
  "videoUrl": "…",
  "videoType": "mp4",                       // "mp4" | "hls"
  "streamType": "mp4",                      // make the ticket field truthful too
  "qualities": [
    { "label": "1080p", "url": "…", "type": "mp4" },
    { "label": "auto",  "url": "…master.m3u8", "type": "hls" }
  ]
}
```

Without this the client is forced into extension sniffing + content-type probing, which breaks in proxy mode. This is a small, additive change.

### 4.4 🟠 Watermark full name in the ticket (one field)

```jsonc
// Add to POST /lessons/{id}/playback-ticket and POST /lessons/{id}/playback
{
  "watermarkText": "0912***4567 | ID: 42",
  "watermarkUserId": "42",
  "watermarkFullName": "محمدجواد فلان"       // ← new; "مهمان" for guests
}
```

Makes identity a single server-provided source, exactly as the brief asks.

### 4.5 🟡 Optional — enforce first/last name server-side

If you want the server to be the authority rather than the client: return `422 missing_name` from `verify-otp` when `isRegistered === false` and both names are empty. **Not required** — the app can enforce it, and the existing `isRegistered` flag tells it when to ask.

### 4.6 🟡 One-line fixes worth doing while you are in there

| Fix                                                                                 | Where                                                                               |
| ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `lessonsCount` is always `0` on the detail route                                    | `serve_single_course()` — set `$course['lessonsCount'] = count($course['lessons'])` |
| `streamType` is hard-coded `"hls"` while MP4 is served                              | ticket + playback responses                                                         |
| `lesson.duration` is `null` when 0 — forces a nullable Dart field                   | `get_product_lessons_public()` — emit `0`                                           |
| `/auth/me` has no `tokenExpiresAt`, so the client cannot pre-empt a 401             | `format_user_profile()`                                                             |
| `expires_in_seconds` (snake) sits next to `isRegistered` (camel) in the same object | `request-otp` response                                                              |
| `GET /me/orders` omits `success` while every sibling includes it                    | `handle_get_orders()`                                                               |

### 4.7 Summary

| #   | Priority       | Item                              | Blocks                                            |
| --- | -------------- | --------------------------------- | ------------------------------------------------- |
| 4.1 | 🔴 **BLOCKER** | Playback position GET/PUT         | "resume position" — an explicit brief requirement |
| 4.3 | 🟠             | `videoType` / per-quality `type`  | clean MP4↔HLS player abstraction                  |
| 4.4 | 🟠             | `watermarkFullName` in the ticket | single-source watermark identity                  |
| 4.2 | 🟠             | Playback session + heartbeat      | only when HLS lands (design for it now)           |
| 4.5 | 🟡             | Server-side name enforcement      | optional; client can enforce                      |
| 4.6 | 🟡             | Six small contract fixes          | developer ergonomics                              |

**Nothing else is missing.** Auth, catalogue, detail, membership, progress lists, orders, shop, app-ui, app-pages and the two-step playback flow are all present and sufficient for v1.

---

## 5. (c) Folder structure & package list

### 5.1 Structure — clean architecture, feature-first

```
mdbapp/
├── android/app/src/main/kotlin/ir/mrdastbaaf/mdbapp/
│   ├── MainActivity.kt                  # MethodChannel: secureScreen(on/off)
│   ├── SecurityChannel.kt               # secureScreen · isRooted · isEmulator
│   ├── RootDetector.kt                  # su binaries, test-keys, Magisk paths, RW /system
│   ├── EmulatorDetector.kt              # FINGERPRINT / MODEL / HARDWARE / SENSORS
│   └── player/
│       ├── Media3PlayerView.kt          # AndroidView platform view over androidx.media3
│       ├── Media3PlayerFactory.kt       # ExoPlayer + HlsMediaSource + header-aware data source
│       └── PlayerEventChannel.kt        # position / state / tracks / error → Dart
│
├── lib/
│   ├── main.dart                        # bootstrap, DI, error zone
│   ├── app.dart                         # MaterialApp.router, theme, RTL, localization
│   │
│   ├── core/
│   │   ├── config/
│   │   │   ├── app_config.dart          # base URL from --dart-define (staging | production)
│   │   │   └── feature_flags.dart       # rootBlock, certPinning, proxyModeGuard …
│   │   ├── constants/
│   │   │   ├── api_paths.dart           # every endpoint in ONE place
│   │   │   ├── watermark_config.dart    # opacity / sizePx / moveIntervalSec / positions
│   │   │   └── security_config.dart
│   │   ├── error/
│   │   │   ├── failure.dart             # sealed: Network · Unauthorized · Forbidden · RateLimited · Server · Unknown
│   │   │   └── api_exception.dart       # maps {code, message, data.status} → Failure, keeps the Persian message
│   │   ├── network/
│   │   │   ├── dio_client.dart
│   │   │   └── interceptors/
│   │   │       ├── auth_interceptor.dart      # attach Bearer; on 401 wipe session + route to login
│   │   │       ├── retry_interceptor.dart     # GET-only, backoff on network/5xx
│   │   │       ├── logging_interceptor.dart   # debug only — NEVER tokens, tickets or video URLs
│   │   │       └── pinning_interceptor.dart   # optional, behind a flag
│   │   ├── storage/
│   │   │   ├── secure_storage.dart      # token ONLY
│   │   │   └── local_store.dart         # resume position cache, catalogue cache, flags
│   │   ├── security/
│   │   │   ├── secure_screen.dart       # Dart facade over FLAG_SECURE
│   │   │   ├── device_integrity.dart    # root/emulator facade, respects the config flag
│   │   │   └── watermark/{watermark_controller.dart, watermark_overlay.dart}
│   │   ├── router/{app_router.dart, auth_guard.dart}
│   │   ├── theme/                       # Material 3, RTL, Vazirmatn, tokens
│   │   └── utils/                       # Persian digits, Toman formatting, Jalali, phone normalisation
│   │
│   ├── l10n/{app_fa.arb, l10n.yaml}
│   │
│   ├── features/
│   │   ├── auth/          (data · domain · presentation)
│   │   ├── catalogue/
│   │   ├── course_detail/
│   │   ├── player/        (player_page · player_controller · quality_sheet ·
│   │   │                   playback_source.dart · player_platform_view.dart)
│   │   ├── membership/
│   │   ├── dashboard/
│   │   ├── shop/          (read-only showcase → external browser)
│   │   └── app_content/   (app-ui home, app-pages viewer)
│   │
│   └── shared/widgets/    # AsyncStateView (loading/empty/error/offline), buttons, cards
│
├── test/{unit, widget, fixtures}
├── analysis_options.yaml
└── pubspec.yaml
```

### 5.2 Packages

| Concern          | Package                                     | Why                                                                                                                                            |
| ---------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| State management | `flutter_riverpod` + `riverpod_annotation`  | Required by the brief; compile-safe DI, no BuildContext coupling, easy to unit-test repositories                                               |
| Networking       | `dio`                                       | Required; the interceptor chain is exactly the 401/retry design the brief specifies                                                            |
| Navigation       | `go_router`                                 | Required; `redirect` guards map cleanly onto the auth guard                                                                                    |
| Token storage    | `flutter_secure_storage`                    | Required; Android Keystore-backed                                                                                                              |
| Models           | `freezed` + `json_serializable`             | ~20 DTOs with **inconsistent envelopes and casing** — codegen keeps the mapping honest and reviewable                                          |
| Codegen          | `build_runner`                              | required by the two above                                                                                                                      |
| Localization     | `flutter_localizations` + `intl` + ARB      | Required; RTL-correct, all strings in ARB                                                                                                      |
| Jalali dates     | `shamsi_date`                               | Actively maintained, pure Dart. Note the server already pre-formats `formattedExpireDateJalali`, so this is mainly for player/order timestamps |
| Persian font     | bundled **Vazirmatn TTF** in `assets/fonts` | Same typeface as the web app; bundling avoids a runtime fetch                                                                                  |
| Connectivity     | `connectivity_plus`                         | Drives the required offline state                                                                                                              |
| Images           | `cached_network_image`                      | Course thumbnails                                                                                                                              |
| Device/app info  | `device_info_plus`, `package_info_plus`     | Device identity for logs/support; app version for diagnostics                                                                                  |
| External links   | `flutter_custom_tabs`                       | Shop product `url` and the membership renewal page open in a Custom Tab                                                                        |
| Tests            | `mocktail`, `flutter_test`                  | Repository + subscription-logic unit tests, as required                                                                                        |
| Lints            | `flutter_lints`, `custom_lint`              | Enforce the architecture                                                                                                                       |

### 5.3 Player engine — the one real technical decision

**Requirement:** Media3/ExoPlayer-based, MP4 **and** HLS, a quality selector that preserves position/play state, and (later) AES-128 HLS where the **key request carries the auth header**.

| Option                                                 | Verdict                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `video_player` (official)                              | ExoPlayer/Media3 under the hood via `video_player_android`, and it does accept `httpHeaders`. But it exposes **no track/quality-selection API** — there is no way to implement "1080p / 720p / 480p" or to select a variant from a master playlist.                                            |
| `chewie`                                               | A controls UI on top of `video_player` — inherits the same limitation. Useful later for the control bar only.                                                                                                                                                                                  |
| `media_kit`                                            | Uses libmpv, not Media3 — contradicts the brief's explicit engine requirement.                                                                                                                                                                                                                 |
| `better_player`                                        | Unmaintained.                                                                                                                                                                                                                                                                                  |
| **Custom Kotlin platform view over `androidx.media3`** | The only option that satisfies all four requirements: `TrackSelectionParameters` for manual variant selection, `DefaultHttpDataSource.Factory` with a default `Authorization` header (which covers the AES-128 key request), full playback-state control, and no third-party abandonment risk. |

**Recommendation: the platform view**, behind a Dart `PlaybackSource` / `SecurePlayerController` interface so the UI never knows which engine is underneath. This also means HLS lands later with **zero UI changes** — exactly what the brief asks for.

I will not commit to this blind: **Phase 1 opens with a one-day spike** against a real signed HLS + AES-128 stream. If the spike fails, the fallback is `video_player` for MP4 plus a narrowed quality selector, with HLS deferred.

### 5.4 Release build (brief #6)

```bash
flutter build appbundle --release --obfuscate --split-debug-info=build/debug-info
```

plus `android/app/proguard-rules.pro` with the Media3 keep rules, `isMinifyEnabled = true`, `isShrinkResources = true`, HTTPS-only via `android:usesCleartextTraffic="false"` and a `network_security_config.xml`, and certificate pinning behind a flag (dio `HttpClientAdapter` + `badCertificateCallback`).

---

## 6. (d) Phase plan

Each phase ends with a commit, a short summary of what was built, how to run it, and what remains — as the brief requires.

| Phase                              | Deliverable                                                                                                                                                                                                                                                        | Exit criteria                                                                                                                                                                | Server dependency                                                   |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| **P0 — Scaffold**                  | Flutter project, Material 3 + RTL theme, Vazirmatn, ARB wiring, strict lints, folder skeleton, `--dart-define` env config for staging/production, CI (`analyze` + `test`)                                                                                          | App builds and runs with a Persian RTL screen; `flutter analyze` clean; staging and production base URLs both selectable                                                     | none                                                                |
| **P1 — Core infra + player spike** | dio client + interceptor chain (Bearer, 401→wipe→login, GET retry with backoff), `Failure`/`ApiException` mapping every documented `WP_Error` code, secure storage, go_router + auth guard, `AsyncStateView`, `ApiPaths`. **Plus the Media3 platform-view spike.** | Interceptor unit tests pass against a mock adapter; the spike plays a signed HLS stream with a header-authenticated key request and switches quality without losing position | none                                                                |
| **P2 — Security shell**            | Kotlin `SecurityChannel`: `FLAG_SECURE` on/off, `RootDetector`, `EmulatorDetector`; Dart facades; config flag to disable blocking                                                                                                                                  | On a rooted emulator the app shows the Persian block screen; on a clean device the player blocks screenshots and shows a black recents preview                               | none                                                                |
| **P3 — Auth**                      | OTP request/verify, Persian-digit normalisation, `isRegistered`-driven login vs. signup, conditional name fields with server-matching validation, token in secure storage, session state, logout, profile                                                          | Full login against staging; wrong code / rate-limited / locked each show the correct Persian message; token survives an app restart; 401 anywhere returns to login           | none                                                                |
| **P4 — Catalogue & course detail** | `/courses` **with Bearer** (category, search), `/courses/{slug}`, `/my/courses`, `/app-ui`-driven home, `/app-pages` viewer; lesson list with free / locked / unlocked badges; `lessons.length` used instead of `lessonsCount`                                     | Live data renders; every screen has loading/empty/error/offline; `owned` is correct when signed in                                                                           | none                                                                |
| **P5 — Membership & dashboard**    | `/memberships/plans`, `/memberships/my-status`, `/me/progress` (completed + bookmarks), `/me/orders`, Jalali dates, renewal button → external browser; unit tests for tier/expiry logic                                                                            | Subscription card and countdown correct; renewal opens the site; repository tests green                                                                                      | none                                                                |
| **P6 — Shop showcase**             | `/shop/products` — `featured` slider + category chips, read-only, product tap → Custom Tab                                                                                                                                                                         | Matches the web wording and layout; no purchase flow in-app                                                                                                                  | none                                                                |
| **P7 — Player & content security** | Media3 player, ticket → playback flow, `PlaybackSource` abstraction, quality selector, watermark overlay (name + id + masked phone from the server), background pause, foreground re-check, resume from position                                                   | Video plays; watermark moves and survives fullscreen; backgrounding pauses; returning re-checks access; position resumes                                                     | **§4.1 (position) blocks resume.** Everything else works without it |
| **P8 — Hardening & release**       | R8 + obfuscation + `split-debug-info`, optional pinning, log hygiene audit (no tokens/tickets/video URLs), full test pass, signed AAB, honest README on security limits                                                                                            | Release AAB passes a device smoke test; grep of release logs shows no secrets; README documents what FLAG_SECURE and the watermark do **not** stop                           | none                                                                |

**Critical path:** P0 → P1 → P3 → P4 → P7. P2, P5 and P6 can run in parallel with P4. Only **P7's resume feature** waits on the server.

### Server track (independent)

| Item                           | Effort                                                         | Blocks                   |
| ------------------------------ | -------------------------------------------------------------- | ------------------------ |
| §4.1 playback position GET/PUT | small — one table column or a usermeta JSON key + two handlers | P7 resume                |
| §4.3 `videoType` fields        | tiny — additive                                                | clean player abstraction |
| §4.4 `watermarkFullName`       | tiny — one field                                               | single-source watermark  |
| §4.6 six small contract fixes  | tiny                                                           | developer ergonomics     |
| §4.2 session + heartbeat       | medium                                                         | only when HLS ships      |

---

## 7. Risks, and what I need from you

| #  | Item                                    | Note                                                                                                                                                                                       |
| -- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| R1 | **Player engine spike**                 | The single biggest unknown. One day in P1, before committing to the architecture.                                                                                                          |
| R2 | **One token slot per user**             | Shipping the app signs the user out of the web PWA. Accept for v1, or ask the server for a multi-device token table?                                                                       |
| R3 | **Proxy mode must stay OFF**            | Its 60-second, user-unbound token would break playback mid-video. I will add a client-side guard that refuses a `proxyMode: true` response rather than silently producing a broken player. |
| R4 | **Staging environment**                 | Is there a staging WordPress host I can point the app at, or do we develop against production? This affects the P1/P3 test loop significantly.                                             |
| R5 | **`/auth/health`**                      | Public and useful on staging, but it leaks host internals — I will not wire it into the production build. Confirm that is acceptable.                                                      |
| R6 | **Content-type fallback**               | Until §4.3 lands, the player will probe content-type once per URL. Confirm that one extra request per playback is acceptable.                                                              |
| R7 | **Persian numerals / Toman formatting** | Should the UI render `۰۱۲۳` and `۴۹۰٬۰۰۰ تومان`? I will default to Persian digits + thousands separators unless you say otherwise.                                                         |
| R8 | **Renewal destination URL**             | Which exact page should the "تمدید اشتراک" button open — `/membership` on the site?                                                                                                        |

**The only hard blocker is §4.1 (playback position).** Everything else in the brief is buildable against the API as it exists today.
