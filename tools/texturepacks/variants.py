"""Erzeugt drei Lite Packs aus den Minecraft-Blocktexturen: Neon, Flat und Pastell.

Aufruf: python tools/texturepacks/variants.py [pfad/zur/minecraft-client.jar]
Ausgabe: src/main/resources/performance-packs/{neon_lite,flat_lite,pastel_lite}/

- Neon:    dunkle Bloecke mit leuchtenden Kanten in der Farbe des Blocks (graue Bloecke: Cyan)
- Flat:    jeder Block einfarbig (Durchschnittsfarbe) mit leichter Kante, 8x8, sehr FPS-freundlich
- Pastell: weiche, helle Farben

Animierte Blocktexturen (Wasser, Lava, Feuer ...) werden zu einem stehenden Bild, das spart Leistung.
"""
import colorsys
import io
import json
import os
import shutil
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'src' / 'main' / 'resources' / 'performance-packs'
DEFAULT_JAR = Path.home() / '.gradle' / 'caches' / 'fabric-loom' / '1.21.11' / 'minecraft-client.jar'
PREFIX = 'assets/minecraft/textures/block/'
SKIP = ('destroy_stage_',)  # Abbau-Risse bleiben wie in Minecraft


def first_frame(img):
    if img.height > img.width and img.height % img.width == 0:
        return img.crop((0, 0, img.width, img.width))
    return img


def average(img):
    r = g = b = n = 0
    for (pr, pg, pb, pa) in img.getdata():
        if pa > 16:
            r += pr
            g += pg
            b += pb
            n += 1
    if n == 0:
        return (128, 128, 128)
    return (r // n, g // n, b // n)


def hsv(rgb):
    return colorsys.rgb_to_hsv(*(c / 255 for c in rgb))


def rgb(h, s, v):
    return tuple(int(round(c * 255)) for c in colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v))))


