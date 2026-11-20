import math, random
from PIL import Image
from textures import *

def glowstone():
    img = Image.new('RGBA', (16, 16), hx('#8a6a2a'))
    r = random.Random(3)
    cells = [(0,0,5,5),(6,0,10,4),(11,0,15,6),(0,6,4,10),(5,5,11,11),(12,7,15,11),(0,11,6,15),(7,12,11,15),(12,12,15,15)]
    for (x0,y0,x1,y1) in cells:
        cx, cy = (x0+x1)/2, (y0+y1)/2
        for x in range(x0, x1):
            for y in range(y0, y1):
                d = max(abs(x+.5-cx)/(x1-x0), abs(y+.5-cy)/(y1-y0))
                c = '#fff6c8' if d < .18 else '#ffe17a' if d < .32 else '#f0c040'
                img.putpixel((x, y), hx(c))
    return img

def deepslate():
    img = noise_fill('#4b4e57', 4, seed=31)
    for y in (3, 8, 12):
        for x in range(16):
            if (x + y) % 9:
                img.putpixel((x, y), hx('#3a3d45'))
    return img

def deepslate_top():
    return tiles('#4b4e57', '#33363d', 32, [(0,0,7,7),(8,0,15,7),(0,8,7,15),(8,8,15,15)])

def anchor_frame():
    img = noise_fill('#1d1533', 3, seed=40)
    for (x, y) in [(3,3),(12,5),(5,12),(10,11)]:
        img.putpixel((x, y), hx('#5b3a9e'))
    return bevel(img, 12, -10)

def anchor_side(level):
    img = anchor_frame()
    for k in range(4):
        x0 = 2 + k * 3
        lit = k < level
        for x in range(x0, x0 + 2):
            for y in range(11, 14):
                img.putpixel((x, y), hx('#7cf3ff' if lit else '#0b0718'))
        if lit:
            img.putpixel((x0, 11), hx('#eafbff'))
    return img

def anchor_top(on, phase=0):
    img = anchor_frame()
    for x in range(3, 13):
        for y in range(3, 13):
            if on:
                v = .5 + .5 * math.sin((x + y) * .7 + phase)
                c = (int(40 + 60 * v), int(190 + 50 * v), 255, 255)
            else:
                c = hx('#0b0718')
            img.putpixel((x, y), c)
    for i in range(3, 13):
        for (x, y) in [(i, 2), (2, i), (i, 13), (13, i)]:
            img.putpixel((x, y), hx('#5b3a9e'))
    return img

def xp_bottle():
    b, o = potion('potion')
    liquid = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            a = o.getpixel((x, y))[3]
            if a:
                v = .5 + .5 * math.sin(x * .9 + y * .6)
                liquid.putpixel((x, y), (int(120 + 80 * v), 255, int(140 + 60 * v), 255))
    return Image.alpha_composite(liquid, b)

def end_crystal():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    glass_p = {'S': hx('#bfe3ff', 230), 'L': hx('#eafbff', 140), 'B': hx('#7cc4f5', 70), 'H': hx('#ffffff', 170)}
    core_p = {'S': hx('#d0408a'), 'L': hx('#ff9ccf'), 'B': hx('#ff5fae'), 'H': hx('#ffe0f0')}
    box(img, 0, 0, 8, 8, 8, glass_p)
    box(img, 32, 0, 8, 8, 8, core_p)
    base_p = {'S': hx('#22262d'), 'L': hx('#6d7682'), 'B': hx('#4a4f58'), 'H': hx('#9aa3ae')}
    box(img, 0, 16, 12, 4, 12, base_p)
    return img

B = 'block/'
save(glowstone(), B + 'glowstone.png')
save(deepslate(), B + 'deepslate.png')
save(deepslate_top(), B + 'deepslate_top.png')
save(tiles('#45484f', '#2c2e34', 33, [(0,0,5,4),(6,0,15,5),(0,5,8,10),(9,6,15,11),(0,11,6,15),(7,12,15,15)]), B + 'cobbled_deepslate.png')
save(anchor_frame(), B + 'respawn_anchor_bottom.png')
for lv in range(5):
    save(anchor_side(lv), B + f'respawn_anchor_side{lv}.png')
save(anim([anchor_top(True, i * math.pi / 4) for i in range(8)]), B + 'respawn_anchor_top.png', {'animation': {'frametime': 3}})
save(anchor_top(False), B + 'respawn_anchor_top_off.png')
cry = anchor_frame()
for (x, y) in [(4,6),(5,6),(11,3),(9,10),(10,10),(3,12)]:
    cry.putpixel((x, y), hx('#7cf3ff'))
save(cry, B + 'crying_obsidian.png')
save(xp_bottle(), 'item/experience_bottle.png')
save(end_crystal(), 'entity/end_crystal/end_crystal.png')
print('extras ok')
