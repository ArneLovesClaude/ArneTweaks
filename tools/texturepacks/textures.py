import os, json, math, random
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'build', 'texturepacks')), 'FrostPvP')
TEX = os.path.join(OUT, 'assets', 'minecraft', 'textures')
rng = random.Random(1337)


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def save(img, rel, mcmeta=None):
    p = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    if mcmeta:
        with open(p + '.mcmeta', 'w') as f:
            json.dump(mcmeta, f, indent=2)


def shift(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


# ---------------------------------------------------------------- palettes
TIERS = {
    'wooden':    ['#3b2a1a', '#8a6a45', '#b8935f', '#d8b885', '#f0dcb5'],
    'stone':     ['#23272e', '#5c6470', '#808a96', '#a7b0bb', '#cdd4dc'],
    'copper':    ['#3a1c10', '#a3542f', '#d9784a', '#f0a070', '#ffd2b0'],
    'iron':      ['#2a2f38', '#8f9aa8', '#c4ccd6', '#e3e9ef', '#ffffff'],
    'golden':    ['#4a3510', '#c98f1c', '#f0c040', '#ffe17a', '#fff6c8'],
    'diamond':   ['#0b2a44', '#2b8fd0', '#5cc8f2', '#a9e8ff', '#eafbff'],
    'netherite': ['#100d18', '#2e2a3a', '#4a4560', '#6e6888', '#9a94b5'],
    'chainmail': ['#22262d', '#6d7682', '#9aa3ae', '#c2c9d1', '#e4e8ec'],
}
NAVY = hx('#0b1e36'); GRIP = hx('#4a7fb8'); GRIP_L = hx('#8cc2ef'); ICE = hx('#7cc4f5'); ICE_L = hx('#bfe3ff'); WHITE = hx('#f4faff')


def pal(t):
    o, s, b, l, h = [hx(c) for c in TIERS[t]]
    return {'O': o, 'S': s, 'B': b, 'L': l, 'H': h, 'h': GRIP, 'g': GRIP_L, 'p': ICE, 'G': ICE_L, 'N': NAVY}


def render(px, p, outline=True, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for (x, y), r in px.items():
        if 0 <= x < size and 0 <= y < size:
            img.putpixel((x, y), p[r] if isinstance(r, str) else r)
    if outline:
        o = p['O']
        filled = set(k for k in px if 0 <= k[0] < size and 0 <= k[1] < size)
        for (x, y) in list(filled):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y + dy)
                if 0 <= n[0] < size and 0 <= n[1] < size and n not in filled:
                    img.putpixel(n, o)
    return img


def shade_head(pts):
    s = [x + y for x, y in pts]
    lo, hi = min(s), max(s)
    out = {}
    for (x, y) in pts:
        t = (x + y - lo) / max(1, hi - lo)
        out[(x, y)] = 'H' if t < .15 else 'L' if t < .42 else 'B' if t < .78 else 'S'
    return out


def handle(px, n, start=(2, 14)):
    for i in range(n):
        px[(start[0] + i, start[1] - i)] = 'g' if i % 3 == 1 else 'h'


# ---------------------------------------------------------------- tools
def sword(t):
    px = {}
    for i in range(10):
        x, y = 5 + i, 10 - i
        px[(x, y)] = 'H' if i % 4 == 0 else 'L'
        if i <= 8:
            px[(x + 1, y)] = 'B'
            px[(x + 1, y + 1)] = 'S'
    for g in [(3, 9), (4, 10), (5, 11), (6, 12), (7, 13)]:
        px[g] = 'p'
    px[(5, 11)] = 'G'
    px[(4, 12)] = 'h'; px[(3, 13)] = 'g'; px[(2, 14)] = 'p'
    return render(px, pal(t))


def axe(t):
    px = {}
    handle(px, 11, (2, 14))
    rows = {1: (7, 10), 2: (6, 11), 3: (5, 11), 4: (5, 10), 5: (5, 9), 6: (6, 8)}
    head = [(x, y) for y, (a, b) in rows.items() for x in range(a, b + 1)]
    px.update(shade_head(head))
    for y, (a, b) in rows.items():
        px[(a, y)] = 'H'
    return render(px, pal(t))


def pickaxe(t):
    px = {}
    handle(px, 9, (2, 14))
    head = []
    cx, cy = 2.0, 13.0
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - cx, y - cy)
            ang = math.degrees(math.atan2(y - cy, x - cx))
            if 10.3 <= d <= 12.4 and -84 <= ang <= -6:
                head.append((x, y))
    px.update(shade_head(head))
    return render(px, pal(t))


def shovel(t):
    px = {}
    handle(px, 8, (2, 14))
    head = [(x, y) for x in range(16) for y in range(16) if (x - 11) ** 2 + (y - 4) ** 2 <= 9.5 or (x, y) in [(13, 1), (14, 2)]]
    px.update(shade_head(head))
    return render(px, pal(t))


def hoe(t):
    px = {}
    handle(px, 10, (2, 14))
    head = [(x, y) for y in (2, 3) for x in range(6, 13)] + [(12, 4), (6, 4)]
    px.update(shade_head(head))
    return render(px, pal(t))


