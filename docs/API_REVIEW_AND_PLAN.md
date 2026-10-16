# mdbapp — API Review & Implementation Plan

**Reviewed:** `soldiermr98-commits/mrdb` @ `main` (cloned to `D:\work-webs\0 Build\mrdb-reference`)
**Target:** native Android app for `mrdastbaaf.com` — Flutter / Dart, small Kotlin parts
**Status:** research only — **no app code written yet**

---

## 0. خلاصهٔ مدیریتی (Persian summary)

پل وردپرس (`mrdastbaaf-bridge` v1.7.13) و وب‌اپ PWA را کامل بررسی کردم. API فعلی برای **ورود، لیست دوره‌ها، جزئیات دوره، پلن‌های اشتراک، وضعیت اشتراک، پیشرفت، سفارش‌ها و پخش ویدیو** آماده است و کاملاً قابل استفاده برای اپ اندروید.

**اما سه شکاف جدی هست که بدون رفع آن‌ها در سمت سرور، نمی‌توان نیازهای امنیتی خواسته‌شده را برآورده کرد:**

1. **HLS وجود ندارد.** سرور فقط MP4 تخت می‌دهد؛ نه فایل `.m3u8`، نه کلید AES-128، نه سگمنت امضاشده. پس «لینک کوتاه‌عمر امضاشدهٔ HLS» فعلاً غیرقابل ساخت است.
2. **توکن احراز هویت نه refresh دارد و نه قابل تشخیص انقضا است** — یک توکن ۳۰ روزهٔ ثابت، و هر بار ورود، توکن دستگاه قبلی را باطل می‌کند (ورود هم‌زمان وب و اپ ممکن نیست).
3. **در حالت پیش‌فرض، API آدرس دائمی و مستقیم CDN را برمی‌گرداند** — یعنی دقیقاً همان چیزی که در نیازمندی شمارهٔ ۱ ممنوع شده است.

مستند زیر این شکاف‌ها را با شکل دقیق درخواست/پاسخ هر اندپوینت لازم فهرست کرده، ساختار پوشه و پکیج‌های فلاتر را پیشنهاد داده و فازبندی اجرا را مشخص می‌کند.

---

## 1. Repository inventory

| Path | What it is |
|---|---|
| `src/` | The existing **web app** — React 19 + Vite + Tailwind PWA (version `1.5.6`). Consumes the same API. |
| `wordpress-plugin/mrdastbaaf-bridge/` | The **WordPress plugin** that owns the API. REST namespace `mrdastbaaf/v1`. Version `1.7.13`. |
| `wordpress-plugin/mrdastbaaf-bridge/includes/class-*.php` | 13 controllers — auth, courses, memberships, payment, shop, app-ui, app-pages, admin, settings, woo-bridge, database, sms, membership-helper. |
| `docs/` | `VIDEO_SECURITY.md`, `STREAM_PROXY_DECISION.md`, `PRE_LIVE_HARDENING.md`, `PRODUCTION_INTEGRATION_PLAN.md`, `PWA_UPDATE_MECHANISM.md`, hotlink `.htaccess` samples. |
| `FullDoc.MD`, `README.md`, `CHANGELOG.md` | ~305 KB of Persian product/architecture documentation. |
| `tests/web/` | Vitest contract tests — useful as executable API documentation. |
| `scripts/encode-qualities.sh` | ffmpeg script that produces the per-quality MP4 renditions. |

**There is no existing Android/Flutter plan anywhere in the repo** — I grepped for `flutter|android|kotlin|native app`. The only Android references are about the **PWA installed via Chrome/WebAPK** (icons, launcher). So the native app is genuinely greenfield, and the web app is our behavioural reference.

**Base URL (production):** `https://mrdastbaaf.com/wp-json/mrdastbaaf/v1`
**Video host allowlist:** `dll.mrdastbaaf.com` (option `mrdastbaaf_video_hosts`)

---

## 2. (a) What the API already supports

### 2.1 Full route inventory (28 routes)

#### Public (no auth required)