def accent_mask(img):
    """Bunte Flecken auf grauem Grund (z. B. Erze): diese Pixel bekommen eine eigene Farbe, sonst sieht Erz wie Stein aus."""
    w, h = img.size
    data = img.load()
    sats = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = data[x, y]
            sats.append(colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)[1] if a > 16 else 0)
    avg_sat = sum(sats) / max(1, len(sats))
    if avg_sat > 0.3:
        return None, None
    mask = [s > 0.35 for s in sats]
    count = sum(mask)
    if count < 3 or count > len(mask) * 0.6:
        return None, None
    r = g = b = 0
    for i, m in enumerate(mask):
        if m:
            pr, pg, pb, _ = data[i % w, i // w]
            r, g, b = r + pr, g + pg, b + pb
    return mask, (r // count, g // count, b // count)


def has_alpha(img):
    return img.getextrema()[3][0] < 255


# ---- Neon ----
def neon(img):
    img = img.convert('RGBA')
    w, h = img.size
    hh, ss, vv = hsv(average(img))
    if ss < 0.15:
        hh, ss = 0.5, 0.75  # graue Bloecke bekommen cyanfarbene Kanten
    fill = rgb(hh, ss * 0.6, 0.11)
    glow = rgb(hh, ss * 0.8, 0.35)
    edge = rgb(hh, max(ss, 0.75), 1.0)
    src = img.load()
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = out.load()
    cut = has_alpha(img)
    mask, spot = accent_mask(img)
    if spot is not None:
        sh, sss, _ = hsv(spot)
        spot_color = rgb(sh, max(sss, 0.6), 1.0)

    def solid(x, y):
        return 0 <= x < w and 0 <= y < h and src[x, y][3] > 16

    for y in range(h):
        for x in range(w):
            if not solid(x, y):
                continue
            a = src[x, y][3]
            if cut:
                border = not (solid(x - 1, y) and solid(x + 1, y) and solid(x, y - 1) and solid(x, y + 1))
                px[x, y] = (edge if border else fill) + (a,)
            else:
                ring = min(x, y, w - 1 - x, h - 1 - y)
                c = edge if ring == 0 else glow if ring == 1 else fill
                if mask is not None and ring > 0 and mask[y * w + x]:
                    c = spot_color
                px[x, y] = c + (a,)
    return out


# ---- Flat ----
def flat(img):
    img = img.convert('RGBA')
    n = 8
    small = img.resize((n, n), Image.NEAREST if has_alpha(img) else Image.BOX)
    base = average(img)
    mask, spot = accent_mask(img)
    cells = None
    if mask is not None:
        sx, sy = img.width / n, img.height / n
        cells = [[False] * n for _ in range(n)]
        for cy in range(n):
            for cx in range(n):
                hits = total = 0
                for yy in range(int(cy * sy), int((cy + 1) * sy)):
                    for xx in range(int(cx * sx), int((cx + 1) * sx)):
                        total += 1
                        hits += mask[yy * img.width + xx]
                cells[cy][cx] = total > 0 and hits / total >= 0.4
    out = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    px = out.load()
    sp = small.load()
    for y in range(n):
        for x in range(n):
            a = sp[x, y][3]
            if a <= 16:
                continue
            d = 14 if y == 0 else -14 if y == n - 1 else 0
            color = spot if cells is not None and cells[y][x] else base
            px[x, y] = tuple(max(0, min(255, c + d)) for c in color) + (255 if a > 128 else a,)
    return out


# ---- Pastell ----
def pastel(img):
    img = img.convert('RGBA')
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            c = rgb(hh, ss * 0.55, 0.55 + vv * 0.45)
            px[x, y] = tuple(int(c[i] * 0.8 + 255 * 0.2) for i in range(3)) + (a,)
    return out


PACKS = {
    'neon_lite': ('Neon Lite', 'Dunkle Blöcke, leuchtende Kanten', neon),
    'flat_lite': ('Flat Lite', 'Einfarbig & schnell (8x8)', flat),
    'pastel_lite': ('Pastell Lite', 'Weiche Pastellfarben', pastel),
}


def tint(img, color):
    """Fuer die Vorschau: graue Biom-Texturen (Gras, Blaetter) wie im Spiel einfaerben."""
    img = img.convert('RGBA')
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * color[0] // 255, g * color[1] // 255, b * color[2] // 255, a)
    return img


def preview(textures, fn):
    tiles = [('grass_block_top.png', (124, 189, 107)), ('obsidian.png', None), ('diamond_ore.png', None), ('redstone_ore.png', None)]
    out = Image.new('RGBA', (64, 64), (0, 0, 0, 255))
    for i, (name, color) in enumerate(tiles):
        t = fn(first_frame(textures[name]))
        if color:
            t = tint(t, color)
        out.paste(t.resize((32, 32), Image.NEAREST), ((i % 2) * 32, (i // 2) * 32))
    return out


def main():
    jar = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_JAR
    textures = {}
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.startswith(PREFIX) and name.endswith('.png') and '/' not in name[len(PREFIX):]:
                file = name[len(PREFIX):]
                if file.startswith(SKIP):
                    continue
                textures[file] = Image.open(io.BytesIO(z.read(name))).convert('RGBA')
    print(len(textures), 'Blocktexturen gelesen')
    for pid, (title, desc, fn) in PACKS.items():
        root = OUT / pid
        if root.exists():
            shutil.rmtree(root)
        target = root / 'assets' / 'minecraft' / 'textures' / 'block'
        target.mkdir(parents=True)
        for file, img in textures.items():
            fn(first_frame(img)).save(target / file, optimize=True)
        preview(textures, fn).save(root / 'preview.png', optimize=True)
        with open(root / 'style.json', 'w', encoding='utf-8') as f:
            json.dump({'name': title, 'description': desc}, f, ensure_ascii=False)
        size = sum(p.stat().st_size for p in root.rglob('*') if p.is_file())
        print(f'{pid:12s} {len(textures)} Texturen, {size // 1024} KB')


if __name__ == '__main__':
    main()
