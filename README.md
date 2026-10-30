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
