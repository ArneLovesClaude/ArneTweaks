"""Builds low-end ("Bambus-PC") theme packs from the Frost base textures."""
import os, json, shutil, colorsys, zipfile
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'build', 'texturepacks'))
BASE = os.path.join(ROOT, 'FrostPvP', 'assets', 'minecraft', 'textures')
OUTDIR = os.path.join(ROOT, 'performance')

# colors that define the Frost theme (GUI, grips, guards, glass, accents)
THEME_COLORS = ['#0b1e36', '#4a7fb8', '#8cc2ef', '#7cc4f5', '#bfe3ff', '#f4faff', '#0f2238', '#1c3352',
                '#2e5f94', '#1a2a40', '#cfe6f7', '#121c29', '#0a111a', '#e9f6ff', '#dff2ff', '#7aa6c9', '#5b86ad',
                '#b9d7ee', '#dcecf8', '#f2f8fd', '#eafbff', '#7cf3ff', '#1fa3c8']
FULL_SHIFT = ['gui/sprites/hud/experience_bar_progress.png', 'gui/sprites/hud/experience_bar_background.png',
              'misc/enchanted_glint_item.png', 'misc/enchanted_glint_armor.png', 'block/respawn_anchor_top.png',
              'block/soul_fire_0.png', 'block/soul_fire_1.png']

THEMES = {
    # id: (name, hue in degrees (None = keep), saturation factor, value factor, description)
    'frost_lite':    ('Frost Lite', None, 1.0, 1.0, 'Eisblau & clean'),
    'sakura_lite':   ('Sakura Lite', 330, 0.9, 1.0, 'Pink & soft'),
    'ember_lite':    ('Ember Lite', 18, 1.0, 1.0, 'Orange-Rot & warm'),
    'venom_lite':    ('Venom Lite', 120, 0.9, 0.95, 'Giftgrün'),
    'midnight_lite': ('Midnight Lite', 270, 0.8, 0.85, 'Dunkel-Lila'),
    'gold_lite':     ('Gold Lite', 45, 1.0, 1.0, 'Gold & edel'),
    'aqua_lite':     ('Aqua Lite', 175, 1.0, 1.0, 'Türkis'),
    'mono_lite':     ('Mono Lite', None, 0.0, 1.0, 'Schwarz-Weiß, minimal'),
    # Version 3.2.0
    'crimson_lite':  ('Crimson Lite', 355, 1.1, 0.85, 'Blutrot & aggressiv'),
    'ocean_lite':    ('Ocean Lite', 225, 1.0, 0.8, 'Tiefblau'),
    'lime_lite':     ('Lime Lite', 85, 1.1, 1.05, 'Neon-Limette'),
    'lavender_lite': ('Lavender Lite', 285, 0.55, 1.05, 'Pastell-Lila'),
    'mint_lite':     ('Mint Lite', 150, 0.6, 1.05, 'Pastell-Mint'),
    'noir_lite':     ('Noir Lite', None, 0.0, 0.6, 'Dunkel & schlicht'),
    'cyber_lite':    ('Cyber Lite', 305, 1.15, 1.0, 'Neon-Pink'),
    'copper_lite':   ('Copper Lite', 25, 0.8, 0.8, 'Kupfer & warm'),
}

ANIMATED = {'water_still', 'water_flow', 'lava_still', 'lava_flow', 'fire_0', 'fire_1', 'soul_fire_0',
            'soul_fire_1', 'respawn_anchor_top'}

# extra vanilla textures that are animated -> static flat replacements (8x8)
STATIC_EXTRA = {
    'nether_portal': ('#8a3ad6', 170), 'sea_lantern': ('#d8f0ea', 255), 'magma': ('#b8461c', 255),
    'prismarine': ('#5fa89a', 255), 'crimson_stem': ('#7a2a3a', 255), 'warped_stem': ('#2a6a66', 255),
    'blast_furnace_front_on': ('#5a5d63', 255), 'smoker_front_on': ('#5a4a3a', 255), 'sculk': ('#0f2a33', 255),
}
BLADES = {'kelp': '#4b8a3a', 'kelp_plant': '#4b8a3a', 'seagrass': '#5aa04a', 'tall_seagrass_top': '#5aa04a',
          'tall_seagrass_bottom': '#5aa04a'}


