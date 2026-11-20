import os, json, wave, subprocess, tempfile
import numpy as np

OUT = os.path.join(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'build', 'texturepacks')), 'FrostPvP', 'assets', 'minecraft')
SR = 44100
rng = np.random.default_rng(42)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.002, decay=8.0):
    x = np.arange(n) / SR
    e = np.exp(-decay * x)
    ai = int(a * SR)
    if ai > 0:
        e[:ai] *= np.linspace(0, 1, ai)
    return e


def band(sig, lo, hi):
    F = np.fft.rfft(sig)
    f = np.fft.rfftfreq(len(sig), 1 / SR)
    F[(f < lo) | (f > hi)] = 0
    return np.fft.irfft(F, len(sig))


def noise(d):
    return rng.standard_normal(int(SR * d))


def tone(freq, d, phase=0):
    if callable(freq):
        x = t(d)
        return np.sin(2 * np.pi * np.cumsum(freq(x)) / SR + phase)
    return np.sin(2 * np.pi * freq * t(d) + phase)


def norm(x, peak=0.85):
    x = x - np.mean(x)
    m = np.max(np.abs(x)) or 1
    fade = min(len(x), int(0.004 * SR))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x / m * peak


def pad(x, d):
    out = np.zeros(int(SR * d)); out[:len(x)] += x[:len(out)]; return out


# ------------------------------------------------------------- designs
def hit(pitch=1.0, bright=1.0):
    d = 0.12
    body = tone(lambda x: 170 * pitch * np.exp(-x * 18) + 70, d) * env(int(SR * d), decay=35)
    snap = band(noise(d), 1500 * bright, 6000 * bright) * env(int(SR * d), decay=90) * 0.9
    return norm(body * 0.9 + snap)


def crit(i):
    d = 0.25
    sp = sum(tone(f * (1 + 0.03 * i), d) * env(int(SR * d), decay=dk) for f, dk in [(2093, 22), (3136, 28), (4186, 35)])
    return norm(pad(hit(1.2, 1.4), d) + sp * 0.35)


def sweep(i):
    d = 0.32
    n = noise(d)
    x = t(d)
    lo = band(n, 600 + 200 * i, 3500 + 300 * i)
    e = np.sin(np.pi * np.clip(x / d, 0, 1)) ** 2
    return norm(lo * e)


def hurt(i):
    d = 0.18
    body = tone(lambda x: (220 - 30 * i) * np.exp(-x * 9) + 60, d) * env(int(SR * d), a=0.003, decay=20)
    air = band(noise(d), 300, 1800) * env(int(SR * d), decay=40) * 0.4
    return norm(body + air)


def ding():
    d = 0.45
    s = sum(a * tone(f, d) * env(int(SR * d), decay=k) for f, a, k in [(1760, 1, 7), (2640, .5, 10), (3520, .25, 14)])
    return norm(s, 0.7)


def bow_shoot(i):
    d = 0.28
    twang = tone(lambda x: 260 - 60 * x, d) * env(int(SR * d), decay=18) * 0.6
    wh = band(noise(d), 800 + 150 * i, 5000) * np.sin(np.pi * np.clip(t(d) / d, 0, 1)) * 0.5
    return norm(twang + wh)


def arrow_hit(i):
    d = 0.15
    return norm(tone(lambda x: 400 * np.exp(-x * 25) + 120 + 15 * i, d) * env(int(SR * d), decay=30) + band(noise(d), 2000, 6000) * env(int(SR * d), decay=120) * 0.3)


def orb(i):
    d = 0.22
    f = [1318.5, 1480, 1568, 1760][i % 4]
    return norm((tone(f, d) + 0.4 * tone(f * 2, d)) * env(int(SR * d), decay=16), 0.55)