| Method | Path | Purpose |
|---|---|---|
| GET | `/version` | PWA/plugin/db versions + `content_hash`. Cache-busted by client. |
| GET | `/auth/health` | OTP-chain diagnostics. Rate-limited 1/10s per IP. |
| POST | `/auth/request-otp` | Send OTP SMS. |
| POST | `/auth/verify-otp` | Verify OTP → issues Bearer token + user profile. |
| POST | `/auth/otp-dispatch` | Internal async SMS dispatcher (loopback/cron). |
| GET | `/app-ui` | Server-driven home screen: hero copy, section toggles/order, dock + guide menu, `featuredCourseIds`. |
| GET | `/app-pages` | Editorial pages list, **filtered by access level** (guest / member / admin). |
| GET | `/app-pages/{id}/content` | Rendered HTML of one editorial page. |
| GET | `/shop/products` | Physical-product feed: `{featured, categories}`. |
| GET | `/courses?category=&search=` | Course catalogue. Bare array, max 50. |
| GET | `/courses/by-id/{id}` | Course detail by WooCommerce product id. |
| GET | `/courses/{slug}` | Course detail by slug (multi-form percent-decode lookup). |
| GET | `/memberships/plans` | Active membership plans. Bare array. Cached 5 min. |
| POST | `/lessons/{id}/playback-ticket` | **Step 1 of playback.** Issues a 15-min ticket + watermark. |
| POST | `/lessons/{id}/playback` | **Step 2.** Exchanges the ticket for `videoUrl` + qualities. |
| GET | `/lessons/{id}/stream?token=` | PHP stream proxy (Range-capable) — only when proxy mode is ON. |
| POST | `/payment/zibal/verify` | Gateway callback. Guarded by HMAC `cb_token` (TTL 48 h), **not** for the app to call directly. |

#### Authenticated (`Authorization: Bearer {user_id}.{secret}`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/auth/me` | Current profile + membership. |
| POST | `/auth/logout` | Revokes the Bearer token. |
| GET | `/my/courses` | Purchased courses for the current user. |
| GET | `/memberships/my-status` | Active membership or `{hasActiveMembership:false}`. |
| GET | `/me/progress` | `{completedLessonIds[], bookmarkedLessonIds[]}`. |
| PUT | `/me/progress` | **Replace** semantics per supplied array. |
| GET | `/me/orders` | Last 50 orders. |
| POST | `/payment/zibal/initiate` | Creates an order → `paymentUrl`. |
| POST | `/payment/manual-transfer/submit` | Card-to-card receipt submission. |

#### Admin-only (`Bearer` + `manage_options`) — not needed by the app

`GET /admin/analytics`, `GET /admin/receipts`, `POST /admin/receipts/{id}/review`, `PUT /admin/plans/{id}`, `GET /admin/homepage-sections`, `GET /admin/lessons`, `POST /admin/lessons`, `PUT /admin/lessons/{id}`.

### 2.2 Auth model — exactly as implemented

```
POST /auth/request-otp   { phone }
  → 200 { success, message, expires_in_seconds, isRegistered, smsWarning? }
  phone normalised: +98 / 0098 → 0…, must match ^09\d{9}$
  limits: 2 min/phone · 5/h/phone · 15/day/phone · 10/h/IP
  errors: invalid_phone 400 · role_not_allowed 403 · rate_limited 429 · otp_storage_failed 500

POST /auth/verify-otp    { phone, otp_code, first_name?, last_name? }
  → 200 { success, token, user }
  token  = "{user_id}.{secret}"          ← opaque, NOT a JWT
  server stores only sha256(secret) in usermeta
  lifetime = 30 days, checked on every request
  max 5 wrong attempts per issued code
  errors: invalid_otp 400 · otp_locked 429 · registration_conflict 409 · role_not_allowed 403

GET /auth/me  →  { id, phoneNumber, maskedPhone, fullName, role, membership }
POST /auth/logout → deletes both token usermeta keys
```

Important behavioural facts the Android app must be built around:

- **Opaque token, not JWT** — the app cannot read its own expiry. There is no `expiresAt` in any response.
- **No refresh endpoint.** Recovery from 401 is a full OTP re-login.
- **One token per user.** `verify-otp` calls `update_user_meta(… 'mrdastbaaf_api_token', …)` — a single slot. **Logging in on the phone silently logs the user out of the web PWA** (and vice versa), and logging in on a second phone kills the first.
- OTP delivery is **asynchronous** (separate PHP process + cron sweep). The `request-otp` response returns before the SMS is actually sent; the app must not treat "no SMS yet" as failure.
- Role guard: only `subscriber` / `customer` may log in. Admins get `403 role_not_allowed`.
- `verify-otp` **auto-registers** unknown phones as `subscriber` with `user_login = phone`.

### 2.3 Membership & entitlement — server is already the source of truth

Access is decided **entirely server-side** in `user_has_lesson_access()`:

```
1. admin (manage_options)                                   → allow
2. owns the course product in a paid WooCommerce order      → allow
   (statuses: wc-completed, wc-processing; matched by
    customer_id OR guest billing_phone in all normalised forms)
3. active membership with tier_rank >= lesson.required_tier → allow
   bronze = 1  <  silver = 2  <  gold = 3
4. otherwise                                                → 403
```

- `GET /memberships/plans` → `[{ id, tier, title, description, priceToman, originalPriceToman, duration, durationLabel, features[], isPopular, isActive, colorScheme }]`
- `GET /memberships/my-status` → `{ hasActiveMembership, membership: { tier, planId, status, startedAt, expiresAt, formattedExpireDateJalali, daysRemaining } | null }`
- `GET /courses` returns `owned: true` per course **only if the Bearer token is attached** (the route is public; auth is optional and computed internally).