def hexrgb(h):
    h = h.lstrip('#'); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def recolor(rgb, hue, sat, val):
    r, g, b = [c / 255 for c in rgb]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    if hue is not None:
        h = (h + (hue - 205) / 360) % 1.0  # frost accents sit around hue 205
    s = min(1, s * sat); v = min(1, v * val)
    return tuple(int(round(c * 255)) for c in colorsys.hsv_to_rgb(h, s, v))


def theme_image(img, mapping, full, hue, sat, val):
    img = img.convert('RGBA')
    px = img.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if full:
                px[x, y] = recolor((r, g, b), hue, sat, val) + (a,)
            elif (r, g, b) in mapping:
                px[x, y] = mapping[(r, g, b)] + (a,)
    return img


def flat(c, alpha=255, n=8):
    img = Image.new('RGBA', (n, n), hexrgb(c) + (alpha,))
    for i in range(n):  # tiny bevel so it is not fully flat
        img.putpixel((i, 0), tuple(min(255, v + 18) for v in hexrgb(c)) + (alpha,))
        img.putpixel((i, n - 1), tuple(max(0, v - 18) for v in hexrgb(c)) + (alpha,))
    return img


def blades(c, n=8):
    img = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    for x in (1, 3, 5, 6):
        for y in range(n):
            if y >= (x % 3):
                img.putpixel((x, y), hexrgb(c) + (255,))
    return img


def build_theme(tid, name, hue, sat, val, desc):
    root = os.path.join(OUTDIR, tid)
    tex = os.path.join(root, 'assets', 'minecraft', 'textures')
    if os.path.exists(root):
        shutil.rmtree(root)
    mapping = {hexrgb(c): recolor(hexrgb(c), hue, sat, val) for c in THEME_COLORS}
    for dp, _, fs in os.walk(BASE):
        for f in fs:
            if not f.endswith('.png'):
                continue
            src = os.path.join(dp, f)
            rel = os.path.relpath(src, BASE)
            img = Image.open(src).convert('RGBA')
            stem = f[:-4]
            # 1) kill animations: keep only first frame
            if stem in ANIMATED and img.height > img.width:
                img = img.crop((0, 0, img.width, img.width))
            # 2) themed recolor
            full = rel in FULL_SHIFT or (hue is None and sat == 0)
            if hue is not None or sat != 1 or val != 1:
                img = theme_image(img, mapping, full, hue, sat, val)
            # 3) blocks -> 8x8 (half the memory, faster atlas stitching)
            if rel.startswith('block/') and img.size == (16, 16):
                img = img.resize((8, 8), Image.BOX if img.getextrema()[3][0] == 255 else Image.NEAREST)
            elif rel.startswith('block/') and img.size == (32, 32):
                img = img.resize((8, 8), Image.BOX)
            dst = os.path.join(tex, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            img.save(dst, optimize=True)
            # gui nine-slice metadata must stay
            if os.path.exists(src + '.mcmeta') and stem not in ANIMATED:
                shutil.copy(src + '.mcmeta', dst + '.mcmeta')
    for stem, (c, a) in STATIC_EXTRA.items():
        flat(c, a).save(os.path.join(tex, 'block', stem + '.png'), optimize=True)
    for stem, c in BLADES.items():
        blades(c).save(os.path.join(tex, 'block', stem + '.png'), optimize=True)
    # icon
    icon = Image.open(os.path.join(ROOT, 'FrostPvP', 'pack.png')).convert('RGBA')
    if hue is not None or sat != 1:
        icon = theme_image(icon, {}, True, hue, sat, val)
    icon.resize((64, 64), Image.BOX).save(os.path.join(root, 'pack.png'), optimize=True)
    with open(os.path.join(root, 'pack.mcmeta'), 'w', encoding='utf-8') as f:
        json.dump({'pack': {'description': f'§b{name} §7· Performance · {desc}',
                            'min_format': [75, 0], 'max_format': [75, 0]}}, f, ensure_ascii=False, indent=2)
    zpath = os.path.join(OUTDIR, f'ArneTweaks_{name.replace(" ", "")}.zip')
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for dp, _, fs in os.walk(root):
            for f in fs:
                p = os.path.join(dp, f)
                z.write(p, os.path.relpath(p, root))
    return zpath


if __name__ == '__main__':
    os.makedirs(OUTDIR, exist_ok=True)
    for tid, (name, hue, sat, val, desc) in THEMES.items():
        z = build_theme(tid, name, hue, sat, val, desc)
        print(f'{os.path.basename(z):32s} {os.path.getsize(z) // 1024} KB')