def levelup():
    notes = [523.25, 659.25, 783.99, 1046.5]
    out = np.zeros(int(SR * 1.1))
    for k, f in enumerate(notes):
        s = (tone(f, .7) + .35 * tone(f * 2, .7) + .15 * tone(f * 3, .7)) * env(int(SR * .7), decay=5)
        st = int(SR * 0.09 * k)
        out[st:st + len(s)] += s
    return norm(out, 0.7)


def eat(i):
    d = 0.2
    out = np.zeros(int(SR * d))
    for k in range(3):
        st = int(SR * (0.01 + 0.055 * k + 0.008 * i))
        b = band(noise(0.04), 900, 5000) * env(int(SR * .04), decay=70)
        out[st:st + len(b)] += b[:len(out) - st]
    return norm(out, 0.6)


def drink():
    d = 0.35
    g = tone(lambda x: 180 + 260 * (x / d), d) * np.sin(np.pi * np.clip(t(d) / d, 0, 1)) ** 2
    bub = sum(tone(700 + 120 * k, 0.03) * env(int(SR * .03), decay=80) for k in range(1))
    out = g * 0.7
    for k in range(4):
        st = int(SR * (0.05 + 0.07 * k))
        out[st:st + len(bub)] += bub * 0.5
    return norm(out, 0.6)


def burp():
    d = 0.25
    return norm(tone(lambda x: 110 + 30 * np.sin(x * 40), d) * band(noise(d) * 0.3 + 1, 0, 3000) * np.sin(np.pi * t(d) / d), 0.5)


def teleport(i):
    d = 0.4
    s = tone(lambda x: 300 + 2200 * (x / d) ** 1.5 + 40 * i, d) * np.sin(np.pi * t(d) / d)
    sh = band(noise(d), 4000, 9000) * np.sin(np.pi * t(d) / d) * 0.25
    return norm(s * 0.6 + sh, 0.6)


def click():
    d = 0.05
    return norm(tone(2400, d) * env(int(SR * d), decay=120) + band(noise(d), 3000, 8000) * env(int(SR * d), decay=300) * 0.4, 0.6)


def shield_block(i):
    d = 0.35
    s = sum(a * tone(f * (1 + 0.02 * i), d) * env(int(SR * d), decay=k) for f, a, k in [(520, 1, 14), (1370, .7, 18), (2210, .45, 24), (3330, .3, 30)])
    return norm(s + band(noise(d), 2000, 7000) * env(int(SR * d), decay=90) * 0.5, 0.7)


def shield_break():
    return norm(np.concatenate([shield_block(0)[:int(SR * .1)], glass(0)]), 0.75)


def glass(i):
    d = 0.5
    out = band(noise(d), 3000, 10000) * env(int(SR * d), decay=14) * 0.6
    for k in range(6):
        st = int(SR * (0.02 + 0.05 * k + 0.01 * i))
        f = 2500 + 900 * ((k * 7 + i * 3) % 5)
        tk = tone(f, 0.12) * env(int(SR * .12), decay=40)
        out[st:st + len(tk)] += tk[:len(out) - st] * 0.5
    return norm(out, 0.7)


def pop():
    d = 0.09
    return norm(tone(lambda x: 600 + 900 * x / d, d) * env(int(SR * d), decay=45), 0.5)


def fall(big):
    d = 0.2
    return norm(tone(lambda x: (90 if big else 140) * np.exp(-x * 6) + 40, d) * env(int(SR * d), decay=22) + band(noise(d), 100, 900) * env(int(SR * d), decay=50) * 0.5)


def death():
    d = 0.7
    return norm(tone(lambda x: 520 * np.exp(-x * 3) + 80, d) * env(int(SR * d), decay=4) + 0.3 * tone(lambda x: 780 * np.exp(-x * 3) + 120, d) * env(int(SR * d), decay=5), 0.7)


def woosh(i):
    d = 0.3
    return norm(band(noise(d), 400 + 100 * i, 2500) * np.sin(np.pi * t(d) / d) ** 2, 0.6)


