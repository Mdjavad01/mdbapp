#!/usr/bin/env bash
#
# Build the PWA bundle that goes on app.mrdastbaaf.com.
#
#   tools/build-web.sh
#
# The build refuses to run without VITE_API_BASE_URL, so this script supplies the
# production endpoint when web/.env.production does not exist yet. It then checks
# that the four things the host needs actually landed in dist/ — a bundle that is
# missing sw.js or .htaccess looks fine locally and breaks updates in the field.

set -euo pipefail

root="$(git rev-parse --show-toplevel)"
cd "$root/web"

if ! command -v node >/dev/null; then
    echo "node is not installed — the web app needs Node 20 or newer." >&2
    exit 1
fi

# ── dependencies ────────────────────────────────────────────────────────────
if [ ! -d node_modules ]; then
    echo "installing dependencies (first run)…"
    # --legacy-peer-deps: the lockfile pins esbuild ^0.25 while vite 8 wants
    # ^0.27. The team installs with bun, which does not enforce that peer range.
    npm ci --legacy-peer-deps --no-audit --no-fund
fi

# ── API endpoint ────────────────────────────────────────────────────────────
if [ ! -f .env.production ]; then
    echo "no web/.env.production — creating it with the production endpoint"
    printf 'VITE_API_BASE_URL=https://mrdastbaaf.com/wp-json/mrdastbaaf/v1\n' > .env.production
fi

# ── version ─────────────────────────────────────────────────────────────────
# The panel's «نسخهٔ مستقر PWA» must be set to exactly this number after the
# upload, otherwise installed clients never see the update banner.
version="$(sed -n "s/.*PWA_VERSION = '\([^']*\)'.*/\1/p" src/version.ts | head -n 1)"

echo
echo "building web ${version:-unknown}…"
npm run build

# ── verify the output ───────────────────────────────────────────────────────
missing=0
for f in index.html sw.js manifest.webmanifest .htaccess; do
    if [ ! -e "dist/$f" ]; then
        echo "MISSING from dist/: $f" >&2
        missing=1
    fi
done
if [ "$missing" -ne 0 ]; then
    echo >&2
    echo "the bundle is incomplete — do not upload it." >&2
    exit 1
fi

size="$(du -sh dist | cut -f1)"

echo
echo "──────────────────────────────────────────────────────────────────────"
echo " dist/ is ready ($size) — $root/web/dist"
echo "──────────────────────────────────────────────────────────────────────"
echo
echo " 1) Upload the CONTENTS of web/dist to the root of app.mrdastbaaf.com"
echo "    (not the folder itself; index.html must sit at the root)."
echo
echo " 2) WordPress → مستر دستباف → تنظیمات → «نسخهٔ مستقر PWA»"
echo "    set it to: ${version:-<the version in src/version.ts>}"
echo "    Saving that field is what tells installed clients an update exists."
echo
echo " 3) Check it: open app.mrdastbaaf.com/#/debug — the bundle version shown"
echo "    there must be the one you just set."
echo
echo " Users already on an older bundle get a «نسخهٔ جدید آماده است» banner."
echo " Users who never update can be told to open #/debug and clear the cache."
echo