**This satisfies security requirement #7 as-is.** The app never needs to decide access locally — it calls the ticket endpoint and interprets 401/403.

### 2.4 Video pipeline as it exists today

```
POST /lessons/{id}/playback-ticket          (public route, Bearer optional)
  → 200 {
      ticketId, lessonId,
      expiresInSeconds: 900,                 ← 15 minutes
      issuedAt, expiresAt,                   ← ISO 8601
      watermarkText:  "0912***4567 | ID: 42",
      watermarkUserId: "42",
      streamType: "hls"                      ← label only; no HLS is served
    }
  rate limit: 30 tickets/hour per (IP + user + lesson)
  errors: unauthorized 401 (not logged in, paid lesson)
          forbidden 403 (no access)
          rate_limited 429

POST /lessons/{id}/playback   { ticketId }
  ticket must match BOTH the current user and the lesson (fail-closed)
  → proxyMode OFF (DEFAULT):
      { videoUrl: "https://dll.mrdastbaaf.com/.../lesson-01.mp4",   ← PERMANENT
        watermarkText, expiresAt,
        qualities: [ { label: "1080p", url: "https://dll.../lesson-01-1080.mp4" }, … ],
        proxyMode: false }
  → proxyMode ON:
      { videoUrl: "https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/lessons/12/stream?token=XXXX",
        qualities: [ { label, url: "…/stream?token=YYYY" } ],
        proxyMode: true }

GET /lessons/{id}/stream?token=XXXX
  60-second transient token · Range supported (206 / 416) · no-store
  validates: token exists · lesson matches · host is https + allowlisted
  does NOT validate the requesting user
```

**What exists and is genuinely good:**
- Two-step ticket exchange with a 15-minute TTL — real, implemented, and reusable.
- Server-generated watermark string (`masked_phone | ID: user_id`), masked server-side so the raw phone never leaves.
- Video-host allowlist + `https` enforcement.
- Ticket rate limiting.
- A Range-capable PHP proxy with a `no-store` policy — already built and shipped (v1.7.0/1.7.1).
- A **provider hook** (`mrdastbaaf_stream_video_url`) explicitly reserved so a third-party plugin can rewrite the URL into signed HLS **without touching core plugin code**.

**What the docs claim vs. reality:**

| Layer | `VIDEO_SECURITY.md` | Reality |
|---|---|---|
| 15-min playback ticket | implemented | ✅ implemented |
| Per-user watermark | implemented | ⚠️ server returns the **text**; rendering is client-side only |
| Video host allowlist | implemented | ✅ implemented |
| Ticket rate limit | implemented | ✅ implemented |
| Hotlink protection (`.htaccess`) | implemented | ⚠️ shipped as a **sample file**, must be installed on `dll.` host; Referer-spoofable |
| PHP streaming proxy | "for the future" | ✅ **actually shipped** in v1.7.0, but **OFF by default** |
| **HLS + signed segments** | planned | ❌ **not implemented at all** |
| DRM | out of scope | ❌ not implemented |

---

## 3. (b) Missing endpoints & server-side changes

Ordered by priority. **Items in P0 are hard blockers** — without them, requirements #1, #2 and the whole player section of the brief cannot be met.

### P0-1 — Token refresh + expiry metadata

**Problem:** opaque 30-day token, no refresh, no way for the client to know when it expires. The brief explicitly asks for a dio interceptor that handles "auth token + refresh".

**Add `POST /auth/refresh`** — auth: none (the refresh token is the credential).

```jsonc
// Request
{ "refreshToken": "<opaque string>", "deviceId": "<stable per-install uuid>" }

// 200
{
  "accessToken":  "<short-lived, 15 min>",
  "refreshToken": "<rotated on every use, 90 days>",
  "tokenType":    "Bearer",
  "expiresIn":    900,
  "expiresAt":    "2026-10-16T12:45:00+00:00"
}

// Errors
401 invalid_refresh_token   // unknown / malformed
401 refresh_expired         // past 90 days → full re-login
401 refresh_reused          // token replay → revoke the whole family
403 role_not_allowed
```

**And extend `POST /auth/verify-otp`** to return the same pair alongside the existing `token`:

```jsonc
{
  "success": true,
  "token": "<unchanged legacy opaque token — keeps the web PWA working>",
  "accessToken":  "…",
  "refreshToken": "…",
  "expiresIn":    900,
  "expiresAt":    "2026-10-16T12:45:00+00:00",
  "user": { … }
}
```

> Keeping `token` is mandatory so the shipped PWA is not broken. The app uses only the pair.

### P0-2 — Multi-device sessions

**Problem:** one token slot per user. Phone login kicks the web session out; two phones cannot coexist. Also there is no device binding for the stream token, so a leaked stream URL is usable by anyone.

