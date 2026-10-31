# Protecting the videos — what is possible and what is not

This document exists because "the video must not be downloadable" is a
requirement that cannot be met literally, and pretending otherwise wastes money.
Below is what each layer actually buys, in order, and what it costs.

## The uncomfortable rule

**If a device can play it, that device can record it.** A screen recorder, a
capture card, or a camera pointed at a phone beats every client-side control
ever shipped. The only question is how much effort and traceability each step
costs an attacker.

So the goal is not "impossible". It is:

1. no single request that yields a file,
2. a URL that expires,
3. attribution when someone does leak it.

## Where the project stands today

| Layer | State | What it stops |
|---|---|---|
| `FLAG_SECURE` (app) | **done** | screenshots, screen recording, recents thumbnail |
| Tiled watermark (app) | **done** | nothing — but attributes a camera recording |
| `controlsList=nodownload`, no PiP, no right-click (web) | **done** | the obvious "save video" menu |
| Playback URL behind a ticket | **done** | anonymous hotlinking; the URL is only handed out after the access check |
| **Encryption of the segments** | **missing** | — |
| **Expiring segment URLs** | **missing** | — |
| DRM (Widevine) | not planned | — |

And one fact that matters more than all of the above: the current HLS output is
**one `.ts` file per quality**, addressed with `#EXT-X-BYTERANGE`. A single
`curl` per quality downloads the whole lesson:

```bash
curl -O https://dll.mrdastbaaf.com/FA/<course>/<lesson>/v0.ts   # the 1080p file
```

Three requests, no login, no ticket. That is the real state, and no client
change affects it.

## The ladder

### 1. Host-level hotlink protection — 10 minutes, stops casual sharing

On the video host (`dll.mrdastbaaf.com`), reject requests whose `Referer` is not
one of our own origins, and refuse to serve the playlist to a bare URL pasted in
a new tab.

```apache
# .htaccess on dll.mrdastbaaf.com
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteCond %{HTTP_REFERER} !^https://(app\.)?mrdastbaaf\.com/ [NC]
  RewriteCond %{HTTP_REFERER} !^$
  RewriteRule \.(m3u8|ts|mp4)$ - [F,L]
</IfModule>

# No directory listing, no index of the library
Options -Indexes
```

Honest note: `curl -H "Referer: https://mrdastbaaf.com/"` defeats it in one line.
It stops "copy the link into a new tab" and search-engine indexing, nothing more.

### 2. AES-128 encrypted HLS — the step that actually works

This is the one worth doing. Each segment is encrypted with a 16-byte key; the
manifest names a key URL; the player fetches the key **with the user's
credentials**, and the plugin answers only if that user may watch that lesson.
Without the key the `.ts` files are noise.

- No video bytes flow through PHP (the key is 16 bytes, fetched once).
- No expiry logic needed in URLs.
- Both players support it natively: hls.js and ExoPlayer read `#EXT-X-KEY`
  themselves.

**Encoder** — one extra flag on the conversion that already runs:

```bash
openssl rand 16 > lesson.key
printf 'https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/lessons/<id>/hls-key\n%s\n' \
  "$(pwd)/lesson.key" > lesson.keyinfo

ffmpeg -i source.mp4 \
  -hls_key_info_file lesson.keyinfo \
  -hls_time 6 -hls_playlist_type vod -hls_flags single_file \
  ... (the existing quality ladder)
```

**Plugin** — `GET /lessons/{id}/hls-key`, Bearer or cookie auth, the same access
check as playback, returns the 16 raw bytes with
`Content-Type: application/octet-stream` and no caching.

**Clients** — the key request must carry the token, and the key URL is baked into
the manifest at encode time, so the token cannot live in the URL:

- web: `new Hls({ xhrSetup: (xhr, url) => { if (url.endsWith('/hls-key')) xhr.setRequestHeader('Authorization', 'Bearer ' + token) } })`
- app: a `ResolvingDataSource` (or an OkHttp `DataSource.Factory`) that adds the
  same header for that path.

Cost: re-encode the library once (the encoder already runs per video), plus one
plugin release and one app release.

**What it does not stop:** a logged-in user who copies the key once and keeps it.
Mitigate by rotating the key per lesson occasionally, or by binding the key
endpoint to a playback session that must still be alive.

### 3. Expiring signed URLs — belt and braces

Sign each segment URL with an expiry (`?e=…&s=…`), so a copied URL dies in
minutes. This requires a gateway that rewrites the manifest on every play, which
is what `docs/legacy-patches/0004-*.patch` describes and why it was never built:
it puts every segment behind PHP. Only worth it on top of encryption, never
instead of it.

### 4. DRM — the only "real" answer, and probably not worth it here

Widevine/PlayReady with a licence server. Stops even a logged-in user from
extracting the file. Cost: a licence service, per-title keys, and a client that
loses offline playback unless you build download licences too. For a course
platform with a watermark and a known audience, encryption plus attribution is
usually the sane stopping point.

## Recommendation

1. **Now:** host rules from step 1, and keep the client controls.
2. **Next:** AES-128 with the authenticated key endpoint. It is the single change
   that turns "three curl commands" into "not without a valid session".
3. **Never claim** that the videos cannot be recorded. The watermark exists so
   that when one does leak, it carries the buyer's phone number.

The honest one-liner for the owner: *we can make downloading require a login and
a key; we cannot make it impossible, and anyone who says otherwise is selling
something.*
