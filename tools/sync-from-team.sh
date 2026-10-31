#!/usr/bin/env bash
#
# Sync `web/` and the plugin from the team repository.
#
# The team still owns soldiermr98-commits/mrdb and keeps pushing to it. This
# repository holds a snapshot of that work so everything is in one place — but a
# snapshot that nobody refreshes becomes a lie, so refreshing it is one command.
#
#   tools/sync-from-team.sh          # show what changed upstream, change nothing
#   tools/sync-from-team.sh --apply  # copy upstream into web/ and the plugin
#   tools/sync-from-team.sh --apply --force   # even if this repo is ahead
#
# Upstream wins for those two folders. A change we want to keep has to go
# upstream first, otherwise the next sync overwrites it. That rule exists
# because two copies drifting apart is exactly how the plugin ended up three
# versions behind the app.
#
# Because of that, a part whose version here is *newer* than upstream is skipped
# rather than overwritten — that means real work lives only in this repository.
# The other part still syncs. --force overrides the skip.

set -euo pipefail

UPSTREAM="${UPSTREAM:-https://github.com/soldiermr98-commits/mrdb.git}"
BRANCH="${BRANCH:-main}"
APPLY=0
FORCE=0

for arg in "$@"; do
    case "$arg" in
        --apply) APPLY=1 ;;
        --force) FORCE=1 ;;
        -h|--help) sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "unknown argument: $arg" >&2; exit 2 ;;
    esac
done

root="${SYNC_ROOT:-$(git rev-parse --show-toplevel)}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp" 2>/dev/null || true' EXIT

# `rm -rf` is not always allowed (sandboxed shells, some corporate policies).
# Fall back to Python's shutil rather than failing halfway through a sync.
wipe() {
    [ -e "$1" ] || return 0
    rm -rf "$1" 2>/dev/null || true
    [ -e "$1" ] || return 0
    local py
    py="$(command -v python3 || command -v python || true)"
    if [ -n "$py" ]; then
        "$py" -c 'import shutil,sys; shutil.rmtree(sys.argv[1], ignore_errors=True)' "$1"
    fi
    if [ -e "$1" ]; then
        echo "could not remove $1" >&2
        exit 1
    fi
}

echo "fetching $UPSTREAM ($BRANCH)…"
git clone --quiet --depth 1 --branch "$BRANCH" "$UPSTREAM" "$tmp/team"
rev="$(git -C "$tmp/team" rev-parse --short HEAD)"
subject="$(git -C "$tmp/team" log -1 --pretty=%s | cut -c1-100)"
echo "upstream $rev — $subject"
echo

# ── what differs ────────────────────────────────────────────────────────────
report() {
    local label="$1" a="$2" b="$3"
    if [ ! -d "$a" ]; then
        echo "  $label: not present here yet"
        return
    fi
    local out
    out="$(diff -rq "$a" "$b" 2>/dev/null | head -n 40 || true)"
    if [ -z "$out" ]; then
        echo "  $label: identical"
    else
        echo "  $label:"
        printf '%s\n' "$out" | sed 's/^/    /'
    fi
}

echo "differences:"
report "web/"        "$root/web" "$tmp/team"
report "plugin"      "$root/wordpress-plugin/mrdastbaaf-bridge" "$tmp/team/wordpress-plugin/mrdastbaaf-bridge"
echo

if [ "$APPLY" -eq 0 ]; then
    echo "nothing written. Re-run with --apply to take upstream's version."
    exit 0
fi

# ── guards ──────────────────────────────────────────────────────────────────
#
# A blind copy is a downgrade waiting to happen: this repository's plugin was
# three versions ahead of upstream when the snapshot was taken, and syncing
# would have deleted that work with no warning. Compare versions and refuse.

# version_gt A B → true when A is newer than B (GNU sort -V; silent if absent).
version_gt() {
    [ "$1" = "$2" ] && return 1
    command -v sort >/dev/null || return 1
    [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | tail -n 1)" = "$1" ]
}

plugin_main="wordpress-plugin/mrdastbaaf-bridge/mrdastbaaf-bridge.php"
read_version() {
    sed -n "s/.*MRDASTBAAF_BRIDGE_VERSION', '\([^']*\)'.*/\1/p" "$1" 2>/dev/null | head -n 1 | tr -d '\r'
}

local_plugin="$(read_version "$root/$plugin_main")"
up_plugin="$(read_version "$tmp/team/$plugin_main")"

# A part that is *ahead* of upstream is skipped, not treated as a fatal error:
# the other part may still be behind and worth syncing. Deleting newer work
# silently is the thing to avoid, and skipping is how to avoid it.
sync_plugin=1
sync_web=1

if [ -n "$local_plugin" ] && [ -n "$up_plugin" ] && version_gt "$local_plugin" "$up_plugin"; then
    if [ "$FORCE" -eq 1 ]; then
        echo "warning: --force given; discarding plugin $local_plugin for upstream $up_plugin." >&2
    else
        sync_plugin=0
        echo "skipping the plugin: this repository has $local_plugin, upstream has $up_plugin." >&2
        echo "  Those $local_plugin changes exist only here. Push them upstream, or pass --force to discard." >&2
    fi
fi

web_here="$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' "$root/web/package.json" 2>/dev/null | head -n 1)"
web_up="$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' "$tmp/team/package.json" 2>/dev/null | head -n 1)"

if [ -n "$web_here" ] && [ -n "$web_up" ] && version_gt "$web_here" "$web_up"; then
    if [ "$FORCE" -eq 1 ]; then
        echo "warning: --force given; discarding web app $web_here for upstream $web_up." >&2
    else
        sync_web=0
        echo "skipping the web app: this repository has $web_here, upstream has $web_up." >&2
        echo "  Push those changes upstream, or pass --force to discard." >&2
    fi
fi

if [ "$sync_plugin" -eq 0 ] && [ "$sync_web" -eq 0 ]; then
    echo >&2
    echo "nothing to sync: both parts are ahead of upstream." >&2
    exit 1
fi

# ── apply ───────────────────────────────────────────────────────────────────
echo "applying…"

if [ "$sync_web" -eq 1 ]; then
    wipe "$root/web"
    mkdir -p "$root/web"
    cp -a "$tmp/team/." "$root/web/"
    wipe "$root/web/.git"
    wipe "$root/web/wordpress-plugin"
    echo "web: updated"
else
    echo "web: left alone"
fi

if [ "$sync_plugin" -eq 1 ]; then
    wipe "$root/wordpress-plugin/mrdastbaaf-bridge"
    cp -a "$tmp/team/wordpress-plugin/mrdastbaaf-bridge" "$root/wordpress-plugin/"
    echo "plugin: updated"
else
    echo "plugin: left alone"
fi

echo "done. working tree:"
git -C "$root" status --short | head -n 40
echo
echo "Review, then commit:"
echo "  git add -A && git commit -m \"chore(sync): team upstream $rev\""