**Add a token table** replacing the `mrdastbaaf_api_token` usermeta slot:

```
wp_mrdastbaaf_api_tokens
  id · user_id · token_hash · device_id · device_name · platform
  app_version · issued_at · last_used_at · expires_at · revoked_at
```

**Change `POST /auth/verify-otp`** to accept optional `deviceId`, `deviceName`, `platform`, `appVersion`, and upsert a row per device instead of overwriting a single meta key.

**Add:**

| Method | Path | Response |
|---|---|---|
| GET | `/auth/sessions` | `{ sessions: [ { id, deviceName, platform, appVersion, createdAt, lastSeenAt, current } ] }` |
| DELETE | `/auth/sessions/{id}` | `{ success: true }` |
| DELETE | `/auth/sessions` | revoke all **except** the current one |

**And change `POST /auth/logout`** to accept `{ deviceId }` and revoke only that device.

### P0-3 — HLS packaging + manifest + AES-128 key

**Problem:** no HLS exists. The brief requires short-lived signed `.m3u8`, adaptive bitrate, manual quality selection from the master playlist variants, and AES-128 with an authenticated key request.

**Server work (outside the API):** run the existing `scripts/encode-qualities.sh` output through an HLS packaging step to produce, per lesson, a master playlist + per-variant playlists + `.ts`/`.m4s` segments, AES-128 encrypted, on the `dll.` host.

**Add `GET /lessons/{id}/manifest`** — auth: `Bearer` **+** a valid `ticketId`.

```jsonc
// Request: ?ticketId=tkt_xxx      Header: Authorization: Bearer …
// 200  Content-Type: application/vnd.apple.mpegurl
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,NAME="1080p"
/v1/lessons/12/variant.m3u8?q=1080p&token=<hmac>&exp=1750000000
#EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720,NAME="720p"
/v1/lessons/12/variant.m3u8?q=720p&token=<hmac>&exp=1750000000
#EXT-X-STREAM-INF:BANDWIDTH=1400000,RESOLUTION=854x480,NAME="480p"
…

// Errors: 401 unauthorized · 403 forbidden / invalid_ticket · 404 no_video · 429 rate_limited
```

**Add `GET /lessons/{id}/key`** — auth: `Bearer` **required** (this is what makes AES-128 non-shareable).

```jsonc
// Request: ?token=<hmac>
// 200  Content-Type: application/octet-stream   Cache-Control: no-store
//      <16 raw bytes>
// Errors: 403 invalid_key_token · 403 token_expired
```

**Token requirements for both:**
- `token = HMAC_SHA256(user_id + device_id + lesson_id + quality + expires, server_secret)`
- TTL **5–15 minutes**, renewable via the heartbeat in P0-4 (a 60-second TTL will break a long video).
- Segment URLs signed the same way; the segment TTL should be long enough that a continuous playback session never hits a 403 mid-stream.
- **The key and manifest requests must accept the `Authorization` header** so ExoPlayer can be configured to send it — confirm this survives the host's LiteSpeed/BitNinja layer.

### P0-4 — Playback session + heartbeat (revalidation & concurrency)

**Problem:** nothing lets the app re-validate the subscription on foreground, and nothing lets the server limit concurrent streams.

**Add:**

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/lessons/{id}/session` | `{ ticketId, deviceId, quality? }` | `{ sessionId, manifestUrl, expiresAt, watermark: { text, userId, opacity, sizePx, moveIntervalSec, positions[] }, qualities: [{label, bandwidth, resolution}] }` |
| POST | `/playback/session/{sessionId}/heartbeat` | `{}` | `{ allowed: true, expiresAt }` or `{ allowed: false, reason: "subscription_expired" \| "tier_too_low" \| "session_revoked" \| "too_many_streams" }` |
| DELETE | `/playback/session/{sessionId}` | — | `{ success: true }` |

This single endpoint set covers brief requirements **#4** (pause + re-validate on foreground) and gives the server the hook for **concurrency limits** and **session revocation**.

### P1-5 — Playback position sync (resume)

**Problem:** `/me/progress` stores only `completedLessonIds` and `bookmarkedLessonIds`. The brief wants "resume from last position (position synced to the server if the API supports it)". It does not.

**Add:**

```jsonc
// GET /lessons/{id}/progress
{ "positionSeconds": 431.5, "durationSeconds": 1420, "completed": false, "updatedAt": "2026-10-16T08:00:00+00:00" }

