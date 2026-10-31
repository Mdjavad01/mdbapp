# Contributing

Three products live here. Knowing which one you are touching is most of the job.

| Folder | Owner | Language | You change it when… |
|---|---|---|---|
| `app/` | Android | Kotlin | the phone app changes |
| `web/` | Web | TypeScript | the PWA changes |
| `wordpress-plugin/mrdastbaaf-bridge/` | Backend | PHP | a response shape, a price, an endpoint changes |

## The one rule that matters

**The plugin is the contract.** Neither client invents an endpoint, and neither
decides access locally. If the app needs a new field, the field is added to the
plugin *first*, in the same pull request if possible — otherwise the app ships a
screen that shows nothing and nobody knows why.

Practical consequences:

- A change to a response shape touches at least two folders. Say so in the PR.
- A change to `app/src/main/kotlin/…/core/constants/ApiPaths.kt` is a plugin
  change too, even if no PHP is in the diff.
- Never make the app "work around" a missing field by computing it locally from
  other fields. Ask for the field.

## Branches and reviews

`main` is what ships. Nothing is committed to it directly except documentation
and the release commit itself.

```bash
git switch -c feat/shop-pagination
# …work…
git push -u origin feat/shop-pagination
# open a pull request
```

A pull request is ready when:

- [ ] `./gradlew test` passes (if `app/` changed)
- [ ] `npm run lint` and `npm run build` pass (if `web/` changed)
- [ ] `php -l` passes on every touched file and the plugin tests pass (if the
      plugin changed)
- [ ] the PR says which folders it touches and whether the plugin and the app
      must be released together
- [ ] no secrets: no `.env`, no keystore, no token in a test fixture

CI runs the same three checks, so a red run is not a style opinion — it is the
build you would have gotten.

## Running the checks locally

### Android

```bash
./gradlew test              # unit tests, no device needed
./gradlew assembleDevDebug  # the APK
./gradlew lint              # Android Lint, fails on errors
```

The machine this project was originally authored on cannot build Android, so CI
is the compiler of record. If you *can* build locally, do — it is faster.

### Web

```bash
cd web
npm ci --legacy-peer-deps   # the lockfile has a peer conflict; see the root README
npm run lint                # tsc --noEmit
npm run dev                 # demo mode, no backend needed
```

`npm run build` needs `VITE_API_BASE_URL` and refuses to run without it. That is
deliberate: a production bundle silently serving mock data is worse than a build
that stops.

### Plugin

```bash
php -l wordpress-plugin/mrdastbaaf-bridge/mrdastbaaf-bridge.php
cd wordpress-plugin/mrdastbaaf-bridge
for f in tests/php/test-*.php; do php "$f"; done
```

The tests are plain PHP with no WordPress — they stub what they need, so they run
anywhere. If you see *"Xdebug has detected a possible infinite loop"*, that is
Xdebug's frame limit, not a loop; run with `XDEBUG_MODE=off`.

## Commit messages

Say **why**, not what. The diff already says what.

```
fix(shop): keep the course category out of the shop menu

product_cat holds the course products too, so the tree listed
«دوره های آموزشی» between the looms and the yarn.
```

`type(scope):` with `feat` · `fix` · `style` · `perf` · `refactor` · `docs` ·
`test` · `chore`. One concern per commit.

## Releasing

| What | How |
|---|---|
| Android | push to `main`; CI builds `mdbapp-dev-debug-v<version>.apk` and uploads it as an artifact |
| Plugin | `tools/release-plugin.sh <version>` → tag `plugin-v<version>` → upload the zip to a GitHub Release |
| Web | `npm run build`, then upload `web/dist` to the host |

Version numbers live in three places and must be bumped by hand:
`app/build.gradle.kts`, `web/package.json`, and the plugin header **plus** its
`MRDASTBAAF_BRIDGE_VERSION` constant. `tools/release-plugin.sh` refuses to
package a zip whose filename disagrees with the header.

## Traps that have already cost time

| Symptom | Cause |
|---|---|
| `git am` stops on the first hunk | the patch was built on a branch that was never merged; see `docs/legacy-patches/` |
| `npm ci` fails with ERESOLVE | `esbuild ^0.25` vs `vite 8`; use `--legacy-peer-deps` |
| `npm run build` refuses | `VITE_API_BASE_URL` missing or not https |
| PHP files show as fully rewritten in a diff | CRLF vs LF — the repo normalises to LF, so re-checkout rather than editing line endings |
| `./gradlew` says "bad interpreter" | `gradlew` was checked out with CRLF; `.gitattributes` pins it to LF |
| Plugin change does not appear on the site | the category tree is cached for ten minutes; the cache key includes the plugin version, so reinstall rather than waiting |

## Where the history is

The PWA and the plugin were imported from
[`soldiermr98-commits/mrdb`](https://github.com/soldiermr98-commits/mrdb). That
repository still holds the full history before the move —
[`docs/MIGRATION.md`](docs/MIGRATION.md) records exactly what came from where.
