# Server-side changes — plugin v1.8.0

Four independent, reviewable changes to the WordPress bridge
(`wordpress-plugin/mrdastbaaf-bridge/`) that the Android app depends on.

Base: `70a04b1` (upstream `main`, plugin **1.7.13**, schema **1.5.1**)
Target: plugin **1.8.0**, schema **1.7.0**

They are stacked — each applies on top of the previous one — but each is a
separate commit with its own tests and its own contract document, so they can
be reviewed, merged or reverted one at a time.

---

## Why patches instead of pull requests

I do not have write access to `soldiermr98-commits/mrdb` (read-only), and the
GitHub token cannot create a fork (`403 Resource not accessible by personal
access token`). So the changes are delivered as a `git format-patch` series,
which is permission-free and applies directly.

To turn these into PRs, either:

1. **Fork the repo yourself**, then
   `git push <your-fork> feat/plugin-multi-device-tokens` etc. — the branches
   exist in the working copy at `D:\work-webs\0 Build\mrdb-server`; or
2. **Apply the series and push branches** (see below) and open the PRs from
   your own account.

The series has been verified: it applies cleanly to a pristine checkout of
`main`, reproduces the branch tree **byte-for-byte** (identical tree hash
`08ae19b`), and passes the full test suite.

---

## The four changes

| # | Patch | What it does | Tests |
|---|---|---|---|
| 1 | `0001-feat-auth-multi-device-Bearer-tokens…` | Multi-device tokens. Replaces the single per-user usermeta slot (which made signing in on the phone sign the user out of the web app) with a `wp_mrdastbaaf_api_tokens` table, max 3 devices, oldest revoked. Adds `GET/DELETE /auth/sessions`. | 40 |
| 2 | `0002-feat-progress-lesson-playback-position…` | `GET/PUT /lessons/{id}/progress`. Stores a playback position per user+lesson, resumes where the user stopped, auto-completes at 95%. | 33 |
| 3 | `0003-feat-playback-truthful-streamType-videoType…` | Corrects `streamType` (was hard-coded `"hls"` while serving MP4), adds `videoType` and per-quality `type`, adds `watermarkFullName`. | 16 |
| 4 | `0004-feat-playback-expiring-signed-URLs…` | Replaces the **permanent** CDN URL with a short-lived signed URL (nginx `secure_link`). Ships the nginx config and the migration note. | 43 |

Total: **143 assertions**, all passing.

Each patch also contains its own contract document under `docs/`:

- `docs/PLUGIN_v1.8.0_MULTI_DEVICE_TOKENS.md`
- `docs/PLUGIN_v1.8.0_PLAYBACK_POSITION.md`
- `docs/PLUGIN_v1.8.0_VIDEO_TYPE_AND_WATERMARK_NAME.md`
- `docs/NGINX_SIGNED_URLS.md` + `docs/DLL_HOST_NGINX_SIGNED_URLS.sample`

---

## Applying

```bash
git clone https://github.com/soldiermr98-commits/mrdb.git
cd mrdb
git checkout -b feat/plugin-v1.8.0

git -c am.keepcr=false am /path/to/server-changes/*.patch
```

`am.keepcr=false` matters: the plugin files use LF, and without it the
CRLF settings on Windows can corrupt the whitespace in the diffs.

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
pure functions only, so they run anywhere:

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
TOTAL PASS: 143
TOTAL FAIL: 0
FAILED FILES: 0
```

**Not covered by unit tests:** anything that needs a live `$wpdb` — the
token store's `issue`/`find_by_token`/`revoke`/`enforce_device_limit`, the
progress controller's SQL, and the settings round-trips. A manual staging
checklist for those is in each contract document.

---

## Deployment order (read before merging change 4)

Change 4 is the only one with a **server configuration** dependency, and it
is order-sensitive:

1. Merge and deploy the plugin **with signing still disabled** — it defaults
   to off, and when off it is a no-op. Nothing changes for users.
2. Put the shared secret in `wp-config.php`:

   ```php
   define('MRDASTBAAF_SIGNED_URL_SECRET', '<same value as in nginx>');
   ```

3. Update the nginx config on the download host
   (`docs/DLL_HOST_NGINX_SIGNED_URLS.sample`), but **leave enforcement
   commented out**. Reload.
4. Enable signing in the plugin. Now every playback URL is signed, but nginx
   is not yet checking — so nothing can break.
5. Verify a signature in the access log, then uncomment enforcement and
   reload nginx.
6. Confirm that a URL **without** the signature is now refused.

Doing 5 before 4 is what causes a total outage: nginx would reject every
request because PHP is not yet attaching a signature. The full runbook,
including rollback and the 403 troubleshooting table, is in
`docs/NGINX_SIGNED_URLS.md`.

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

**Change 4 does not touch the database at all.**

**Change 3 is additive.** Every pre-existing response key is unchanged, so
the shipped PWA 1.5.6 is unaffected by all four changes.

---

## What still needs doing on the server

Not part of these four, still open:

- **HLS + AES-128 packaging.** The download host now hands out expiring URLs,
  which closes the "capture it once, download forever" hole. It does not stop
  a determined user from pulling the file while their session is live. Real
  protection needs segment-level signing, which needs HLS.
- **Playback session + heartbeat** (`POST /lessons/{id}/session`). Needed for
  concurrent-stream limits and for the app's foreground re-validation.
- `GET /payment/order/{orderId}` for post-payment polling.
- Restrict `GET /auth/health` in production — it is public and reports host
  internals.

See `docs/API_REVIEW_AND_PLAN.md` in the app repo for the full list.