// PUT /lessons/{id}/progress      { "positionSeconds": 431.5, "durationSeconds": 1420 }
// 200 { "success": true }
```

*Alternative:* extend `PUT /me/progress` with `lessonPositions: { "<lessonId>": 431.5 }` so one endpoint handles everything.

### P1-6 — Entitlement pre-check (cheap gate before opening the player)

**Problem:** today the only way to know whether a lesson is playable is to call `playback-ticket` and interpret 401/403. That works, but it burns a rate-limited ticket and cannot explain *why* access was denied.

```jsonc
// GET /lessons/{id}/entitlement       (Bearer optional)
{
  "canPlay": false,
  "reason": "tier_too_low",            // ok | login_required | no_membership | tier_too_low | not_purchased | lesson_not_found
  "requiredTier": "gold",
  "userTier": "bronze",
  "isFreePreview": false,
  "owned": false
}
```

Lets the app render a proper Persian message ("این جلسه نیازمند اشتراک طلایی است") instead of a generic error.

### P1-7 — Payment status polling

**Problem:** the app opens `paymentUrl` in a Custom Tab; when the user returns there is no endpoint to check that specific order. `/me/orders` works but returns 50 rows and has no "just this one" semantics.

```jsonc
// GET /payment/order/{orderId}
{
  "orderId": "ord_xxx", "orderNumber": "MD-12345",
  "status": "completed",              // pending | processing | verifying | completed | failed | rejected | cancelled
  "amountToman": 490000, "itemTitle": "اشتراک طلایی ۱۲ ماهه",
  "verifiedAt": "2026-10-16T08:12:00+00:00", "refNumber": "1234567890",
  "message": "پرداخت با موفقیت تایید شد."
}
```

### P2-8 — App config / force-update endpoint

`GET /version` returns `pwa_version`, `plugin_version`, `db_version`, `content_hash` — useful, but it has no notion of a *minimum* app version.

```jsonc
// GET /app-config        (public)
{
  "minAndroidVersion": "1.0.0",
  "latestAndroidVersion": "1.1.0",
  "forceUpdate": false,
  "updateUrl": "https://play.google.com/store/apps/details?id=…",
  "supportPhone": "0912…", "supportUrl": "https://mrdastbaaf.com/…",
  "features": { "rootBlock": true, "certPinning": false, "watermark": true, "shop": false },
  "watermark": { "opacity": 0.35, "sizePx": 14, "moveIntervalSec": 7 }
}
```

Server-driven feature flags mean the team can disable root-blocking or pinning without shipping a new APK.

### P2-9 — Contract consistency fixes

| Issue | Detail | Requested change |
|---|---|---|
| Inconsistent envelopes | `/courses`, `/memberships/plans`, `/app-pages` return **bare arrays**; `/my/courses`, `/me/orders` return **objects** | Keep as-is but document; or wrap everything in `{ success, data }` with a versioned migration |
| `lesson.duration` can be `null` | `get_product_lessons_public()` emits `null` when 0 | Always emit an int (`0`) so the Dart DTO can be non-nullable |
| `/auth/me` is thin | Returns only `id, phoneNumber, maskedPhone, fullName, role, membership` — no `completedLessonIds`, `bookmarkedLessonIds`, `avatarUrl`, `createdAt`, even though the web `UserProfile` type declares them | Add `completedLessonIds` + `bookmarkedLessonIds`, or document that `/me/progress` is the sole source |
| `/auth/me` has no expiry | Client cannot pre-empt 401 | Include `tokenExpiresAt` |
| `streamType: "hls"` is a lie today | The ticket says `hls` but an MP4 is served | Either make it truthful once P0-3 lands, or rename to `streamType: "mp4"` until then |

### P2-10 — Things the app must NOT call

- `POST /payment/zibal/verify` — server-to-server callback only.
- `GET /cron/reconcile` — shared secret travels in a query string; never ship it in an app.
- `POST /auth/otp-dispatch` — internal loopback trigger.
- All `/admin/*` routes.

### Summary table

| # | Priority | Endpoint / change | Blocks which brief requirement |
|---|---|---|---|
| P0-1 | 🔴 | `POST /auth/refresh` + token pair on login | dio refresh interceptor |
| P0-2 | 🔴 | Multi-device token table + `/auth/sessions` | usable app alongside the web PWA |
| P0-3 | 🔴 | HLS packaging + `/lessons/{id}/manifest` + `/lessons/{id}/key` | **#1** (no downloadable video / signed short-lived HLS) |
| P0-4 | 🔴 | `/lessons/{id}/session` + `/playback/session/{id}/heartbeat` | **#4** (foreground revalidation), concurrency |
| P1-5 | 🟠 | `GET/PUT /lessons/{id}/progress` | resume from last position |
| P1-6 | 🟠 | `GET /lessons/{id}/entitlement` | clear Persian denial messages |
| P1-7 | 🟠 | `GET /payment/order/{orderId}` | post-payment confirmation |
| P2-8 | 🟡 | `GET /app-config` | force update, remote feature flags |
| P2-9 | 🟡 | DTO/envelope consistency | cleaner Dart models |

**Not needed from the server (already solved):** subscription is already the source of truth (#7), the watermark text already comes from the server (#3), the ticket flow already exists, and CORS is irrelevant for a native client (no `Origin` header is sent).

---

## 4. (c) Proposed Flutter structure & packages

### 4.1 Folder structure — clean architecture, feature-first

```
mdbapp/
├── android/
│   └── app/src/main/kotlin/ir/mrdastbaaf/mdbapp/
│       ├── MainActivity.kt              # MethodChannel: FLAG_SECURE on/off
│       ├── SecurityChannel.kt           # secureScreen, rootDetect, emulatorDetect
│       ├── RootDetector.kt              # su binaries, test-keys, RW /system, Magisk paths
│       ├── EmulatorDetector.kt          # FINGERPRINT / MODEL / HARDWARE / sensors
│       └── player/
│           ├── Media3PlayerView.kt      # AndroidView platform view over androidx.media3
│           ├── Media3PlayerFactory.kt   # ExoPlayer + HlsMediaSource + DefaultHttpDataSource
│           └── PlayerEventChannel.kt    # position / state / quality / error events → Dart
│
├── lib/
│   ├── main.dart                        # bootstrap, DI container, error zone
│   ├── app.dart                         # MaterialApp.router + theme + RTL + localization
│   │
│   ├── core/
│   │   ├── config/
│   │   │   ├── app_config.dart          # base URL, timeouts, build flavour
│   │   │   └── feature_flags.dart       # from GET /app-config + compile-time defaults
│   │   ├── constants/
│   │   │   ├── api_paths.dart           # every endpoint in ONE place
│   │   │   ├── watermark_config.dart    # opacity / sizePx / moveIntervalSec / positions
│   │   │   └── security_config.dart     # rootBlock, certPinning, pin list
│   │   ├── error/
│   │   │   ├── failure.dart             # sealed: Network / Unauthorized / Forbidden / Server / Unknown
│   │   │   └── api_exception.dart       # maps the plugin's {code, message, data.status}
│   │   ├── network/
│   │   │   ├── dio_client.dart          # Dio + BaseOptions + adapter
│   │   │   ├── interceptors/
│   │   │   │   ├── auth_interceptor.dart        # attach access token, 401 → refresh → retry once
│   │   │   │   ├── cache_bust_interceptor.dart  # ?_=<epochMs> on GET (LiteSpeed workaround)
│   │   │   │   ├── retry_interceptor.dart       # GET only, 3 tries, on 502/503/504 + network
│   │   │   │   ├── logging_interceptor.dart     # DEBUG only — NEVER logs tokens or video URLs
│   │   │   │   └── pinning_interceptor.dart     # optional, behind a flag
│   │   │   └── api_result.dart          # Result<T> / Either
│   │   ├── storage/
│   │   │   ├── secure_storage.dart      # flutter_secure_storage — tokens ONLY
│   │   │   └── local_store.dart         # prefs: last position, catalogue cache, flags
│   │   ├── security/
│   │   │   ├── secure_screen.dart       # Dart facade over the FLAG_SECURE MethodChannel
│   │   │   ├── device_integrity.dart    # root + emulator facade, respects the config flag
│   │   │   └── watermark/
│   │   │       ├── watermark_controller.dart   # random position on a timer
│   │   │       └── watermark_overlay.dart      # IgnorePointer + RepaintBoundary
│   │   ├── router/
│   │   │   ├── app_router.dart          # go_router, typed routes
│   │   │   └── auth_guard.dart          # redirect: signed-out → login
│   │   ├── theme/                       # Material 3, RTL, Vazirmatn, design tokens
│   │   └── utils/                       # Jalali dates, Toman formatting, Persian digits
│   │
│   ├── l10n/
│   │   ├── app_fa.arb                   # ALL user-facing strings
│   │   └── l10n.yaml
│   │
│   ├── features/
│   │   ├── auth/
│   │   │   ├── data/      (auth_api.dart, auth_repository_impl.dart, dto/)
│   │   │   ├── domain/    (entities/, repositories/, usecases/)
│   │   │   └── presentation/ (login_page.dart, otp_page.dart, auth_controller.dart)
│   │   ├── catalogue/     (list, category filter, search)
│   │   ├── course_detail/ (sections, lessons, free-preview badges, owned state)
│   │   ├── player/        (player_page.dart, player_controller.dart, quality_sheet.dart,
│   │   │                   player_platform_view.dart, playback_session_repository.dart)
│   │   ├── membership/    (plans, status, purchase handoff to Custom Tab)
│   │   ├── dashboard/     (profile, my courses, orders, progress)
│   │   ├── app_content/   (app-ui driven home, app-pages editorial viewer, shop teaser)
│   │   └── settings/      (support, about, version, logout)
│   │
│   └── shared/
│       ├── widgets/       (AsyncStateView — loading/empty/error/offline, AppButton, …)
│       └── extensions/
│
├── test/
│   ├── unit/              (repositories, subscription logic, watermark positioning, error mapping)
│   ├── widget/            (login, catalogue, player controls)
│   └── fixtures/          (captured JSON responses from the real API)
├── l10n.yaml
├── analysis_options.yaml  # strict lints
└── pubspec.yaml
```

### 4.2 Package list

| Concern | Package | Why this one | Alternative considered |
|---|---|---|---|
| State management | `flutter_riverpod` + `riverpod_annotation` | Compile-safe DI, testable, no BuildContext coupling | `flutter_bloc` — more ceremony; Riverpod matches the brief's first choice |
| Networking | `dio` | Interceptor chain is exactly what the brief asks for | `http` — no interceptor model |
| Navigation | `go_router` | Declarative, redirect guards for auth, deep links | `auto_route` |
| Token storage | `flutter_secure_storage` | Android Keystore-backed; the brief mandates it | — |
| Models | `freezed` + `json_serializable` | Immutable DTOs, `copyWith`, exhaustive unions | hand-written (error-prone at 28 endpoints) |
| Codegen | `build_runner` | required by the two above | — |
| Localization | `flutter_localizations` + `intl` + ARB | Official, RTL-correct, satisfies "all strings in ARB" | `easy_localization` |
| Jalali dates | `shamsi_date` | Actively maintained, pure Dart, formats/parses Jalali | `jalali_date` (heavier, pulls a date picker UI) |
| Persian font | bundled `Vazirmatn` TTF in `assets/fonts` | The web app already uses Vazirmatn; bundling avoids a runtime fetch and guarantees identical rendering | `google_fonts` (network dependency, licensing churn) |
| Player | **custom Kotlin platform view over `androidx.media3`** | The only way to get all of: HLS, AES-128 with an `Authorization` header on the key request, manual variant selection from the master playlist, and adaptive bitrate | `video_player` (ExoPlayer-backed but exposes **no** quality-selection API and no per-request header control); `media_kit` (libmpv, not Media3 — contradicts the brief); `better_player` (unmaintained) |
| Player fallback | `video_player` | Keep it behind the same Dart interface for the simple MP4 case while HLS is being built server-side | — |
| Root / emulator detection | **custom Kotlin** (`RootDetector.kt`, `EmulatorDetector.kt`) + Dart facade | The brief says "do not use deprecated packages"; `flutter_jailbreak_detection` is effectively unmaintained. Native checks are stronger anyway. | `safe_device` |
| Device attestation (optional, later) | Google **Play Integrity API** via MethodChannel | Server-verifiable, far stronger than local root checks | SafetyNet (deprecated) |
| Screen capture blocking | **custom Kotlin** `FLAG_SECURE` MethodChannel | The brief explicitly requires this approach | `screen_protector` (adds a dependency for one flag) |
| Connectivity | `connectivity_plus` | Drives the offline state required on every screen | — |
| Images | `cached_network_image` | Course thumbnails | `flutter_cache_manager` |
| App metadata | `package_info_plus`, `device_info_plus` | `appVersion` for `/auth/verify-otp`, `deviceName` for `/auth/sessions` | — |
| External links | `flutter_custom_tabs` | Zibal payment page in a Chrome Custom Tab | `url_launcher` (leaves the app entirely) |
| Tests | `mocktail`, `flutter_test` | Repositories + subscription logic as required | `mockito` (needs codegen) |
| Lints | `flutter_lints` + `custom_lint` (riverpod) | Enforce the architecture | — |

**Release build flags (brief #6):**

```bash
flutter build appbundle --release \
  --obfuscate \
  --split-debug-info=build/debug-info
```

plus `android/app/proguard-rules.pro` with the Media3 / HLS keep rules, `isMinifyEnabled = true`, `isShrinkResources = true`.

---

## 5. (d) Phase plan

Two tracks run in parallel: **Track A (Flutter app)** and **Track B (server)**. Track A can start immediately and reach Phase 4 without any server change. Phases 5+ are gated on Track B P0 items.

### Track A — Flutter

| Phase | Deliverable | Exit criteria |
|---|---|---|
| **A0 — Scaffold** | Flutter project, `main.dart`, Material 3 + RTL theme, Vazirmatn, ARB wiring, strict lints, folder skeleton, CI (`analyze` + `test`), first commit | App builds and runs with a Persian RTL "hello" screen; `flutter analyze` clean |
| **A1 — Core infrastructure** | dio client + all interceptors, `Result`/`Failure` model, secure storage, go_router + auth guard, feature flags, `ApiPaths`, `AsyncStateView` (loading/empty/error/offline) | Unit tests for the interceptor chain (401→refresh→retry, retry-on-502, cache-bust) pass against a mock adapter |
| **A2 — Security shell** | Kotlin `SecurityChannel`: `FLAG_SECURE` on/off; `RootDetector`; `EmulatorDetector`; Dart facades; config flag to switch blocking off | On a rooted emulator the app shows the Persian block screen; on a clean device the player screen blocks screenshots and shows a black recents preview |
| **A3 — Auth** | OTP request/verify screens, token persistence, session state, logout, profile; error mapping for every documented `WP_Error` code | Full login against the real staging API; wrong code, rate-limit and locked states each show the correct Persian message; token survives an app restart |
| **A4 — Catalogue & course detail** | `/courses` (category + search), `/courses/{slug}`, `/my/courses`, `/app-ui`-driven home, `/app-pages` viewer | Catalogue, search, free-preview badges and `owned` state all render from live data; every screen has loading/empty/error/offline |
| **A5 — Membership & dashboard** | `/memberships/plans`, `/memberships/my-status`, `/me/progress`, `/me/orders`, Jalali dates | Subscription card, expiry countdown and order history correct; unit tests for the tier/expiry logic |
| **A6 — Player & content security** | Media3 platform view, playback ticket → session → manifest, quality selector, watermark overlay, background pause, foreground revalidation, last-position resume | Video plays via signed HLS; watermark moves and survives fullscreen; backgrounding pauses; returning to foreground re-validates; position resumes |
| **A7 — Payment** | Zibal handoff via Custom Tab + order-status polling, manual-transfer form | A real test payment completes and the membership appears in the dashboard |
| **A8 — Hardening & release** | R8 + obfuscation + `split-debug-info`, optional certificate pinning, error/telemetry hygiene, full test pass, signed AAB | Release AAB passes a device smoke test; no token or video URL appears in any log |

### Track B — Server (WordPress plugin)

| Phase | Deliverable | Blocks |
|---|---|---|
| **B0** | Confirm/install the hotlink `.htaccess` on the `dll.` host; verify `Authorization` survives to the plugin on `manifest`/`key` | A6 |
| **B1** | Token table + `POST /auth/refresh` + token pair on login + `/auth/sessions` | A3 (partial), A6 |
| **B2** | HLS packaging pipeline (ffmpeg → master + variants + AES-128 segments), `GET /lessons/{id}/manifest`, `GET /lessons/{id}/key` with signed short-lived tokens | A6 |
| **B3** | `POST /lessons/{id}/session`, heartbeat, `DELETE …/session`; bind stream tokens to `user_id` + `device_id` | A6 |
| **B4** | `GET/PUT /lessons/{id}/progress`, `GET /lessons/{id}/entitlement`, `GET /payment/order/{orderId}`, `GET /app-config` | A5, A7 |

### Suggested order of work

1. **A0 → A1 → A2** (no server dependency, highest risk retired early: RTL/theme, interceptor chain, FLAG_SECURE).
2. **A3 → A4 → A5** while Track B builds **B1**.
3. **B2/B3 spike in parallel with A6**, because the Media3 + AES-128 + custom-header path is the single biggest technical unknown — I want a throwaway proof-of-concept before committing to the player architecture.
4. **A6 → A7 → A8.**

### Risks & decisions needed

| # | Risk / decision | Notes |
|---|---|---|
| R1 | **Player engine** — Media3 platform view vs. a package | I recommend the platform view. Needs a 1-day spike against a real HLS + AES-128 stream before we commit. |
| R2 | **HLS is a server project, not an app project** | ffmpeg packaging, storage, and a CDN that can serve signed segment URLs. This is the critical path for security requirement #1. |
| R3 | **60-second stream token vs. long playback** | As implemented, the proxy token dies 60 s into a video. Any server-side proxy design must use a renewable, device-bound session token instead. |
| R4 | **Host limits** | The proxy sets `memory_limit=512M` per stream on shared hosting. With HLS we should serve segments from the CDN and keep PHP out of the byte path. |
| R5 | **Two clients, one token slot** | Until B1 lands, shipping the app will log web users out. B1 is a **hard prerequisite** for release, not a nice-to-have. |
| R6 | **Persian numeral / Jalali rendering** | Decide early whether to render Persian digits (`۰۱۲۳`) in the UI and how to format Toman (`۴۹۰٬۰۰۰ تومان`). |
| R7 | **Subscription purchase stays on the web** | The app only hands off to the gateway. Confirm whether `paymentUrl` opens acceptably in a Custom Tab on Android, and what the return deep link is. |

---

## 6. Open questions for the product owner

1. Is the target host a **staging** environment I can point the app at during development, or do we test against production?
2. **Which lessons get HLS?** All of them, or only the premium tiers (the docs suggest a hybrid per-lesson mode was deliberately left unbuilt)?
3. Do we want **concurrency limiting** (one active stream per account) — and if so, what should happen to the second device?
4. Is **certificate pinning** required for the first release, or acceptable behind a flag that is off initially?
5. Should the app show the **physical shop** in v1, or keep it to courses + membership as the brief's "out of scope" implies?
6. Who owns the **server work in Track B** — is it me, or a separate backend developer? Several items (B1 especially) block the app release.
