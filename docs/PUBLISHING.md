# Publishing the app (Google Play · کافه‌بازار)

What the build produces, what each store needs, and the decisions that have to
be made before the first upload. The first upload is the one that matters: it
fixes the app's identity (package name + signing key) for good.

---

## 1. What we ship

| | Value |
|---|---|
| Package name | `com.mrdastbaaf.mdbapp` (permanent — never changes again) |
| Version | `versionName 1.7.4` · `versionCode 24` |
| minSdk / targetSdk | 24 (Android 7) / **36 (Android 16)** |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE` — nothing else |
| Native code | none, so no 64-bit split problem |

`targetSdk 36` is not cosmetic: Google Play rejects new apps and updates that
target below Android 16 from **2026-08-31**, and this repo compiled against 35
until 1.7.4. That is why 1.7.4 also bumps AGP to 8.9.2 and Gradle to 8.11.1 —
`compileSdk 36` needs them.

Two artefacts, same code, different wrappers:

| File | For | Why |
|---|---|---|
| `app-prod-release.apk` | کافه‌بازار | Bazaar takes APKs |
| `app-prod-release.aab` | Google Play | App Bundle is mandatory for new apps |

---

## 2. The signing key

`mdbapp-release.jks` + `keystore.properties` live at the repo root and are
**gitignored** — they exist on the owner's machine and nowhere else.

- alias `mdbapp`, valid until 2054-03-07
- SHA-256 `46:CE:5F:93:55:30:8A:10:95:07:F2:8F:0B:ED:44:C5:71:EA:DB:07:27:36:EF:C9:39:B5:DE:77:BA:06:2A:78`

Rules that are easy to learn the hard way:

1. **Back the key up.** Lose it and the app can never be updated — not on Play,
   not on Bazaar, not by sideloading over an installed copy. Keep an encrypted
   copy off this machine, with the two passwords.
2. **One key for both stores.** Play App Signing means our key becomes the
   *upload* key and Google holds the real signing key; Bazaar has no such
   service, so the APK is signed by us directly. Using the same key everywhere
   keeps one identity.
3. `keystore.properties` is read by Gradle; when it is absent the release build
   is left **unsigned** on purpose (never debug-signed).

---

## 3. Building a signed release

**Locally** (Android Studio machine, JDK 17, SDK 36):

```bash
./gradlew assembleProdRelease bundleProdRelease
# app/build/outputs/apk/prod/release/app-prod-release.apk
# app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

**From CI** — `.github/workflows/release.yml`, triggered by a tag or by hand.
It needs four repository secrets (Settings → Secrets and variables → Actions):

| Secret | How to get it |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 mdbapp-release.jks` (Git Bash: same command) |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` in `keystore.properties` |
| `ANDROID_KEY_ALIAS` | `mdbapp` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` in `keystore.properties` |

Then either push a tag `app-v1.7.4`, or run the workflow from the Actions tab.
It fails before building if a secret is missing, and after building it verifies
the APK's certificate against the fingerprint above — a wrong key is caught
here, not by the store.

---

## 4. Before the first upload (both stores)

- [ ] `versionCode` is higher than anything already published (24 today; bump
      on every upload, a store never accepts the same code twice).
- [ ] Icon 512×512 PNG, feature graphic 1024×500 (Play), 3–8 screenshots.
- [ ] Short + full description in Persian.
- [ ] **Privacy policy URL** — the app collects a phone number and a full name
      for orders, so both stores require one. It must say what is stored, why,
      and how to ask for deletion.
- [ ] Data-safety / data-collection form (Play) — phone number, name, purchase
      history; encrypted in transit; deletable on request.
- [ ] Content rating questionnaire (Play) / age rating (Bazaar).
- [ ] Test the release build itself, not the debug one: it is R8-minified and
      `isShrinkResources = true`, so a missing keep-rule only shows up here.

---

## 5. Decisions that belong to the owner

These are policy questions, not code questions. Verify the current wording in
each console before submitting — the rules change, and a rejection costs days.

1. **Selling courses inside the app.** The app takes payment through Zibal for
   memberships and courses. Bazaar's rule for *digital* goods has historically
   been that in-app purchase goes through their own payment SDK (Poolakey); Play
   has its own billing requirement for digital content. Two honest options:
   - ship the store build **without in-app purchase** (catalogue + player, buy
     on the website), which no store objects to; or
   - integrate the store's billing for the store build.
   The web/PWA build is unaffected either way.
2. **Play availability.** Google Play's developer programme is not available in
   every country; check that the account you intend to publish under can
   actually be registered before planning a launch date.
3. **Who signs the Play upload.** If the account is a company's, the upload key
   should belong to that company, not to a personal machine.

---

## 6. What is already true in this repo

- The release build type is R8-minified, resource-shrunk, non-debuggable, and
  signed from `keystore.properties` when present.
- `applicationIdSuffix` keeps the three flavours apart:
  `…mdbapp` (prod), `…mdbapp.staging`, `…mdbapp.dev`; the debug build type adds
  `.debug` on top, so a debug install never collides with a store install.
- No `usesCleartextTraffic`, `allowBackup="false"`, certificate pinning is
  opt-in at build time and currently **off** — enabling it before launch means
  pinning a certificate that must then never rotate without an app update.
