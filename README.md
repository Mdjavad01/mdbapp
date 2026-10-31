# mrdastbaaf-bridge — plugin releases

This branch is not the app. It holds the WordPress plugin on its own, so it can
be released, versioned and downloaded as a normal plugin zip instead of being
delivered as a patch to be applied by hand.

- The plugin lives at `mrdastbaaf-bridge/` — exactly the folder WordPress
  expects, so a release zip uploads straight into **Plugins → Add New → Upload**.
- Releases are tagged `plugin-v<version>`; the app's tags are untouched.
- Every release asset is a ready zip. Nothing to edit, nothing to paste.

## Installing a release

1. Download `mrdastbaaf-bridge-<version>.zip` from the Releases page.
2. WordPress → Plugins → Add New → Upload Plugin → choose the zip.
3. If the plugin is already installed, WordPress offers to replace it; that
   keeps the settings, the database and the options untouched.

## Source of truth

The team's repository (`soldiermr98-commits/mrdb`) remains the origin of the
plugin. This branch is a build of it plus the changes made here, so the two can
be compared commit by commit. When write access to that repository exists, this
branch pushes there and this one stops being used.

## Changelog

### 1.7.20

**You can finally tell an app sale from a website sale.**

Every order used to look like a website order: `created_via` was hardcoded to
`mrdastbaaf-webapp` and the WooCommerce note said «از وب‌اپ» even when the
purchase came from the Android app. There was no source column and the app sent
no identifying header, so the two channels were indistinguishable.

- `wp_mrdastbaaf_orders` gains `source` (`app` | `web`), set at initiate time
  for both Zibal and card-to-card.
- The app sends `X-MDB-Client: android/<version>`. Builds older than that are
  still classified correctly: the OkHttp user agent is the tell, so app orders
  are tagged from the moment this version is installed, without an app update.
  A browser — including the PWA — sends `Mozilla/5.0` and stays `web`.
- WooCommerce orders now carry `_mrdastbaaf_source`, `created_via` is
  `mdbapp-android` for app purchases, and the note and payment title name the
  channel instead of always claiming the web app.
- `mrdastbaaf_order_source` filter for a future client (iOS, desktop wrapper).

**New screen: مستر دستباف → سفارش‌ها.** Until now online subscription sales had
no screen at all — «بررسی فیش‌ها» only lists card-to-card rows — so individual
plan purchases were invisible outside the database.

The page lists every purchase (subscription, course, physical product) with its
source, and filters by source, kind, status, gateway and free text, with a CSV
export of the current filter. It reads the one existing table — nothing is
copied — and pages on the server, with a new `created_at` index so it stays
fast at a hundred thousand orders.

### 1.7.19

**The status tabs sit in a card like everything else.** They were a bare
`ul.subsubsub` floating between two cards, with WordPress's default pipe
separators. They are now pills inside a `.mrd-card--tabs`, and the separator is
gone.

**The table's columns have widths.** With `table-layout: auto` the "جزئیات فیش"
cell — three lines plus a note — took whatever it wanted and pushed the rest
into a horizontal scrollbar. Columns 1–4 and 6–9 now have fixed widths and the
details column takes the remainder; the minimum width went from 1120 to 1240.

### 1.7.18

**The receipt queue is readable.** That table has nine columns and the admin
shell caps content at 1080px, so each column got about 87px: the "عملیات" cell
wrapped, the table scrolled sideways, and the card around it spent 40 more
pixels on padding for a second frame nobody needed.

- The page uses a 1680px container (`.mrd-scope--wide`); every other page keeps
  the old width, because forms do not want to be 1600px wide.
- The table is dense: 10px cell padding and a 1120px minimum, so it scrolls only
  when the window is genuinely narrow.
- The redundant `.mrd-card` wrapper is gone — the table already draws a frame.
- Confirm/reject buttons no longer wrap.

**Cache invalidation follows the version.** The category transient key now
carries the plugin version, so installing a release takes effect immediately
instead of after the ten-minute TTL. The old key is why 1.7.17 could appear not
to have worked.

### 1.7.17

**The course category no longer appears in the shop.** `product_cat` holds the
course products too, so `GET /shop/categories` listed «دوره های آموزشی» between
the looms and the yarn. Slugs in the exclusion list are filtered out; the
default is `courses`, and more can be added with the
`mrdastbaaf_shop_excluded_categories` filter — the site has a catch-all
`all-products` category that probably belongs there as well.

### 1.7.16

**Shop for 1800 products.** The site has more than ten categories and 1800
products; `GET /shop/products` answered that by sending every product in one
response and hydrating each one — roughly 600 KB and 1800 `wc_get_product()`
calls per request.

- `GET /shop/categories` — the tree with `parent` and `count`, no products.
  Ten rows, under 2 KB, cached in a transient for ten minutes: a warm hit runs
  zero queries. The cache is flushed on product and category saves.
- `GET /shop/catalog?category=&page=&per_page=` — one page of one category,
  24 products, with `total` and `totalPages`. Subcategories come from
  WooCommerce's own `include_children`, so a parent is never empty.
- Products are selected by WooCommerce visibility (published, not hidden from
  the catalog) instead of the hand-ticked list: 1800 checkboxes is not a
  workflow, and the hidden switch already exists in the product editor.
- `GET /shop/products` is untouched — the web app behaves exactly as before.

### 1.7.15

Baseline. Everything before this branch is the team's released plugin.