# ------------------------------------------------------------- write
def write_ogg(sig, rel):
    path = os.path.join(OUT, 'sounds', 'frost', rel + '.ogg')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(sig, -1, 1) * 32767).astype('<i2')
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tf:
        with wave.open(tf.name, 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tf.name, '-c:a', 'libvorbis', '-q:a', '6', path], check=True)
    os.unlink(tf.name)


SOUNDS = {}


def reg(event, name, gens, volume=1.0):
    files = []
    for k, g in enumerate(gens):
        rel = f'{name}{k + 1}'
        write_ogg(g(), rel)
        files.append({'name': f'frost/{rel}', 'volume': volume} if volume != 1.0 else f'frost/{rel}')
    for e in ([event] if isinstance(event, str) else event):
        SOUNDS[e] = {'replace': True, 'sounds': files}


def build():
    reg('entity.player.attack.strong', 'hit_strong', [lambda i=i: hit(1 + 0.06 * i) for i in range(4)])
    reg('entity.player.attack.knockback', 'hit_knock', [lambda i=i: hit(0.85 + 0.05 * i, 0.8) for i in range(3)])
    reg('entity.player.attack.weak', 'hit_weak', [lambda i=i: hit(1.3 + 0.05 * i, 0.6) for i in range(3)], 0.6)
    reg('entity.player.attack.nodamage', 'hit_nodmg', [lambda: hit(1.6, 0.5)], 0.4)
    reg('entity.player.attack.crit', 'crit', [lambda i=i: crit(i) for i in range(3)])
    reg('entity.player.attack.sweep', 'sweep', [lambda i=i: sweep(i) for i in range(3)], 0.6)
    reg('entity.player.hurt', 'hurt', [lambda i=i: hurt(i) for i in range(3)])
    reg(['entity.player.hurt_on_fire', 'entity.player.hurt_drown', 'entity.player.hurt_freeze', 'entity.player.hurt_sweet_berry_bush'], 'hurt_alt', [lambda i=i: hurt(i + 1) for i in range(2)])
    reg('entity.arrow.hit_player', 'ding', [ding])
    reg(['entity.arrow.shoot', 'item.crossbow.shoot'], 'bow', [lambda i=i: bow_shoot(i) for i in range(3)])
    reg('entity.arrow.hit', 'arrow_hit', [lambda i=i: arrow_hit(i) for i in range(4)])
    reg(['entity.ender_pearl.throw', 'entity.snowball.throw', 'entity.egg.throw', 'entity.splash_potion.throw', 'entity.lingering_potion.throw'], 'throw', [lambda i=i: woosh(i) for i in range(3)])
    reg(['entity.player.teleport', 'entity.enderman.teleport', 'item.chorus_fruit.teleport'], 'teleport', [lambda i=i: teleport(i) for i in range(2)])
    reg('entity.experience_orb.pickup', 'orb', [lambda i=i: orb(i) for i in range(4)])
    reg('entity.player.levelup', 'levelup', [levelup])
    reg('entity.generic.eat', 'eat', [lambda i=i: eat(i) for i in range(3)])
    reg(['entity.generic.drink', 'item.honey_bottle.drink'], 'drink', [drink])
    reg('entity.player.burp', 'burp', [burp])
    reg('ui.button.click', 'click', [click])
    reg('item.shield.block', 'shield', [lambda i=i: shield_block(i) for i in range(3)])
    reg('item.shield.break', 'shield_break', [shield_break])
    reg(['block.glass.break', 'entity.splash_potion.break'], 'glass', [lambda i=i: glass(i) for i in range(3)])
    reg('entity.item.pickup', 'pop', [pop], 0.5)
    reg('entity.player.small_fall', 'fall_small', [lambda: fall(False)])
    reg('entity.player.big_fall', 'fall_big', [lambda: fall(True)])
    reg('entity.player.death', 'death', [death])
    with open(os.path.join(OUT, 'sounds.json'), 'w') as f:
        json.dump(SOUNDS, f, indent=2)
    print('events:', len(SOUNDS))


if __name__ == '__main__':
    build()