# ---------------------------------------------------------------- armor icons
ARMOR = {
'helmet': """
................
................
................
....OOOOOOOO....
...OHLLLLLLBO...
..OHLBBBBBBBSO..
..OLBBBBBBBBSO..
..OLBOOOOOOBSO..
..OLBO....OBSO..
..OBSO....OSSO..
..OOOO....OOOO..
................
................
................
................
................""",
'chestplate': """
................
..OOO......OOO..
.OHLBOOOOOOBLSO.
.OLBBLLLLLLBBSO.
.OLBBBBBBBBBBSO.
.OOOBBBBBBBBOOO.
...OLBBBBBBSO...
...OLBBHHBBSO...
...OLBBBBBBSO...
...OLBBBBBBSO...
...OLBBBBBBSO...
...OBSSSSSSSO...
...OOOOOOOOOO...
................
................
................""",
'leggings': """
................
...OOOOOOOOOO...
...OHLLLLLLBO...
...OLBBBBBBSO...
...OLBBOOBBSO...
...OLBO..OBSO...
...OLBO..OLSO...
...OLBO..OLSO...
...OLBO..OLSO...
...OLBO..OLSO...
...OLBO..OLSO...
...OLBO..OLSO...
...OBSO..OBSO...
...OOOO..OOOO...
................
................""",
'boots': """
................
................
................
................
................
................
................
..OOOO....OOOO..
..OLBO....OLBO..
..OLBO....OLBO..
..OLBO....OLBO..
.OHLBO....OLBSO.
OLBBSO....OLBBSO
OOOOOO....OOOOOO
................
................""",
}


def from_mask(mask, p):
    rows = [r for r in mask.strip('\n').split('\n')]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c != '.':
                img.putpixel((x, y), p[c])
    return img


# ---------------------------------------------------------------- armor layers (64x32)
def face(img, x0, y0, w, h, p, mode='full'):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            lx, ly = x - x0, y - y0
            edge = lx == 0 or ly == 0 or lx == w - 1 or ly == h - 1
            c = p['S'] if edge else (p['L'] if ly == 1 else p['B'])
            if (lx + ly) % 7 == 0 and not edge:
                c = p['H']
            img.putpixel((x, y), c)


def box(img, u, v, w, h, d, p, rows=None, visor=False):
    """rows: (start,end) slice of side faces to paint (in height coords)."""
    r0, r1 = rows if rows else (0, h)
    faces_side = [(u, v + d, d), (u + d, v + d, w), (u + d + w, v + d, d), (u + 2 * d + w, v + d, w)]
    for i, (fx, fy, fw) in enumerate(faces_side):
        face(img, fx, fy + r0, fw, r1 - r0, p)
        if visor and i == 1:  # front of helmet -> open face
            for x in range(fx + 1, fx + fw - 1):
                for y in range(fy + 3, fy + h - 1):
                    img.putpixel((x, y), (0, 0, 0, 0))
    if r0 == 0:
        face(img, u + d, v, w, d, p)
    if r1 == h:
        face(img, u + d + w, v, w, d, p)


def armor_layers(t):
    p = pal(t)
    a = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    box(a, 0, 0, 8, 8, 8, p, visor=True)       # helmet
    box(a, 16, 16, 8, 12, 4, p)                 # chestplate body
    box(a, 40, 16, 4, 12, 4, p, rows=(0, 9))    # arms
    box(a, 0, 16, 4, 12, 4, p, rows=(7, 12))    # boots
    b = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    box(b, 16, 16, 8, 12, 4, p, rows=(7, 12))   # legging belt
    box(b, 0, 16, 4, 12, 4, p, rows=(0, 9))     # legs
    name = 'gold' if t == 'golden' else t
    save(a, f'entity/equipment/humanoid/{name}.png')
    save(b, f'entity/equipment/humanoid_leggings/{name}.png')


# ---------------------------------------------------------------- misc items
def circle_item(c, r, p, cx=7.5, cy=7.5):
    pts = [(x, y) for x in range(16) for y in range(16) if (x - cx) ** 2 + (y - cy) ** 2 <= r * r]
    px = shade_head(pts)
    return px


def ender_pearl():
    p = {'O': hx('#062c33'), 'S': hx('#0f6b73'), 'B': hx('#1fa3a8'), 'L': hx('#6fe0d9'), 'H': hx('#e8fffd')}
    px = circle_item(None, 5.6, p)
    return render(px, p)


def ender_eye():
    p = {'O': hx('#062c33'), 'S': hx('#0f6b73'), 'B': hx('#1fa3a8'), 'L': hx('#6fe0d9'), 'H': hx('#e8fffd'), 'N': NAVY}
    px = circle_item(None, 5.6, p)
    for y in range(5, 11):
        px[(7, y)] = 'N'; px[(8, y)] = 'N'
    return render(px, p)


def apple(colors):
    p = {k: hx(v) for k, v in zip('OSBLH', colors)}
    p['t'] = hx('#5a3a22'); p['l'] = hx('#7fd38a')
    px = circle_item(None, 5.4, p, 7.5, 9)
    px[(7, 3)] = 't'; px[(8, 2)] = 't'; px[(9, 2)] = 'l'; px[(10, 1)] = 'l'; px[(10, 2)] = 'l'
    return render(px, p)


def steak():
    p = {'O': hx('#2a140a'), 'S': hx('#6b3519'), 'B': hx('#965027'), 'L': hx('#b8703f'), 'H': hx('#f3e2c8')}
    pts = [(x, y) for x in range(16) for y in range(16) if ((x - 7.5) / 6.2) ** 2 + ((y - 8) / 4.6) ** 2 <= 1]
    px = shade_head(pts)
    for x in range(3, 13):
        px[(x, 5 if x < 8 else 6)] = 'H'
    return render(px, p)


def golden_carrot():
    p = pal('golden'); p['l'] = hx('#7fd38a')
    px = {}
    for i in range(9):
        x, y = 3 + i, 12 - i
        px[(x, y)] = 'L'; px[(x + 1, y)] = 'B'
        if i < 7:
            px[(x + 1, y + 1)] = 'S'
    px[(12, 2)] = 'l'; px[(13, 1)] = 'l'; px[(13, 3)] = 'l'; px[(14, 2)] = 'l'
    return render(px, p)


