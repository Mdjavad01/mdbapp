# Changelog

Three products, three version numbers. They move independently — an app release
does not imply a plugin release — but the "Pairs with" column says what was
tested together.

Release assets: [GitHub Releases](https://github.com/Mdjavad01/mdbapp/releases).
Plugin history before 1.7.16, and web history before the import, are in
[`web/CHANGELOG.md`](web/CHANGELOG.md).

---

## Android app

| Version | Code | What changed |
|---|---|---|
| **1.7.3** | 23 | the palette comes from the panel: `GET /app-ui` → `theme` feeds `MdbTheme(brand = …)` through a `BrandViewModel`, so a recolour is a save in WordPress, not a release. A bad hex falls back anchor by anchor |
| 1.7.2 | 22 | HLS playback with a real quality menu: the renditions come from the manifest and are selected through the track selector, not by swapping URLs. MP4 keeps the old path |
| 1.7.1 | 21 | every request now carries `X-MDB-Client: android/<version>`, so the bridge can record whether a sale came from the app or the site |
| 1.7.0 | 20 | shop is a category menu → section page with breadcrumb, subcategory chips and paged products |
| 1.6.0 | 19 | data layer for `/shop/categories` + `/shop/catalog` (1800 products, paginated) |
| 1.5.0 | 18 | bottom bar follows the panel's `dock` switches; `/app-ui` fetched once behind a mutex; 20 MB disk cache for the three public endpoints |
| 1.4.0 | 17 | membership plans became a duration ladder (1/3/6/12 months) with a computed per-month price and saving badge; tier names gone from the UI |
| 1.3.0 | 16 | shop opened as menu → submenu instead of one long scroll |
| 1.2.0 | 15 | the site's palette (terracotta/saffron/cream) derived from four anchors, so a recolour is four numbers |
| 1.1.3 | 14 | in-app plans screen; SMS autofill; haptics on a wrong code; calmer, centred login field |
| 1.1.2 | 13 | the OTP field no longer locks after a wrong code; digit boxes fill left to right |
| 1.1.1 | 12 | course detail and checkout restyled — hero with price chip, lesson cards, gradient app bar |
| 1.1.0 | 11 | shop cards rebuilt to match the catalogue |
| 1.0.9 | 10 | home hero with a drawn kilim texture; deeper sections and guide list |
| 1.0.8 | 9 | catalogue: segmented filter and richer course cards |
| 1.0.7 | 8 | account: gradient profile hero, staggered sections, plan and order cards |
| 1.0.6 | 7 | login and OTP rebuilt: gradient header, shared primary action, six digit boxes over one hidden field |
| 1.0.5 | 6 | release signing from `keystore.properties`; tiled, drifting watermark on the player |
| 1.0.4 | 5 | subscriptions purchasable inside the app, same gateway as the shop |
| 1.0.3 | 4 | fixed the shop crash on duplicate list keys; rebuilt the hero layout; brand-gradient app bar on every tab |
| 1.0.2 | 3 | home tab driven by `/app-ui` and `/app-pages` |
| 1.0.1 | 2 | shop tab, shipping form, in-app Zibal payment; versioned APK filename |
| 1.0.0 | 1 | scaffold, auth, catalogue, course detail, player |

---

## WordPress plugin (`mrdastbaaf-bridge`)

| Version | What changed | Pairs with |
|---|---|---|
| **1.7.24** | fixes 1.7.23: `get_app_ui()` built its response array by hand and dropped the `theme` block that `get_defaults()` had just produced — so the colour fields saved fine and no client ever saw them. The block is now in the response, with `test-app-ui-theme.php` (11 cases) guarding it | app 1.7.3 · web 1.8.0 |
| 1.7.23 | `GET /app-ui` returns a `theme` block (primary/accent/canvas/success) and the settings page finally has the four colour fields for it — a recolour is a save in WordPress, not two releases. An invalid hex keeps the previous value instead of repainting the app | app 1.7.3 · web 1.8.0 |
| 1.7.21 | playback reports `videoType` (hls/mp4) and an HLS lesson returns no separate qualities; the PHP stream proxy is bypassed for HLS; the lesson form explains where `master.m3u8` goes | app 1.7.2 · web 1.6.0 |
| 1.7.20 | `source` column on orders (app/web), detected from `X-MDB-Client` or the OkHttp agent; `_mrdastbaaf_source` on WooCommerce orders; new «سفارش‌ها» screen with filters and CSV export; `created_at` index | app 1.7.1 |
| 1.7.19 | receipt status tabs sit in a card; fixed column widths so the table stops scrolling sideways | app 1.7.0 |
| 1.7.18 | receipt page uses the full width; the category cache key includes the plugin version, so a release takes effect immediately | app 1.7.0 |
| 1.7.17 | the course category is excluded from the shop tree | app 1.7.0 |
| 1.7.16 | `GET /shop/categories` (tree, `parent`, `count`, 10-minute transient) and `GET /shop/catalog` (24 products per page) — the shop for 1800 products | app 1.7.0 |
| 1.7.15 | *imported from the team repository* — shop products, plans, manual receipts, playback | app 1.0.0 |

---

## Web app (PWA)

| Version | What changed |
|---|---|
| **1.8.1** | the palette actually scales: the whole amber and emerald scales are re-derived from the four panel anchors, so every button, link, chip, border and heading follows a recolour — no component knows a hex value. Button labels flip to dark when the primary is light, and brand-coloured text is darkened until it passes 4.5:1 (7:1 for headings). The default palette renders byte-identically to 1.8.0 |
| 1.8.0 | the shop is a real catalogue: `#/shop` is a category menu → section page with breadcrumb, subcategory chips and 24 products per page from `GET /shop/categories` + `GET /shop/catalog` — so a new WooCommerce product shows up on the site without being hand-ticked. Membership lost the برنزی/نقرهای table and shows duration plans with a per-month price and a saving badge, like the app. The palette now comes from the panel as CSS variables |
| 1.7.3 | network-stall retries and a visibility resume in the player |
| 1.7.2 | the black screen was `canPlayType` lying about HLS — hls.js now runs whenever it is supported; `#/debug` player self-test added |
| 1.7.0–1.7.1 | hls.js player with a quality menu built from the manifest, and the playback error overlay that names the real reason |
| 1.6.0 | the player uses hls.js for `videoType: hls` and builds its quality menu from the manifest; Safari keeps native playback and no menu. **First change to the web app in this repository** — it now diverges from upstream, see below |
| 1.5.7 | imported from `mrdb` at `0e310d4` — mobile login bottom-sheet, instructor avatar, receipt image upload |

From 1.6.0 the web app is **ahead of upstream** (`mrdb` is still at 1.5.7), so
`tools/sync-from-team.sh` skips it instead of overwriting. Those changes have to
go upstream before the two can be reconciled. The rest of the history is in
[`web/CHANGELOG.md`](web/CHANGELOG.md).
