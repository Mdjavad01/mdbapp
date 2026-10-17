# mdbapp

Native Android client for **mrdastbaaf.com** — the Persian handicraft marketplace and online-course platform.

Kotlin · Jetpack Compose · Material 3 · Media3 · Hilt · Retrofit/OkHttp · kotlinx.serialization

> **Phase P0 — scaffold.** The project builds a runnable app with the design system, RTL/Persian foundation, configuration, security shell and network stack in place. Product screens arrive from P3 onward. See [Roadmap](#roadmap).

---

## What this app is

The backend is an existing, live WordPress installation running the **`mrdastbaaf-bridge`** plugin (v1.7.15), which exposes a REST API under `mrdastbaaf/v1`. The app is a **consumer** of that API and nothing more:

- It never invents an endpoint. Every path in `core/constants/ApiPaths.kt` maps to a route the plugin registers.
- It **never decides access locally**. Whether a lesson can be played is answered by `POST /lessons/{id}/playback-ticket`, which returns 401/403 with a Persian message the app displays verbatim.
- Subscriptions are **sold on the website**, not in the app. Renewal opens `https://app.mrdastbaaf.com/#/membership` in an external browser.

The sibling repository **`mrdb`** holds the React PWA and the plugin. It is the source of truth for every request/response shape; the full contract review lives in [`docs/API_REVIEW_AND_PLAN.md`](docs/API_REVIEW_AND_PLAN.md).

---

## Requirements

| | |
|---|---|
| JDK | **17** (the toolchain is pinned to 17 via `compileOptions` / `jvmTarget`) |
| Android SDK | **Platform 35**, Build-Tools 35.x |
| Gradle | 8.9 — provided by the wrapper, no local install needed |
| Android Studio | Ladybug (2024.2) or newer recommended |

`local.properties` must point at your SDK:

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
```

---

## Build flavours

The base URL is compiled in per flavour, so a release build can never accidentally point at a debug host.

| Flavour | Application ID | Base URL | HTTP logging | Strict security |
|---|---|---|---|---|
| `dev` | `com.mrdastbaaf.mdbapp.dev.debug` | production API | ✅ | ❌ (so emulators work) |
| `staging` | `com.mrdastbaaf.mdbapp.staging.debug` | *staging host — placeholder* | ✅ | ✅ |
| `prod` | `com.mrdastbaaf.mdbapp` | production API | ❌ | ✅ |

> ⚠️ **`staging` is not usable yet.** The staging subdomain has not been provisioned, so the flavour points at a placeholder host and every request will fail. Until it exists, develop against `devDebug`, which targets the production API with logging on — read-only endpoints and the designated test account only.

### Commands

```bash
./gradlew assembleDevDebug          # build
./gradlew installDevDebug           # build + install on a connected device
./gradlew test                      # unit tests (fast, no emulator)
./gradlew lint                      # Android Lint — fails the build on errors
./gradlew :app:dependencies         # inspect the dependency tree
```

### Release build

```bash
./gradlew bundleProdRelease --obfuscate --split-debug-info=build/debug-info
```

`--obfuscate` is mandatory (security requirement #6). It produces a mapping file under `build/debug-info/` that **must be uploaded to Play Console** for every release, or crash reports become unreadable. The directory is gitignored on purpose — never commit it.

---

## Architecture

Clean architecture, feature-first. The dependency direction is one-way and enforced by package layout:

```
presentation  →  domain  ←  data
   (Compose,      (models,      (Retrofit,
    ViewModel)     interfaces)   Hilt modules)
```

`domain` knows nothing about Retrofit, Compose or Android. `data` implements the interfaces `domain` declares. `presentation` sees only `domain`.

```
app/src/main/kotlin/com/mrdastbaaf/mdbapp/
├── MdbApplication.kt              @HiltAndroidApp
├── MainActivity.kt                single Activity, edge-to-edge
│
├── core/
│   ├── config/                    AppConfig (env, timeouts) · FeatureFlags
│   ├── constants/                 ApiPaths · SecurityConfig · WatermarkConfig
│   ├── error/                     Failure (sealed) · ApiException · error mapping
│   ├── network/                   OkHttp + Retrofit · MdbApi · interceptors/ · DTOs
│   ├── session/                   SessionManager — the one source of "signed in"
│   ├── storage/                   SecureTokenStore (Keystore) · LocalStore (caches)
│   ├── security/                  SecureScreen (FLAG_SECURE)
│   ├── navigation/                Routes · MdbNavHost
│   ├── ui/                        MdbApp · theme/ · component/ · state/
│   └── util/                      PersianFormat · PhoneNormalizer
│
└── features/
    └── home/                      data/ · domain/ · presentation/
```

### Key decisions

**`Failure` is a `Throwable` hierarchy.** It lets `Result<T>` carry a typed failure across repository boundaries without inventing an `Either` type, while `exceptionOrNull() as? Failure` keeps the exhaustive `when` in the UI honest.

**`UiState` has four members, and `Offline` is not one of them.** Connectivity is orthogonal to whether the last load succeeded: a cached catalogue should stay readable while offline, so "offline" is a banner driven by `ConnectivityObserver`, not a screen state. Only the initial load with no cache renders the offline error.

**Screen capture protection is a composable, not a singleton.** `FLAG_SECURE` belongs to a *window*, and the only reliable owner of the current window is the composition that is on screen. `SecureScreen { }` sets it on entry and clears it on dispose, so navigating from the player back to the catalogue restores normal screenshot behaviour.

**Tokens live in the Android Keystore, not `EncryptedSharedPreferences`.** `androidx.security:security-crypto` was deprecated by Google and its final release is an alpha. `KeystoreCrypto` generates an AES-256-GCM key in the hardware-backed Keystore and stores the ciphertext itself — the behaviour the brief asks for, without a dead dependency.

**RTL is forced in the theme.** The app is Persian-only, so relying on the device locale would render left-to-right on any phone set to English. `MdbTheme` provides `LayoutDirection.Rtl`, so no screen needs a local override.

**Interceptors are ordered deliberately:** logging → retry → auth. Retries happen *outside* auth so a retried request is re-signed; logging is outermost so the recorded duration reflects what the user actually waited.

**No cache-busting interceptor.** The web client appends `?_=timestamp` to defeat LiteSpeed, but that only matters for a browser's shared cache. The plugin already sends `Cache-Control: no-store` and `X-LiteSpeed-Cache-Control: no-cache` on every response in the namespace.

---

## Security model — and its honest limits

This section is required reading before anyone claims the app "protects the videos".

### What is implemented

| Requirement | Implementation |
|---|---|
| Never persist video URLs or tickets | They exist only in memory for the duration of a playback session. A fresh ticket is requested immediately before every playback. `LocalStore` holds no media data. |
| `FLAG_SECURE` on paid screens | `core/security/SecureScreen.kt`, via the window — blocks screenshots, screen recording, and blanks the recents thumbnail. |
| Token storage | AES-256-GCM ciphertext in `SharedPreferences`; key in the Android Keystore, non-exportable. Excluded from cloud backup and device transfer. |
| HTTPS only | `cleartextTrafficPermitted="false"` for all hosts, `usesCleartextTraffic="false"`. |
| No secret logging | `SafeLoggingInterceptor` logs only method, path, status and duration. No headers, no bodies — so it *cannot* leak a token or a video URL. |
| R8 + obfuscation | `--obfuscate --split-debug-info`, with Media3 and kotlinx.serialization keep rules. |
| Certificate pinning | Implemented in `core/network/PinningConfig.kt`, **off by default**. Read the rotation warnings in that file before enabling it. |

### What is **not** protected — stated plainly

**Neither `FLAG_SECURE` nor the watermark stops someone who extracts the video URL from network traffic.**

- `FLAG_SECURE` blocks the *screen*. It does nothing about the *stream*. Anyone who can read the network traffic — a rooted device, a proxy with a user-installed CA, a modified client — can recover the URL and download the file.
- The watermark is **advisory and client-rendered**. It is a strong deterrent against re-recording and a way to attribute a leak, but a modified client can simply not draw it. There is no server-side burn-in.
- **Today the API hands out a permanent, direct CDN URL** for each lesson and quality. `POST /lessons/{id}/playback` returns `videoUrl` pointing straight at `dll.mrdastbaaf.com`. Whoever obtains that URL can download the video forever, with no login. The only server-side mitigation is Referer-based hotlink protection, which is trivially spoofed.

**Real protection requires server-side work that has not been built yet:**

1. **HLS with short-lived signed segment URLs** — the video is sliced into segments, each with an expiry, so a captured URL is worthless within minutes and no single request yields the whole file. The plugin has a `mrdastbaaf_stream_video_url` filter reserved for exactly this; nothing implements it.
2. **Device-bound playback sessions** — a token tied to one installation, renewable via a heartbeat, so a leaked URL is unusable elsewhere.
3. **AES-128 encrypted HLS** with the key served from an authenticated endpoint, so segments are useless without a live session.

Until those exist, treat the app's client-side controls as **raising the cost of casual sharing**, not as content protection. This is a property of the backend, not a gap the Android app can close.

The full gap analysis — including the exact request/response shapes the server needs to add — is in [`docs/API_REVIEW_AND_PLAN.md`](docs/API_REVIEW_AND_PLAN.md).

---

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| **P0** | Scaffold: Gradle, flavours, Compose + M3 theme, Vazirmatn, RTL, Hilt, Retrofit stack, error model, secure storage, `FLAG_SECURE` | ✅ |
| **P1** | Core infrastructure: connectivity tracking, `ApiPaths`, interceptor behaviour | ✅ (folded into P0/P3) |
| **P3** | Auth: OTP request/verify, Persian-digit normalisation, conditional signup fields, session, logout, profile | ✅ |
| P2 | Security shell: Kotlin root/emulator detection behind a config flag | next |
| P4 | Catalogue and course detail: `/courses`, `/courses/{slug}`, `/my/courses`, `/app-ui`, `/app-pages` | |
| P5 | Membership and dashboard: plans, status, progress, orders, Jalali dates | |
| P6 | Shop showcase: `/shop/products`, read-only, opens the website | |
| P7 | Player: Media3, quality selector, dynamic watermark, background pause, foreground re-validation, resume | blocked on the server (see below) |
| P8 | Hardening: R8 verification, optional pinning, release AAB | |

> ⚠️ **The P0–P3 code has not been compiled yet.** The machine this was authored
> on has only JDK 15 and Android SDK platform 33 — too old for AGP 8.x — and its
> network could not sustain the toolchain downloads (27 KB/s). Run
> `./gradlew assembleDevDebug` in Android Studio and report anything that fails.

### Blocked on the server

| Item | Blocks |
|---|---|
| **`GET/PUT /lessons/{id}/progress`** — playback position. The API currently stores only completed/bookmarked lesson ids, so "resume from last position" is impossible. | P7 resume |
| `videoType` on `videoUrl` and each `qualities[]` entry — extension sniffing is unreliable (proxy URLs have no extension at all) | clean MP4↔HLS switch |
| `watermarkFullName` on the playback ticket — today the full name comes from a separate `/auth/me` call | single-source watermark |
| Multi-device tokens — the backend has **one token slot per user**, so signing in on the phone signs the user out of the web PWA | release |

---

## Contributing

- Persian (fa) only, full RTL. **ASCII digits on the wire, Persian-Indic digits on screen** — `PersianFormat` and `PhoneNormalizer` exist to enforce exactly that.
- All user-facing strings in `res/values/strings.xml`. No hardcoded Persian in Kotlin.
- Every screen handles loading / empty / error / offline through `AsyncStateView`.
- Repository interfaces get unit tests. Run `./gradlew test` before committing.
- Small commits, one concern each.
