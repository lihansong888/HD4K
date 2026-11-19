#!/usr/bin/env python3
"""Regenerate every Android launcher/logo/banner raster from icon_source.png.

Usage (from anywhere):  python3 other/branding/gen_icons.py
Needs Pillow and numpy.  Writes straight into app/src/*/res.

icon_source.png is a 512x512 image: a rounded-square "tile" on a dark
background.  The tile is cut out (anti-aliased rounded-rect mask) and reused
for the legacy icons, the adaptive-icon foreground, the in-app logo and the
TV banner.
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SRC = HERE / "icon_source.png"
MAIN = ROOT / "app/src/main/res"
LEAN = ROOT / "app/src/leanback/res"

APP_NAME = "高清4K影视"
FONT = ("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc", 2)  # Noto Sans CJK SC Bold

# Tile geometry measured on the 512px source (rim highlight at 41..471, corner radius ~92).
TILE_BOX = (41, 41, 471, 471)
TILE_RADIUS = 92

# Background colours sampled from the source corners (TL, TR, BL, BR).
CORNERS = [(24, 40, 78), (29, 23, 62), (11, 20, 45), (18, 11, 38)]


def rounded_mask(size, radius, ss=4):
    big = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(big).rounded_rectangle((0, 0, size * ss - 1, size * ss - 1), radius=radius * ss, fill=255)
    return big.resize((size, size), Image.LANCZOS)


def make_tile():
    src = Image.open(SRC).convert("RGBA")
    tile = src.crop(TILE_BOX)
    tile.putalpha(rounded_mask(tile.width, TILE_RADIUS))
    return tile


def circle_of(tile):
    out = tile.copy()
    mask = Image.new("L", (tile.width * 4,) * 2, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, tile.width * 4 - 1, tile.height * 4 - 1), fill=255)
    mask = mask.resize(tile.size, Image.LANCZOS)
    alpha = np.minimum(np.asarray(out.getchannel("A")), np.asarray(mask))
    out.putalpha(Image.fromarray(alpha))
    return out


def fit(img, size):
    return img.resize((size, size), Image.LANCZOS)


def centered(canvas_size, img):
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    off = (canvas_size - img.width) // 2
    canvas.alpha_composite(img, (off, off))
    return canvas


def gradient(w, h):
    tl, tr, bl, br = [np.array(c, dtype=float) for c in CORNERS]
    x = np.linspace(0, 1, w)[None, :, None]
    y = np.linspace(0, 1, h)[:, None, None]
    arr = (tl * (1 - x) + tr * x) * (1 - y) + (bl * (1 - x) + br * x) * y
    return Image.fromarray(arr.round().astype("uint8"), "RGB").convert("RGBA")


def save_png(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", path.relative_to(ROOT), img.size)


def main():
    tile = make_tile()
    round_tile = circle_of(tile)

    # Legacy (API < 26) icons: square tile + round variant.
    for dpi, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        save_png(centered(px, fit(tile, round(px * 0.92))), MAIN / f"mipmap-{dpi}" / "ic_launcher.png")
        rpath = MAIN / f"mipmap-{dpi}" / "ic_launcher_round.webp"
        centered(px, fit(round_tile, round(px * 0.96))).save(rpath, "WEBP", lossless=True, quality=100)
        print("wrote", rpath.relative_to(ROOT), (px, px))

    # Adaptive foreground: 108dp layer at 8x, tile = 58dp so it survives a circular mask.
    save_png(centered(864, fit(tile, 464)), MAIN / "drawable-nodpi" / "ic_launcher_foreground.png")

    # In-app logo (48dp on TV, 36dp on mobile).
    save_png(fit(tile, 192), MAIN / "drawable-nodpi" / "ic_logo.png")

    # TV banner 320x180dp @ xhdpi.
    w, h = 640, 360
    banner = gradient(w, h)
    icon_px, gap, margin = 190, 28, 36
    size = 84
    while True:
        font = ImageFont.truetype(FONT[0], size, index=FONT[1])
        text_w = font.getlength(APP_NAME)
        if icon_px + gap + text_w <= w - 2 * margin or size <= 30:
            break
        size -= 2
    total = icon_px + gap + text_w
    x0 = round((w - total) / 2)
    banner.alpha_composite(fit(tile, icon_px), (x0, (h - icon_px) // 2))
    draw = ImageDraw.Draw(banner)
    ascent, descent = font.getmetrics()
    draw.text((x0 + icon_px + gap, (h - (ascent + descent)) / 2), APP_NAME, font=font, fill=(255, 255, 255, 255))
    save_png(banner.convert("RGB"), LEAN / "mipmap-xhdpi" / "ic_banner.png")


if __name__ == "__main__":
    main()
