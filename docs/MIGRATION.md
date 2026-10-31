# Where everything came from

This repository is a **migration**, not a fork. The Android app was built here
from the start; the PWA and the plugin were imported from the team's repository
on 2026-10-31. Their history before that date lives in the original repository,
which is still online.

## Source

| | |
|---|---|
| Repository | [`soldiermr98-commits/mrdb`](https://github.com/soldiermr98-commits/mrdb) |
| Branch | `main` |
| Commit imported | **`0e310d4`** — *Merge feat/v1.5.7-login-sheet-instructor-receipt-image — web app 1.5.7 + plugin 1.7.15* |
| Date of that commit | 2026-10-17 |

`git log` in the old repository is the history for `web/` and the plugin up to
that commit. Nothing was rewritten or squashed — it simply lives there.

## What moved where

| Old path | New path | Changed? |
|---|---|---|
| `src/`, `public/`, `index.html`, `package.json`, `vite.config.ts`, `tsconfig.json`, `vitest.config.ts`, `tests/`, `scripts/`, `docs/`, `FullDoc.MD`, `CHANGELOG.md`, `metadata.json`, locks | `web/` (same names) | no — moved as a unit, so relative links still resolve |
| `wordpress-plugin/mrdastbaaf-bridge/` | `wordpress-plugin/mrdastbaaf-bridge/` | no — same path |
| — | `app/` | the Android app, which was never in `mrdb` |

## What was already different from the import

The plugin here is **1.7.20**, not the 1.7.15 that `0e310d4` contains. Those five
versions were built in this repository and are **not** in the old repository:

| Version | Change |
|---|---|
| 1.7.16 | `GET /shop/categories` and `GET /shop/catalog` — the shop for 1800 products |
| 1.7.17 | the course category is excluded from the shop tree |
| 1.7.18 | receipt table width; the category cache key includes the version |
| 1.7.19 | receipt tabs in a card; fixed column widths |
| 1.7.20 | order source (app/web) and the «سفارش‌ها» screen |

Until those are pushed upstream, the two repositories differ in exactly this
list. `tools/sync-from-team.sh` will not overwrite them: it refuses when the
copy here is newer than upstream.

## The old repository's new role

It is the **archive** for everything before 2026-10-31. Two options, and only one
of them is healthy:

1. **Archive it** (recommended). Mark it read-only on GitHub, keep it for
   `git blame` and for the history of `web/`. All new work happens here.
2. **Keep both alive.** Then every change to `web/` or the plugin has to be made
   upstream first, because `tools/sync-from-team.sh` is a snapshot copy and will
   overwrite anything made only here. This is workable but it is twice the
   bookkeeping, and it is how the plugin ended up five versions behind.

## If you need the old history inside this repository

It is possible — `git subtree add --prefix=web <old-repo> main` grafts the
history under `web/` — but it also re-imports `web/wordpress-plugin/`, which then
has to be deleted again on every future pull. That trade was judged not worth it:
the old repository is not going away, and `git log` there answers the question
directly.
