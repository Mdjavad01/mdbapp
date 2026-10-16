"""Generate the mdbapp launcher icons.

Run from the repository root:

    python scripts/generate_launcher_icons.py

The motif is a woven diamond — a deliberately simple geometric mark that stays
legible at 48 px and echoes the brand's loom/handicraft identity. Replace the
artwork when the brand team supplies the real asset; keep the output paths so
nothing else in the project has to change.
"""

from __future__ import annotations

import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

BRAND = (154, 52, 18, 255)      # #9a3412 — terracotta, the PWA theme colour
CREAM = (253, 251, 247, 255)    # #fdfbf7 — the PWA background colour
ACCENT = (217, 119, 6, 255)     # #d97706 — amber accent

# Legacy launcher icons (API 24-25 and any launcher that ignores adaptive icons)
LEGACY = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Adaptive icon foreground: 108dp canvas, the logo must sit inside the
# inner 66dp safe zone (i.e. roughly the middle 61% of the canvas).
FOREGROUND = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def draw_motif(draw: ImageDraw.ImageDraw, size: int, colour, stroke: int) -> None:
    """Draw the woven-diamond mark centred in a `size` x `size` canvas."""
    cx = cy = size / 2

    # Outer diamond
    outer = size * 0.30
    draw.polygon(
        [(cx, cy - outer), (cx + outer, cy), (cx, cy + outer), (cx - outer, cy)],
        outline=colour,
        width=stroke,
    )

    # Inner diamond
    inner = size * 0.15
    draw.polygon(
        [(cx, cy - inner), (cx + inner, cy), (cx, cy + inner), (cx - inner, cy)],
        outline=colour,
        width=max(1, stroke // 2),
    )

    # Four warp threads at the compass points — the "loom" cue
    tick = size * 0.055
    gap = size * 0.345
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        draw.line(
            [(cx + dx * gap, cy + dy * gap), (cx + dx * (gap + tick), cy + dy * (gap + tick))],
            fill=colour,
            width=stroke,
        )


def make_legacy(size: int, rounded: bool) -> Image.Image:
    scale = 4  # supersample for smooth edges
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if rounded:
        draw.ellipse([0, 0, s - 1, s - 1], fill=BRAND)
    else:
        draw.rounded_rectangle([0, 0, s - 1, s - 1], radius=s * 0.22, fill=BRAND)

    draw_motif(draw, s, CREAM, max(1, int(s * 0.028)))
    return img.resize((size, size), Image.LANCZOS)


def make_foreground(size: int) -> Image.Image:
    scale = 4
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Inside the adaptive canvas the logo occupies the safe zone, so the motif
    # is drawn at a smaller relative size than on the legacy icon.
    draw_motif(draw, s, CREAM, max(1, int(s * 0.022)))
    return img.resize((size, size), Image.LANCZOS)


def write(img: Image.Image, folder: str, name: str) -> None:
    path = os.path.join(RES, folder)
    os.makedirs(path, exist_ok=True)
    out = os.path.join(path, name)
    img.save(out, "PNG", optimize=True)
    print(f"  {folder}/{name}  {img.width}x{img.height}")


def main() -> None:
    print("legacy launcher icons:")
    for density, size in LEGACY.items():
        write(make_legacy(size, rounded=False), f"mipmap-{density}", "ic_launcher.png")
        write(make_legacy(size, rounded=True), f"mipmap-{density}", "ic_launcher_round.png")

    print("adaptive icon foregrounds:")
    for density, size in FOREGROUND.items():
        write(make_foreground(size), f"mipmap-{density}", "ic_launcher_foreground.png")

    print("done.")


if __name__ == "__main__":
    main()
