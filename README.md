# mdbapp

Everything that ships to a customer for **mrdastbaaf.com** — the Persian
handicraft marketplace and course platform — in one repository.

| Folder | What it is | Stack |
|---|---|---|
| [`app/`](app) | The Android client | Kotlin · Jetpack Compose · Media3 |
| [`web/`](web) | The PWA that runs in a browser | React · Vite · TypeScript |
| [`wordpress-plugin/mrdastbaaf-bridge/`](wordpress-plugin/mrdastbaaf-bridge) | The WordPress plugin — the only backend either client talks to | PHP |

## Why one repository

The app and the PWA are **consumers of one REST API**. That API is not a separate
service: it is the `mrdastbaaf-bridge` plugin inside the WordPress installation.
When a field is renamed in the plugin, a client breaks — and for a year that
breakage was discovered only on a phone, days later.

Keeping the three together means:

- a change to a response shape and the clients that read it land in one review;
- the app's `ApiPaths.kt`, the plugin's route table and the PWA's service layer
  are visible side by side;
- one release note can state which plugin version a given app build needs.

## How they fit together

```
        ┌──────────────┐        ┌──────────────┐
        │   app/       │        │    web/      │
        │  (Android)   │        │   (PWA)      │
        └──────┬───────┘        └──────┬───────┘
               │   HTTPS + Bearer      │   cookie / Bearer
               └───────────┬───────────┘
                           ▼
              /wp-json/mrdastbaaf/v1/…
                           │
        ┌──────────────────┴──────────────────┐
        │  wordpress-plugin/mrdastbaaf-bridge │
        │  (WordPress + WooCommerce + Zibal)  │
        └─────────────────────────────────────┘
```

Neither client decides access. Whether a lesson plays, whether a plan is active,
what a product costs — the plugin answers, the clients display. Anything a client
decides locally is a bug waiting for a server change.

## Versions that go together

| Component | Version | Notes |
|---|---|---|
| Plugin | **1.7.20** | order source (app/web), orders screen, shop catalogue endpoints |
| Android app | **1.7.1** | versionCode 21 |
| Web app | **1.5.7** | unchanged since the team's last release |

The app is built against the plugin above; the endpoints it uses are listed in
[`docs/API_REVIEW_AND_PLAN.md`](docs/API_REVIEW_AND_PLAN.md).

## Building

### Android

```bash
./gradlew assembleDevDebug     # APK for a device
./gradlew test                 # unit tests
./gradlew assembleProdRelease  # R8-shrunk release (signing needs keystore.properties)
```

Requirements and flavours are in [`app/README.md`](app/README.md). The machine
this is developed on cannot build Android, so **CI is the compiler of record** —
[`.github/workflows/android.yml`](.github/workflows/android.yml) builds, tests and
uploads the APK on every push to `main`.

### Web

```bash
cd web
npm ci --legacy-peer-deps        # see the note below
npm run dev                      # http://localhost:3000, demo data
npm run lint                     # tsc --noEmit
VITE_API_BASE_URL=https://mrdastbaaf.com/wp-json/mrdastbaaf/v1 npm run build
```

`npm run build` **refuses to run** without `VITE_API_BASE_URL`, and refuses a
non-https value. That guard is deliberate: a production bundle silently pointing
at mock data is worse than a failed build. Put the value in `web/.env.production`
if you build often.

`--legacy-peer-deps` is needed because the lockfile pins `esbuild ^0.25` while
`vite 8` wants `^0.27`. The team installs with **bun** (`web/bun.lock`), which
does not enforce that peer range. The dependency itself works — the resolution
is what npm objects to. Fixing it properly means bumping `esbuild` upstream.

### Plugin

There is nothing to compile. Lint the PHP and run the plugin's own tests:

```bash
php -l wordpress-plugin/mrdastbaaf-bridge/mrdastbaaf-bridge.php
cd wordpress-plugin/mrdastbaaf-bridge && bash tests/php/run-all.sh
```

## Releasing

### Plugin

Releases are **built from this repository** — the plugin is not a patch to be
applied by hand, and it is not a second copy of the code.

```bash
tools/release-plugin.sh 1.7.21
```

That zips `wordpress-plugin/mrdastbaaf-bridge` at the current commit, prints the
tag and the upload command, and leaves the zip in `dist/`. The release asset is
what the owner uploads through **Plugins → Add New → Upload**.

Tags are `plugin-v<version>`, so they never collide with app tags.

> Releases up to **1.7.20** were built from a separate `plugin` branch. That
> branch is now frozen history: from here on the plugin lives in `main` and is
> released from it.

### Android

Push to `main`; CI builds and uploads `mdbapp-dev-debug-v<version>.apk`. A signed
release needs `keystore.properties` (see
[`keystore.properties.example`](keystore.properties.example)) and is built
locally, never in CI — the key is deliberately not in the repository.

## Keeping in sync with the team repository

`web/` and the plugin originate from
[`soldiermr98-commits/mrdb`](https://github.com/soldiermr98-commits/mrdb), which
the team still owns. That repository remains the upstream for the PWA.

```bash
tools/sync-from-team.sh          # fetch upstream and report what changed
tools/sync-from-team.sh --apply  # copy it into web/ and wordpress-plugin/
```

The sync is a **snapshot**, not a merge: upstream wins for those two folders. Any
change we make there has to go upstream first, or the next sync drops it. That
rule exists because the alternative — two copies drifting apart — is what made
the plugin version mismatch in the first place.

## Layout

```
.
├── app/                      Android module (Gradle)
├── web/                      PWA — upstream snapshot, see above
├── wordpress-plugin/
│   └── mrdastbaaf-bridge/    the bridge plugin
├── docs/
│   ├── API_REVIEW_AND_PLAN.md   the API contract, reviewed endpoint by endpoint
│   ├── SCALING.md               what breaks at 10k users, and in what order to fix it
│   └── legacy-patches/          obsolete patches, kept for the features they describe
├── tools/                    sync + release scripts
├── .github/workflows/        Android CI
├── app/build.gradle.kts, settings.gradle.kts, gradlew…   (Gradle root)
└── keystore.properties.example
```

## Contributing

- **Persian (fa) only, full RTL.** ASCII digits on the wire, Persian digits on
  screen.
- All user-facing strings in `app/src/main/res/values/strings.xml` — no hardcoded
  Persian in Kotlin.
- Every screen handles loading / empty / error / offline through `AsyncStateView`.
- Repository interfaces get unit tests. `./gradlew test` before committing.
- Small commits, one concern each. The commit message says *why*, not *what*.