def potion(kind):
    """returns (bottle, overlay)"""
    glass = hx('#dff2ff', 200); glass_d = hx('#7cc4f5', 230); cork = hx('#c9a77c')
    r = 4.6 if kind == 'potion' else 5.0
    cy = 10 if kind == 'potion' else 10.5
    body = {(x, y) for x in range(16) for y in range(16) if (x - 7.5) ** 2 + (y - cy) ** 2 <= r * r}
    neck = {(x, y) for x in (6, 7, 8, 9) for y in range(3 if kind == 'potion' else 4, 7)}
    shape = body | neck
    bottle = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in shape:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in shape and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                bottle.putpixel(n, glass_d)
    for x in (7, 8):
        bottle.putpixel((x, 2 if kind == 'potion' else 3), cork)
    for (x, y) in [(5, int(cy) - 2), (5, int(cy) - 1), (6, int(cy) - 3)]:
        bottle.putpixel((x, y), glass)
    if kind == 'lingering_potion':
        for x in range(5, 11):
            bottle.putpixel((x, 6), glass_d)
    ov = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in body:
        if y >= cy - r + 2 and (x, y) not in [(5, int(cy) - 2), (5, int(cy) - 1), (6, int(cy) - 3)]:
            v = 255 if (x + y) % 5 else 225
            ov.putpixel((x, y), (v, v, v, 255))
    return bottle, ov


def bow(stage):
    p = {'O': NAVY, 'B': hx('#cfe6f7'), 'L': hx('#f4faff'), 'S': hx('#7aa6c9'), 'h': GRIP, 'g': GRIP_L,
         's': hx('#ffffff', 230), 'a': ICE, 'f': WHITE, 'w': hx('#9aa3ae')}
    px = {}
    for a in np.linspace(180, 270, 60):
        x = 13 + 11.8 * math.cos(math.radians(a)); y = 13 + 11.8 * math.sin(math.radians(a))
        px[(int(round(x)), int(round(y)))] = 'L' if a < 225 else 'B'
    px[(4, 4)] = 'h'; px[(5, 4)] = 'g'; px[(4, 5)] = 'g'
    pull = [None, 1, 2, 3][stage]
    end1, end2 = (13, 1), (1, 13)
    if pull is None:
        for i in range(1, 12):
            px.setdefault((13 - i, 1 + i), 's')
    else:
        m = (7 + pull, 7 + pull)
        for (a, b) in [(end1, m), (end2, m)]:
            n = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
            for k in range(n + 1):
                q = (round(a[0] + (b[0] - a[0]) * k / n), round(a[1] + (b[1] - a[1]) * k / n))
                px.setdefault(q, 's')
        for k in range(1, 8 + pull):
            px[(m[0] - k, m[1] - k)] = 'w'
        px[(m[0] - 8 - pull + 1, m[1] - 8 - pull + 1)] = 'a'
        px[(m[0] - 8 - pull + 2, m[1] - 8 - pull + 1)] = 'a'
        px[(m[0] - 8 - pull + 1, m[1] - 8 - pull + 2)] = 'a'
        px[(m[0] + 1, m[1])] = 'f'; px[(m[0], m[1] + 1)] = 'f'
    return render(px, p, outline=False)


def arrow():
    p = {'O': NAVY, 'w': hx('#c9d1da'), 'a': ICE, 'A': ICE_L, 'f': WHITE}
    px = {}
    for i in range(10):
        px[(3 + i, 12 - i)] = 'w'
    for q in [(12, 1), (13, 1), (14, 1), (14, 2), (14, 3), (13, 2)]:
        px[q] = 'a'
    px[(12, 2)] = 'A'
    for q in [(1, 12), (2, 12), (1, 13), (3, 13), (2, 14), (3, 14), (4, 13)]:
        px[q] = 'f'
    return render(px, p)


def gem(colors):
    p = {k: hx(v) for k, v in zip('OSBLH', colors)}
    pts = [(x, y) for x in range(16) for y in range(16) if abs(x - 7.5) + abs(y - 7.5) * 0.85 <= 6]
    return render(shade_head(pts), p)


