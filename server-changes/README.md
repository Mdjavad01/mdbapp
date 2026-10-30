# Server-side changes — plugin v1.8.0

Five independent, reviewable changes to the WordPress bridge
(`wordpress-plugin/mrdastbaaf-bridge/`) that the Android app depends on.

Base: `0e310d4` (upstream `main`, plugin **1.7.15**, schema **1.5.1**)
Target: plugin **1.8.0**, schema **1.7.0**

> Rebased onto `0e310d4` (plugin 1.7.14 + 1.7.15). The earlier series was cut
> from `70a04b1` (1.7.13) and stopped applying once upstream landed the
> content-hash WSOD fix and the receipt-image work. Both are now part of the
> base and are preserved: the two conflicts were "both sides added a line"
> (version constant, cron hooks, route registrations), resolved by keeping
> both. 1.7.14 and 1.7.15 do not touch `/version` or `/auth/*`.

They are stacked — each applies on top of the previous one — but each is a
separate commit with its own tests and its own contract document, so they can
be reviewed, merged or reverted one at a time.

---

## Why patches instead of pull requests

I do not have write access to `soldiermr98-commits/mrdb` (read-only), and the
GitHub token cannot create a fork (`403 Resource not accessible by personal
access token`). So the changes are delivered as a `git format-patch` series,
which is permission-free and applies directly.

The series has been verified end to end:

- applies cleanly to a pristine checkout of `0e310d4`, in order
- reproduces the branch tree **byte-for-byte** (tree hash `0b919e5`)
- the patched tree lints clean and passes all **324** assertions, plus the
  **20** assertions of upstream's `test-receipt-image.php`

To turn these into PRs, fork the repo yourself and push the branches — they
exist in the working copy at `D:\work-webs\0 Build\mrdb-server`:

```
feat/plugin-multi-device-tokens
feat/plugin-playback-position
feat/plugin-video-type-watermark-name
feat/plugin-signed-urls
feat/plugin-health-and-order-status
```

---

## The five changes

| # | Patch | What it does | Tests |
|---|---|---|---|
| 1 | `0001-feat-auth-multi-device-Bearer-tokens…` | Multi-device tokens. Replaces the single per-user usermeta slot (which made signing in on the phone sign the user out of the web app) with a `wp_mrdastbaaf_api_tokens` table, max 3 devices, oldest revoked. Adds `GET/DELETE /auth/sessions`. | 79 |
| 2 | `0002-feat-progress-lesson-playback-position…` | `GET/PUT /lessons/{id}/progress`. Stores a playback position per user+lesson, resumes where the user stopped, auto-completes at 95%. | 33 |
| 3 | `0003-feat-playback-truthful-streamType-videoType…` | Corrects `streamType` (was hard-coded `"hls"` while serving MP4), adds `videoType` and per-quality `type`, adds `watermarkFullName`. | 16 |
| 4 | `0004-feat-playback-expiring-signed-URLs-via-a-PHP-gateway…` | Replaces the **permanent** CDN URL with a short-lived signed URL served through a PHP gateway (`s.php`) on the download host. Ships the gateway, the `.htaccess` and the Apache runbook. | 114 |
| 5 | `0005-feat-api-restrict-auth-health-add-GET-payment-order…` | Restricts `GET /auth/health` to a token in production; adds `GET /payment/order/{orderId}` for post-payment polling. | 71 |
| 6 | `0006-feat-shop-category-tree-parent-count-v1.9.0.patch` | `GET /shop/products` now returns `parent` and `count` per category and includes the ancestors of ticked categories (empty `products`, `count: 0`) so the app can build a menu → submenu. Backward compatible with the web app. Plugin 1.9.0. | 1 |
| 7 | `0007-feat-plans-duration-days-hide-tier-v1.9.1.patch` | `GET /memberships/plans` returns `durationDays`; the plan form stops offering a tier dropdown (the column stays and is carried in a hidden input, because `tier_rank()` still gates course access). Plugin 1.9.1. | 1 |

Total: **324 assertions** across seven test files.

Each patch also contains its own contract document under `docs/`:

- `PLUGIN_v1.8.0_MULTI_DEVICE_TOKENS.md`
- `PLUGIN_v1.8.0_PLAYBACK_POSITION.md`
- `PLUGIN_v1.8.0_VIDEO_TYPE_AND_WATERMARK_NAME.md`
- `APACHE_SIGNED_URLS.md` (plus `gateway/htaccess.sample`,
  `gateway/signed-url-config.php.sample`)
- `PLUGIN_v1.8.0_HEALTH_AND_ORDER_STATUS.md`

---

## Applying

