"""由 tools/icon_source.png 生成各密度启动图标与 TV banner。运行: python3 tools/gen_icons.py"""
from PIL import Image, ImageDraw
import os
here = os.path.dirname(os.path.abspath(__file__))
res = os.path.join(here, "..", "app", "src", "main", "res")
src = Image.open(os.path.join(here, "icon_source.png")).convert("RGBA")
tile = src.crop((41, 41, 471, 471))  # 去掉外围透明边
dens = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

def rounded(img, r):
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, img.size[0]-1, img.size[1]-1), r, fill=255)
    out = img.copy(); out.putalpha(m); return out

for d, s in dens.items():
    p = os.path.join(res, f"mipmap-{d}"); os.makedirs(p, exist_ok=True)
    t = tile.resize((s, s), Image.LANCZOS)
    rounded(t, int(s*0.21)).save(os.path.join(p, "ic_launcher.png"))
    c = tile.resize((s, s), Image.LANCZOS)
    m = Image.new("L", (s, s), 0); ImageDraw.Draw(m).ellipse((0, 0, s-1, s-1), fill=255)
    c.putalpha(m); c.save(os.path.join(p, "ic_launcher_round.png"))

# 自适应图标前景：图标主体缩放到安全区
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
t = tile.resize((300, 300), Image.LANCZOS)
fg.paste(rounded(t, 64), (66, 66))
os.makedirs(os.path.join(res, "drawable-nodpi"), exist_ok=True)
fg.save(os.path.join(res, "drawable-nodpi", "ic_launcher_fg.png"))

# TV banner 320x180
b = Image.new("RGBA", (320, 180), (11, 16, 32, 255))
b.paste(rounded(tile.resize((140, 140), Image.LANCZOS), 30), (90, 20), rounded(tile.resize((140, 140), Image.LANCZOS), 30))
b.convert("RGB").save(os.path.join(res, "drawable-nodpi", "banner.png"))