def ingot(colors):
    p = {k: hx(v) for k, v in zip('OSBLH', colors)}
    pts = [(x, y) for y in range(5, 11) for x in range(2 + (10 - y) // 2, 14 - (y - 5) // 2)]
    px = shade_head(pts)
    for x in range(5, 12):
        px[(x, 6)] = 'H'
    return render(px, p)


def stick():
    p = {'O': NAVY, 'h': GRIP, 'g': GRIP_L}
    px = {}
    handle(px, 12, (2, 14))
    return render(px, p)


def snowball():
    p = {'O': hx('#5b86ad'), 'S': hx('#b9d7ee'), 'B': hx('#dcecf8'), 'L': hx('#f2f8fd'), 'H': hx('#ffffff')}
    return render(circle_item(None, 4.6, p), p)


def bucket(liquid=None):
    p = pal('iron')
    p['w'] = hx('#4aa3ff'); p['W'] = hx('#9fd2ff'); p['v'] = hx('#ff7a1a'); p['V'] = hx('#ffd27a')
    px = {}
    for y in range(5, 14):
        w = 5 - (y - 5) // 3
        for x in range(8 - w, 8 + w):
            px[(x, y)] = 'L' if x < 7 else 'B' if x < 10 else 'S'
    for x in range(3, 13):
        px[(x, 4)] = 'H'
    if liquid:
        for x in range(4, 12):
            px[(x, 5)] = 'w' if liquid == 'water' else 'v'
        px[(5, 5)] = 'W' if liquid == 'water' else 'V'
    return render(px, p)


def fishing_rod(cast=False):
    p = {'O': NAVY, 'h': GRIP, 'g': GRIP_L, 's': hx('#e9f2fa'), 'r': hx('#ff4d6a')}
    px = {}
    handle(px, 12, (2, 14))
    if not cast:
        for y in range(3, 13):
            px[(13, y)] = 's'
        px[(13, 13)] = 'r'
    return render(px, p, outline=False)


def flint_and_steel():
    p = pal('iron'); p['f'] = hx('#3a4250'); p['F'] = hx('#5c6678')
    px = {}
    for x in range(2, 9):
        px[(x, 10)] = 'L'; px[(x, 11)] = 'B'
    px[(2, 9)] = 'L'; px[(8, 9)] = 'B'; px[(2, 8)] = 'L'
    for x, y in [(9, 4), (10, 4), (11, 4), (9, 5), (10, 5), (11, 5), (12, 5), (10, 6), (11, 6), (12, 6)]:
        px[(x, y)] = 'F' if y == 4 else 'f'
    return render(px, p)


def totem():
    p = pal('golden'); p['e'] = hx('#2fd0a0')
    m = """
................
.....OOOOOO.....
....OHLLLLBO....
....OLeBBeSO....
....OLBBBBSO....
.OOOOLBSSBSOOOO.
OHLLLBBBBBBBBBSO
.OOOOLBBBBSOOOO.
....OLBBBBSO....
....OLBBBBSO....
....OLBOOBSO....
....OLBO.OSO....
....OBSO.OSO....
....OOOO.OOO....
................
................"""
    return from_mask(m, p)


def shield_tex():
    # entity/shield/shield_base_nopattern 64x64 : front face u1 v1 12x22, back 13..25
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    p = pal('diamond')
    def rect(x0, y0, w, h, c):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                img.putpixel((x, y), c)
    rect(0, 0, 28, 24, p['S'])
    rect(1, 1, 12, 22, NAVY)
    rect(2, 2, 10, 20, GRIP)
    for y in range(3, 21):
        img.putpixel((7, y), ICE); img.putpixel((6, y), ICE_L if y % 4 else ICE)
    for x in range(3, 11):
        img.putpixel((x, 11), ICE)
    rect(14, 1, 12, 22, p['B'])
    rect(26, 0, 8, 24, GRIP)  # handle area
    return img


# ---------------------------------------------------------------- blocks
def noise_fill(base, amt=6, size=16, seed=0, alpha=255):
    r = random.Random(seed)
    img = Image.new('RGBA', (size, size))
    b = hx(base) if isinstance(base, str) else base
    for x in range(size):
        for y in range(size):
            d = r.choice([-amt, 0, 0, 0, amt // 2, amt])
            img.putpixel((x, y), shift(b, d)[:3] + (alpha,))
    return img


def bevel(img, light=14, dark=-16):
    w, h = img.size
    for i in range(w):
        img.putpixel((i, 0), shift(img.getpixel((i, 0)), light))
        img.putpixel((i, h - 1), shift(img.getpixel((i, h - 1)), dark))
    for j in range(h):
        img.putpixel((0, j), shift(img.getpixel((0, j)), light))
        img.putpixel((w - 1, j), shift(img.getpixel((w - 1, j)), dark))
    return img


def tiles(base, line, seed, cells):
    img = noise_fill(base, 5, seed=seed)
    L = hx(line)
    for (x0, y0, x1, y1) in cells:
        for x in range(x0, x1 + 1):
            img.putpixel((x % 16, y1 % 16), L)
        for y in range(y0, y1 + 1):
            img.putpixel((x1 % 16, y % 16), L)
        for x in range(x0, x1):
            img.putpixel((x % 16, y0 % 16), shift(img.getpixel((x % 16, y0 % 16)), 12))
    return img


def planks(base):
    img = noise_fill(base, 4, seed=hash(base) % 999)
    d = shift(hx(base), -38)
    for row in range(4):
        y = row * 4 + 3
        for x in range(16):
            img.putpixel((x, y), d)
        sx = (row * 7 + 3) % 16
        for yy in range(row * 4, row * 4 + 3):
            img.putpixel((sx, yy), d)
        for x in range(16):
            img.putpixel((x, row * 4), shift(img.getpixel((x, row * 4)), 10))
    return img


def log_side(base):
    img = noise_fill(base, 4, seed=7)
    for x in (2, 6, 10, 13):
        for y in range(16):
            if (y + x) % 7:
                img.putpixel((x, y), shift(hx(base), -26))
    return img


def log_top(bark, inner):
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                c = hx(bark)
            else:
                c = shift(hx(inner), -16 if int(d) % 2 == 0 else 0)
            img.putpixel((x, y), c)
    return img


def ore(gem_c, seed):
    img = noise_fill('#8a919b', 4, seed=3)
    r = random.Random(seed)
    G = hx(gem_c)
    for (cx, cy) in [(3, 3), (10, 5), (5, 11), (12, 12)]:
        for (dx, dy) in [(0, 0), (1, 0), (0, 1), (1, 1)]:
            img.putpixel((cx + dx, cy + dy), G if (dx + dy) else shift(G, 50))
        img.putpixel((cx + 2, cy + 1), shift(G, -50))
        img.putpixel((cx + 1, cy + 2), shift(G, -50))
    return img


def storage(colors):
    o, s, b, l, h = [hx(c) for c in colors]
    img = Image.new('RGBA', (16, 16), b)
    for i in range(16):
        img.putpixel((i, 0), h); img.putpixel((0, i), h)
        img.putpixel((i, 15), s); img.putpixel((15, i), s)
    for i in range(2, 14):
        img.putpixel((i, 2), l); img.putpixel((2, i), l)
        img.putpixel((i, 13), s); img.putpixel((13, i), s)
    return img


def glass(tint=None, alpha=0):
    fill = (255, 255, 255, 0) if tint is None else hx(tint, alpha)
    img = Image.new('RGBA', (16, 16), fill)
    edge = hx('#e9f6ff', 230) if tint is None else shift(hx(tint, 235), 30)
    for i in range(16):
        for (x, y) in [(i, 0), (0, i), (i, 15), (15, i)]:
            img.putpixel((x, y), edge)
    if tint is None:
        for (x, y) in [(3, 3), (4, 3), (3, 4)]:
            img.putpixel((x, y), hx('#ffffff', 140))
    return img


def pane_top(tint=None):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    c = hx('#e9f6ff', 230) if tint is None else shift(hx(tint, 235), 30)
    for x in range(16):
        img.putpixel((x, 7), c); img.putpixel((x, 8), c)
    return img


DYES = {
    'white': '#f4f6f8', 'light_gray': '#a3a9b1', 'gray': '#4c5259', 'black': '#16181c',
    'brown': '#7a5234', 'red': '#d63a3a', 'orange': '#f58a1f', 'yellow': '#f7d23c',
    'lime': '#7ed43d', 'green': '#4b7a24', 'cyan': '#1fa3a8', 'light_blue': '#5cc8f2',
    'blue': '#3a5bd6', 'purple': '#8a3ad6', 'magenta': '#c84fd0', 'pink': '#f492b8',
}


def wool(c):
    img = noise_fill(c, 5, seed=11)
    for x in range(16):
        for y in range(16):
            if (x + 2 * y) % 6 == 0:
                img.putpixel((x, y), shift(img.getpixel((x, y)), -10))
    return img


def cobweb():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    c = hx('#f4faff', 220)
    for i in range(1, 15):
        img.putpixel((i, i), c); img.putpixel((15 - i, i), c)
        img.putpixel((i, 8), c); img.putpixel((8, i), c)
    for a in range(0, 360, 12):
        x = 8 + 5 * math.cos(math.radians(a)); y = 8 + 5 * math.sin(math.radians(a))
        img.putpixel((int(round(x)) % 16, int(round(y)) % 16), hx('#f4faff', 160))
    return img


def ladder():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    r = hx('#c9a77c'); d = hx('#8a6a45')
    for y in range(16):
        img.putpixel((2, y), r); img.putpixel((3, y), d); img.putpixel((12, y), r); img.putpixel((13, y), d)
    for y in (2, 6, 10, 14):
        for x in range(4, 12):
            img.putpixel((x, y), r); img.putpixel((x, y + 1), d)
    return img


def tnt(face_):
    if face_ == 'side':
        img = noise_fill('#d63a3a', 4, seed=21)
        for x in range(16):
            for y in range(5, 11):
                img.putpixel((x, y), hx('#f4f6f8'))
        # "TNT"
        T = [(1, 6), (2, 6), (3, 6), (2, 7), (2, 8), (2, 9)]
        N = [(6, 6), (6, 7), (6, 8), (6, 9), (7, 7), (8, 8), (9, 6), (9, 7), (9, 8), (9, 9)]
        T2 = [(x + 11, y) for x, y in T]
        for q in T + N + T2:
            img.putpixel(q, NAVY)
        for x in range(16):
            img.putpixel((x, 0), shift(hx('#d63a3a'), 20)); img.putpixel((x, 15), shift(hx('#d63a3a'), -30))
        return img
    img = storage(['#000000', '#a82a2a', '#d63a3a', '#e85a5a', '#f07a7a'])
    if face_ == 'top':
        for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
            img.putpixel((x, y), NAVY)
    return img


def anim(frames, size=16):
    img = Image.new('RGBA', (size, size * len(frames)))
    for i, f in enumerate(frames):
        img.paste(f, (0, i * size))
    return img


def water(size, n=16):
    frames = []
    for i in range(n):
        f = Image.new('RGBA', (size, size))
        for x in range(size):
            for y in range(size):
                v = 0.5 + 0.5 * math.sin((x * 0.5 + y * 0.9) + i * 2 * math.pi / n)
                g = int(180 + 22 * v)
                f.putpixel((x, y), (g, g, g, 165))
        frames.append(f)
    return anim(frames, size)


def lava(size, n=16):
    frames = []
    for i in range(n):
        f = Image.new('RGBA', (size, size))
        for x in range(size):
            for y in range(size):
                v = 0.5 + 0.5 * math.sin(x * 0.7 + i * 2 * math.pi / n) * math.cos(y * 0.6 - i * 2 * math.pi / n)
                c = (255, int(110 + 90 * v), int(20 + 40 * v), 255)
                f.putpixel((x, y), c)
        frames.append(f)
    return anim(frames, size)


def low_fire(colors, n=16):
    frames = []
    cols = [hx(c) for c in colors]
    for i in range(n):
        f = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for x in range(16):
            h = 3 + int(2.5 * (0.5 + 0.5 * math.sin(x * 1.3 + i * 0.8)))
            for k in range(h):
                y = 15 - k
                c = cols[min(len(cols) - 1, k * len(cols) // h)]
                f.putpixel((x, y), c[:3] + (235,))
        frames.append(f)
    return anim(frames)


def leaves():
    img = Image.new('RGBA', (16, 16))
    r = random.Random(5)
    for x in range(16):
        for y in range(16):
            v = 150 + r.choice([-14, -6, 0, 0, 8, 16])
            if (x * 3 + y * 5) % 11 == 0:
                img.putpixel((x, y), (0, 0, 0, 0))
            else:
                img.putpixel((x, y), (v, v, v, 255))
    return img


# ---------------------------------------------------------------- GUI
def mask_img(m, cmap):
    rows = m.strip('\n').split('\n')
    img = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c in cmap:
                img.putpixel((x, y), cmap[c])
    return img


HEART = """
.OOO.OOO.
OHffOfffO
OffffffsO
OffffffsO
.OffffsO.
..OffsO..
...OsO...
....O....
........."""


def heart(fill, shade, outline=NAVY, half=False, empty=False, hi=WHITE):
    cm = {'O': outline, 'f': fill, 's': shade, 'H': hi}
    img = mask_img(HEART, cm if not empty else {'O': outline})
    if empty:
        inner = mask_img(HEART, {'f': hx('#1a2a40', 200), 's': hx('#1a2a40', 200), 'H': hx('#1a2a40', 200)})
        img = Image.alpha_composite(img, inner)
    if half:
        for x in range(5, 9):
            for y in range(9):
                if img.getpixel((x, y))[3] and HEART.strip('\n').split('\n')[y][x] in 'fsH':
                    img.putpixel((x, y), (0, 0, 0, 0))
    return img


FOOD = """
....OOOO.
...OffffO
..OffffsO
..OffffsO
..OfffssO
.OOOsssO.
OwO.OOO..
OwwO.....
.OO......"""

ARMOR_ICON = """
.OO...OO.
OHfOOOffO
OfffffffO
.OOfffsO.
..OfffsO.
..OfffsO.
..OfffsO.
..OssssO.
..OOOOOO."""


def icon_set(mask, fill, shade, half=False, empty=False):
    cm = {'O': NAVY, 'f': fill, 's': shade, 'H': WHITE, 'w': hx('#f4f6f8')}
    if empty:
        cm = {'O': NAVY, 'f': hx('#1a2a40', 200), 's': hx('#1a2a40', 200), 'H': hx('#1a2a40', 200), 'w': hx('#1a2a40', 200)}
    img = mask_img(mask, cm)
    if half:
        rows = mask.strip('\n').split('\n')
        for x in range(5, 9):
            for y in range(9):
                if rows[y][x] in 'fsH':
                    img.putpixel((x, y), hx('#1a2a40', 200))
    return img


def hotbar():
    img = Image.new('RGBA', (182, 22), (0, 0, 0, 0))
    for x in range(182):
        for y in range(22):
            edge = x in (0, 181) or y in (0, 21)
            img.putpixel((x, y), hx('#0b1e36', 235) if edge else hx('#0f2238', 140))
    for i in range(1, 9):
        x = 1 + 20 * i
        for y in range(3, 19):
            img.putpixel((x, y), hx('#7cc4f5', 70))
    return img


def hotbar_sel():
    img = Image.new('RGBA', (24, 23), (0, 0, 0, 0))
    for x in range(24):
        for y in range(23):
            if x in (0, 1, 22, 23) or y in (0, 1, 21, 22):
                img.putpixel((x, y), hx('#bfe3ff', 255) if (x in (0, 23) or y in (0, 22)) else hx('#7cc4f5', 255))
            else:
                img.putpixel((x, y), hx('#7cc4f5', 40))
    return img


def xp_bar(progress):
    img = Image.new('RGBA', (182, 5))
    for x in range(182):
        for y in range(5):
            if progress:
                t = x / 181
                c = (int(90 + 100 * t), int(200 + 40 * t), 255, 255)
                if y == 1:
                    c = shift(c, 30)
                if y == 4:
                    c = shift(c, -40)
            else:
                c = hx('#0b1e36', 220) if y in (0, 4) or x in (0, 181) else hx('#1a2a40', 200)
            img.putpixel((x, y), c)
    return img


def crosshair():
    img = Image.new('RGBA', (15, 15), (0, 0, 0, 0))
    w = (255, 255, 255, 255)
    img.putpixel((7, 7), w)
    for k in (3, 4, 5, 9, 10, 11):
        img.putpixel((7, k), w); img.putpixel((k, 7), w)
    return img


def button(kind):
    img = Image.new('RGBA', (200, 20))
    base = {'n': '#1c3352', 'h': '#2e5f94', 'd': '#121c29'}[kind]
    border = {'n': '#0b1e36', 'h': '#bfe3ff', 'd': '#0a111a'}[kind]
    for x in range(200):
        for y in range(20):
            if x in (0, 199) or y in (0, 19):
                c = hx(border)
            elif y == 1:
                c = shift(hx(base), 25)
            elif y == 18:
                c = shift(hx(base), -20)
            else:
                c = hx(base)
            img.putpixel((x, y), c)
    return img


def glint():
    img = Image.new('RGBA', (64, 64))
    for x in range(64):
        for y in range(64):
            v = 0.5 + 0.5 * math.sin((x + y) * 2 * math.pi / 32)
            v = v ** 6
            img.putpixel((x, y), (int(60 * v), int(120 * v), int(200 * v), 255))
    return img


def particle_star(c):
    img = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    C = hx(c)
    for q in [(3, 1), (3, 2), (3, 4), (3, 5), (1, 3), (2, 3), (4, 3), (5, 3)]:
        img.putpixel(q, C)
    img.putpixel((3, 3), (255, 255, 255, 255))
    return img


def pack_png():
    img = Image.new('RGBA', (128, 128))
    for x in range(128):
        for y in range(128):
            t = (x + y) / 254
            img.putpixel((x, y), (int(11 + 40 * t), int(30 + 90 * t), int(54 + 150 * t), 255))
    cx = cy = 64
    W = (240, 250, 255, 255)
    for a in range(0, 360, 60):
        ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
        for r in range(0, 46):
            for w in (-2, -1, 0, 1, 2):
                img.putpixel((int(cx + ca * r - sa * w), int(cy + sa * r + ca * w)), W)
        for side in (-1, 1):
            for k in range(14):
                b = math.radians(a + side * 40)
                x0, y0 = cx + ca * 28, cy + sa * 28
                for w in (-1, 0, 1):
                    img.putpixel((int(x0 + math.cos(b) * k + w), int(y0 + math.sin(b) * k)), W)
    return img


# ================================================================== BUILD
def build():
    tools = {'sword': sword, 'axe': axe, 'pickaxe': pickaxe, 'shovel': shovel, 'hoe': hoe}
    for t in ['wooden', 'stone', 'copper', 'iron', 'golden', 'diamond', 'netherite']:
        for n, f in tools.items():
            save(f(t), f'item/{t}_{n}.png')
    for t in ['copper', 'chainmail', 'iron', 'golden', 'diamond', 'netherite']:
        for piece, m in ARMOR.items():
            save(from_mask(m, pal(t)), f'item/{t}_{piece}.png')
        armor_layers(t)

    save(bow(0), 'item/bow.png')
    for i in range(3):
        save(bow(i + 1), f'item/bow_pulling_{i}.png')
    save(arrow(), 'item/arrow.png')
    save(ender_pearl(), 'item/ender_pearl.png')
    save(ender_eye(), 'item/ender_eye.png')
    gold_apple = ['#4a3510', '#c98f1c', '#f0c040', '#ffe17a', '#fff6c8']
    save(apple(gold_apple), 'item/golden_apple.png')
    save(apple(gold_apple), 'item/enchanted_golden_apple.png')
    save(apple(['#3d0a12', '#a8182e', '#e0304a', '#ff6b80', '#ffd2da']), 'item/apple.png')
    save(steak(), 'item/cooked_beef.png')
    save(golden_carrot(), 'item/golden_carrot.png')
    for k in ['potion', 'splash_potion', 'lingering_potion']:
        b, o = potion(k)
        save(b, f'item/{k}.png')
        save(o, f'item/{k}_overlay.png')
    save(potion('potion')[0], 'item/glass_bottle.png')
    save(gem(['#0b2a44', '#2b8fd0', '#5cc8f2', '#a9e8ff', '#eafbff']), 'item/diamond.png')
    save(gem(['#08331c', '#1f9a52', '#3ddc84', '#9af5c0', '#eafff2']), 'item/emerald.png')
    save(ingot(TIERS['iron']), 'item/iron_ingot.png')
    save(ingot(TIERS['golden']), 'item/gold_ingot.png')
    save(ingot(TIERS['copper']), 'item/copper_ingot.png')
    save(ingot(TIERS['netherite']), 'item/netherite_ingot.png')
    save(stick(), 'item/stick.png')
    save(snowball(), 'item/snowball.png')
    save(bucket(), 'item/bucket.png')
    save(bucket('water'), 'item/water_bucket.png')
    save(bucket('lava'), 'item/lava_bucket.png')
    save(fishing_rod(), 'item/fishing_rod.png')
    save(fishing_rod(True), 'item/fishing_rod_cast.png')
    save(flint_and_steel(), 'item/flint_and_steel.png')
    save(totem(), 'item/totem_of_undying.png')
    save(shield_tex(), 'entity/shield/shield_base_nopattern.png')

    # blocks
    B = 'block/'
    save(noise_fill('#8d949e', 5, seed=1), B + 'stone.png')
    save(tiles('#7d848e', '#4e545c', 2, [(0, 0, 7, 4), (8, 0, 15, 4), (0, 5, 4, 10), (5, 5, 15, 10), (0, 11, 9, 15), (10, 11, 15, 15)]), B + 'cobblestone.png')
    save(noise_fill('#8b6a4a', 6, seed=3), B + 'dirt.png')
    gt = noise_fill('#9a9a9a', 8, seed=4)
    save(gt, B + 'grass_block_top.png')
    side = noise_fill('#8b6a4a', 6, seed=3)
    ov = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        h = 3 + (1 if x % 5 == 2 else 0)
        for y in range(h):
            side.putpixel((x, y), hx('#6aa84f'))
            ov.putpixel((x, y), (154, 154, 154, 255))
    save(side, B + 'grass_block_side.png')
    save(ov, B + 'grass_block_side_overlay.png')
    save(noise_fill('#e6dcb3', 4, seed=5), B + 'sand.png')
    save(tiles('#8f8f8f', '#6a6a6a', 6, [(0, 0, 4, 3), (5, 0, 10, 4), (11, 0, 15, 3), (0, 4, 6, 9), (7, 5, 15, 10), (0, 10, 8, 15), (9, 11, 15, 15)]), B + 'gravel.png')
    save(planks('#c9a77c'), B + 'oak_planks.png')
    save(planks('#e8dcc0'), B + 'birch_planks.png')
    save(planks('#8a6a45'), B + 'spruce_planks.png')
    save(log_side('#6b5136'), B + 'oak_log.png')
    save(log_top('#6b5136', '#c9a77c'), B + 'oak_log_top.png')
    save(leaves(), B + 'oak_leaves.png')
    save(glass(), B + 'glass.png')
    save(pane_top(), B + 'glass_pane_top.png')
    for name, c in DYES.items():
        save(wool(c), B + f'{name}_wool.png')
        save(Image.new('RGBA', (16, 16), hx(c)), B + f'{name}_concrete.png')
        save(bevel(noise_fill(shift(hx(c), -30), 3, seed=8)), B + f'{name}_terracotta.png')
        save(glass(c, 90), B + f'{name}_stained_glass.png')
        save(pane_top(c), B + f'{name}_stained_glass_pane_top.png')
    save(bevel(noise_fill('#ece9bd', 4, seed=9)), B + 'end_stone.png')
    obs = noise_fill('#17112b', 4, seed=10)
    for (x, y) in [(3, 4), (4, 4), (11, 9), (12, 10), (6, 12), (9, 2)]:
        obs.putpixel((x, y), hx('#5b3a9e'))
    save(obs, B + 'obsidian.png')
    save(tiles('#55595f', '#2a2c30', 12, [(0, 0, 5, 5), (6, 0, 15, 6), (0, 6, 9, 15), (10, 7, 15, 15)]), B + 'bedrock.png')
    save(noise_fill('#8e3a3a', 6, seed=13), B + 'netherrack.png')
    save(noise_fill('#f2f8fd', 3, seed=14), B + 'snow.png')
    save(noise_fill('#a8d4f5', 4, seed=15), B + 'ice.png')
    save(noise_fill('#8ec3ef', 3, seed=16), B + 'packed_ice.png')
    save(ore('#5cc8f2', 1), B + 'diamond_ore.png')
    save(ore('#e3c2a8', 2), B + 'iron_ore.png')
    save(ore('#f0c040', 3), B + 'gold_ore.png')
    save(ore('#3ddc84', 4), B + 'emerald_ore.png')
    save(ore('#2a2f38', 5), B + 'coal_ore.png')
    save(storage(TIERS['diamond']), B + 'diamond_block.png')
    save(storage(TIERS['iron']), B + 'iron_block.png')
    save(storage(TIERS['golden']), B + 'gold_block.png')
    save(storage(['#08331c', '#1f9a52', '#3ddc84', '#9af5c0', '#eafff2']), B + 'emerald_block.png')
    save(storage(TIERS['netherite']), B + 'netherite_block.png')
    save(cobweb(), B + 'cobweb.png')
    save(ladder(), B + 'ladder.png')
    for f in ('side', 'top', 'bottom'):
        save(tnt(f), B + f'tnt_{f}.png')
    save(water(16), B + 'water_still.png', {'animation': {'frametime': 3}})
    save(water(32), B + 'water_flow.png', {'animation': {'frametime': 2}})
    save(lava(16), B + 'lava_still.png', {'animation': {'frametime': 3}})
    save(lava(32), B + 'lava_flow.png', {'animation': {'frametime': 2}})
    for n in ('fire_0', 'fire_1'):
        save(low_fire(['#fff3b0', '#ffc24a', '#ff7a1a']), B + f'{n}.png', {'animation': {'frametime': 2}})
    for n in ('soul_fire_0', 'soul_fire_1'):
        save(low_fire(['#eafbff', '#7cf3ff', '#1fa3c8']), B + f'{n}.png', {'animation': {'frametime': 2}})

    # gui
    S = 'gui/sprites/hud/'
    red, red_s = hx('#ff3b5c'), hx('#c21f3c')
    gold, gold_s = hx('#ffd23f'), hx('#d19a0f')
    poison, poison_s = hx('#8fd14f'), hx('#5c9a2a')
    wither, wither_s = hx('#3a3f4a'), hx('#22252c')
    frozen, frozen_s = hx('#a9e8ff'), hx('#5cc8f2')
    sets = {'': (red, red_s), 'absorbing_': (gold, gold_s), 'poisoned_': (poison, poison_s),
            'withered_': (wither, wither_s), 'frozen_': (frozen, frozen_s)}
    for pre, (f, s) in sets.items():
        save(heart(f, s), S + f'heart/{pre}full.png')
        save(heart(f, s, half=True), S + f'heart/{pre}half.png')
        save(heart(f, s, outline=WHITE), S + f'heart/{pre}full_blinking.png')
        save(heart(f, s, outline=WHITE, half=True), S + f'heart/{pre}half_blinking.png')
        save(heart(f, s), S + f'heart/{pre}hardcore_full.png')
        save(heart(f, s, half=True), S + f'heart/{pre}hardcore_half.png')
        save(heart(f, s, outline=WHITE), S + f'heart/{pre}hardcore_full_blinking.png')
        save(heart(f, s, outline=WHITE, half=True), S + f'heart/{pre}hardcore_half_blinking.png')
    save(heart(red, red_s, empty=True), S + 'heart/container.png')
    save(heart(red, red_s, empty=True, outline=WHITE), S + 'heart/container_blinking.png')
    save(heart(red, red_s, empty=True), S + 'heart/container_hardcore.png')
    save(heart(red, red_s, empty=True, outline=WHITE), S + 'heart/container_hardcore_blinking.png')
    save(heart(hx('#e9eef4'), hx('#a3a9b1')), S + 'heart/vehicle_full.png')
    save(heart(hx('#e9eef4'), hx('#a3a9b1'), half=True), S + 'heart/vehicle_half.png')
    save(heart(red, red_s, empty=True), S + 'heart/vehicle_container.png')

    meat, meat_s = hx('#c8743f'), hx('#8a4a22')
    save(icon_set(FOOD, meat, meat_s), S + 'food_full.png')
    save(icon_set(FOOD, meat, meat_s, half=True), S + 'food_half.png')
    save(icon_set(FOOD, meat, meat_s, empty=True), S + 'food_empty.png')
    hm, hm_s = hx('#9bc26a'), hx('#5c7d3a')
    save(icon_set(FOOD, hm, hm_s), S + 'food_full_hunger.png')
    save(icon_set(FOOD, hm, hm_s, half=True), S + 'food_half_hunger.png')
    save(icon_set(FOOD, hm, hm_s, empty=True), S + 'food_empty_hunger.png')
    ar, ar_s = hx('#cfe6f7'), hx('#7cc4f5')
    save(icon_set(ARMOR_ICON, ar, ar_s), S + 'armor_full.png')
    save(icon_set(ARMOR_ICON, ar, ar_s, half=True), S + 'armor_half.png')
    save(icon_set(ARMOR_ICON, ar, ar_s, empty=True), S + 'armor_empty.png')
    save(hotbar(), S + 'hotbar.png')
    save(hotbar_sel(), S + 'hotbar_selection.png')
    save(xp_bar(False), S + 'experience_bar_background.png')
    save(xp_bar(True), S + 'experience_bar_progress.png')
    save(crosshair(), S + 'crosshair.png')
    nine = {'gui': {'scaling': {'type': 'nine_slice', 'width': 200, 'height': 20, 'border': 3}}}
    save(button('n'), 'gui/sprites/widget/button.png', nine)
    save(button('h'), 'gui/sprites/widget/button_highlighted.png', nine)
    save(button('d'), 'gui/sprites/widget/button_disabled.png', nine)

    save(glint(), 'misc/enchanted_glint_item.png')
    save(glint(), 'misc/enchanted_glint_armor.png')
    save(particle_star('#7cc4f5'), 'particle/critical_hit.png')
    save(particle_star('#c08cff'), 'particle/enchanted_hit.png')

    os.makedirs(OUT, exist_ok=True)
    pack_png().save(os.path.join(OUT, 'pack.png'))
    with open(os.path.join(OUT, 'pack.mcmeta'), 'w', encoding='utf-8') as f:
        json.dump({'pack': {'description': '§b❄ Frost PvP §7– clean textures & custom sounds',
                            'min_format': [75, 0], 'max_format': [75, 0]}}, f, ensure_ascii=False, indent=2)


if __name__ == '__main__':
    build()
    n = sum(len([f for f in fs if f.endswith('.png')]) for _, _, fs in os.walk(TEX))
    print('textures:', n)
