# Legacy patches — kept for the record, not for use

These patches are **not applicable** and must not be applied.

They were written against an unreleased `1.8.0` line that only ever existed on a
local branch of the team repository. The released plugin was `1.7.15` at the
time, so `git am` stops on the first hunk. Everything still wanted from them has
to be rebuilt on the current code in
[`wordpress-plugin/mrdastbaaf-bridge/`](../../wordpress-plugin/mrdastbaaf-bridge).

They are kept because they contain the only written design for features that
were never built — most importantly **expiring signed video URLs**, which is the
server half of "the videos cannot be downloaded".

| Patch | What it describes | Status |
|---|---|---|
| `0001` | Multi-device Bearer tokens | superseded — the plugin still has one token slot per user |
| `0002` | `GET/PUT /lessons/{id}/progress` — playback position | **not built**; blocks resume |
| `0003` | `videoType` on playback responses, `watermarkFullName` | **not built** |
| `0004` | Expiring signed segment URLs via a PHP gateway | **not built**; the real content protection |
| `0005` | Restrict `GET /auth/health` in production, `GET /payment/order/{id}` | partly wanted |
| `0006` | Shop category tree (`parent`, `count`) | **shipped** — plugin 1.7.16 and later |
| `0007` | `durationDays` on plans, hide the tier field | **shipped** — plugin 1.7.17 and later |

So: `0006` and `0007` are already in the plugin and can be ignored entirely. The
rest are specifications to implement, not patches to apply.

How the plugin is actually released now:
[`tools/release-plugin.sh`](../../tools/release-plugin.sh).