```bash
git clone https://github.com/soldiermr98-commits/mrdb.git
cd mrdb
git checkout -b feat/plugin-v1.8.0

git -c am.keepcr=false am /path/to/server-changes/*.patch
```

`am.keepcr=false` matters: the plugin files use LF, and without it the CRLF
settings on Windows can corrupt the whitespace in the diffs.

To apply them one at a time (recommended for review):

```bash
git am /path/to/server-changes/0001-*.patch
# review, run tests, then
git am /path/to/server-changes/0002-*.patch
```

To back out a single change after applying:

```bash
git revert <sha>
```

---

## Running the tests

The tests are plain PHP with no WordPress and no PHPUnit — they exercise the
pure functions plus a small in-memory `$wpdb` double, so they run anywhere:

```bash
cd wordpress-plugin/mrdastbaaf-bridge
PHP=/path/to/php ./tests/php/run-all.sh
```

They require `mbstring` for UTF-8 truncation of device labels. A minimal
`php.ini` is enough:

```ini
extension_dir = "ext"
extension=mbstring
extension=openssl
date.timezone = UTC
```

Expected output:

```
TOTAL PASS: 324
TOTAL FAIL: 0
FAILED FILES: 0
```

**Not covered by unit tests:** anything that needs a live `$wpdb` — the token
store's real `issue`/`find_by_token` SQL (the tests use an in-memory double
that only understands the queries the class issues), the progress controller's
SQL, `handle_get_order`, and the settings round-trips. A manual staging
checklist for those is in each contract document.

---

## Deployment order (read before merging change 4)

Change 4 is the only one with a **server configuration** dependency, and the
order is not negotiable. The gateway must be live before PHP starts signing,
and direct access must be blocked last.

1. **Deploy the gateway** to the download host (`dll.mrdastbaaf.com`):
   `s.php`, `SignedUrlGateway.php`, `signed-url-core.php`, and a
   `signed-url-config.php` with the shared secret and `media_root`.
   Do **not** touch `.htaccess` yet.
2. **Turn signing on in the plugin** — set `mrdastbaaf_signed_url_gateway` to
   `https://dll.mrdastbaaf.com/s.php`, set `mrdastbaaf_signed_urls_enabled`,
   and put `MRDASTBAAF_SIGNED_URL_SECRET` in `wp-config.php` with the same
   value as the gateway config. Verify playback still works.
3. **Only then block direct access** — install `gateway/htaccess.sample` as
   `.htaccess` in the media root. This supersedes the Referer-based
   `docs/DLL_HOST_HTACCESS.sample`, which a forged header defeats.
4. **Verify**: direct access 403, forged-Referer access 403, a real signed URL
   200, and a `Range` request 206.

Doing step 3 before step 2 causes a total outage: every request is refused
because PHP is not yet attaching a signature. The full runbook, including
rollback and a 403 troubleshooting table, is in `docs/APACHE_SIGNED_URLS.md`.

---

## Migration notes

**Changes 1 and 2 add tables.** They are created by the normal `dbDelta`
upgrade path on activation, and the schema version is bumped
(`1.5.1 → 1.6.0 → 1.7.0`) so the upgrade runs once. Existing data is
untouched.

**Change 1 needs no data migration.** Pre-upgrade tokens exist only as
`sha256(secret)` in usermeta, which is exactly what the new `token_hash`
column stores, so the existing constant-time comparison still validates them.
The row is created lazily on each user's next authenticated request, and the
legacy meta is then deleted. **No user is signed out by the upgrade** — which
matters here, because users are logged into the web app and buying
subscriptions through it.

**Changes 4 and 5 do not touch the database at all.**

**Change 5 changes a public endpoint's behaviour.** `GET /auth/health` stops
answering in production unless a token is configured. Nothing in the web app
or the PWA calls it, so this should be invisible — but if any uptime monitor
is watching it, that monitor will start seeing 404s. Set
`MRDASTBAAF_HEALTH_TOKEN` and add `?token=` if so.

**Changes 3 and 5 are additive** to existing response shapes. Every
pre-existing key is unchanged, so the shipped PWA 1.5.6 is unaffected by all
five changes.

---

## What still needs doing on the server

Not part of these five, still open:

- **HLS + AES-128 packaging.** The gateway now hands out expiring URLs, which
  closes the "capture it once, download forever" hole. It does not stop a
  determined user from pulling the file while their session is live. Real
  protection needs segment-level signing, which needs HLS.
- **Playback session + heartbeat** (`POST /lessons/{id}/session`). Needed for
  concurrent-stream limits and for the app's foreground re-validation.
- Multi-device is capped at 3 with no user-visible device list in the app yet
  (the endpoints exist: `GET/DELETE /auth/sessions`).

See `docs/API_REVIEW_AND_PLAN.md` in the app repo for the full list.
