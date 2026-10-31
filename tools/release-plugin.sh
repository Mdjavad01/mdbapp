#!/usr/bin/env bash
#
# Build a plugin release from this repository.
#
# The plugin is not delivered as a patch and not as a second copy of the code:
# it is zipped straight from wordpress-plugin/mrdastbaaf-bridge at the commit
# being released, and the zip is what the owner uploads through
# Plugins → Add New → Upload.
#
#   tools/release-plugin.sh 1.7.21
#
# The version must already be in the plugin header — this script refuses to
# publish a zip whose filename disagrees with the code inside it.

set -euo pipefail

version=""
allow_dirty=0

for arg in "$@"; do
    case "$arg" in
        --allow-dirty) allow_dirty=1 ;;
        -h|--help) sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        -*) echo "unknown option: $arg" >&2; exit 2 ;;
        *) version="$arg" ;;
    esac
done

if [ -z "$version" ]; then
    echo "usage: tools/release-plugin.sh <version> [--allow-dirty]   e.g. 1.7.21" >&2
    exit 2
fi

root="$(git rev-parse --show-toplevel)"
cd "$root"

plugin_dir="wordpress-plugin/mrdastbaaf-bridge"
main_file="$plugin_dir/mrdastbaaf-bridge.php"

if [ ! -f "$main_file" ]; then
    echo "plugin not found at $plugin_dir" >&2
    exit 1
fi

header_version="$(sed -n 's/^ \* Version: *//p' "$main_file" | head -n 1 | tr -d '\r')"
constant_version="$(sed -n "s/.*MRDASTBAAF_BRIDGE_VERSION', '\([^']*\)'.*/\1/p" "$main_file" | head -n 1 | tr -d '\r')"

if [ "$header_version" != "$version" ] || [ "$constant_version" != "$version" ]; then
    echo "version mismatch:" >&2
    echo "  asked for : $version" >&2
    echo "  header    : $header_version" >&2
    echo "  constant  : $constant_version" >&2
    echo >&2
    echo "Bump both in $main_file first — a zip whose name lies is worse than no zip." >&2
    exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "warning: working tree is dirty; the zip will contain uncommitted work." >&2
    if [ -t 0 ]; then
        printf 'continue? [y/N] '
        read -r answer || answer=""
        case "$answer" in y|Y) ;; *) exit 1 ;; esac
    else
        # No terminal to ask: never ship uncommitted code silently.
        if [ "$allow_dirty" -ne 1 ]; then
            echo "refusing in a non-interactive shell. Commit first, or pass --allow-dirty." >&2
            exit 1
        fi
    fi
fi

# ── syntax check before packaging ───────────────────────────────────────────
if command -v php >/dev/null; then
    find "$plugin_dir" -name '*.php' -print0 | while IFS= read -r -d '' f; do
        php -l "$f" >/dev/null || { echo "syntax error in $f" >&2; exit 1; }
    done
    echo "php -l: ok"
else
    echo "php not on PATH — skipping the syntax check"
fi

# ── package ─────────────────────────────────────────────────────────────────
mkdir -p dist
out="dist/mrdastbaaf-bridge-$version.zip"
rm -f "$out"

if command -v zip >/dev/null; then
    (cd "$(dirname "$plugin_dir")" && zip -qr "$root/$out" "$(basename "$plugin_dir")")
else
    py="$(command -v python3 || command -v python || true)"
    if [ -z "$py" ]; then
        echo "need either 'zip' or python3 to build the archive" >&2
        exit 1
    fi
    "$py" - "$plugin_dir" "$out" <<'PY'
import os, sys, zipfile

src, out = sys.argv[1], sys.argv[2]
name = os.path.basename(src)
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for base, _dirs, files in os.walk(src):
        for f in files:
            path = os.path.join(base, f)
            z.write(path, os.path.join(name, os.path.relpath(path, src)))
print("wrote", out)
PY
fi

size="$(wc -c < "$out" | tr -d ' ')"
echo
echo "built $out ($size bytes)"
echo "the zip root is '$plugin_dir' basename, so WordPress installs it as-is."
echo
echo "next:"
echo "  git tag -a plugin-v$version -m \"mrdastbaaf-bridge $version\""
echo "  git push origin plugin-v$version"
echo "  gh release create plugin-v$version \"$out\" \\"
echo "      --title \"mrdastbaaf-bridge $version\" --notes-file -   # paste the changelog"
echo
echo "Then upload the zip through Plugins → Add New → Upload Plugin."
