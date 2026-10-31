"""Generates the built-in cosmetic textures (2x vanilla resolution, pixel-art style).

Layouts (2x):
  cape/wings 128x64: cape front (2,2)-(22,34), cape back (24,2)-(44,34), elytra (44,0)-(92,44)
  shield 128x128:     plate front (2,2)-(26,46), plate back (28,2)-(52,46), handle (52,0)-(84,24)
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(sys.argv[1])
OUT.mkdir(parents=True, exist_ok=True)


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def shade(c, f):
    return tuple(clamp(v * f) for v in c[:3]) + (255,)


class Canvas:
    def __init__(self, w, h, seed):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def px(self, x, y, c):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.img.putpixel((x, y), c if len(c) == 4 else tuple(c) + (255,))

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def rect(self, box, fn):
        x0, y0, x1, y1 = box
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px(x, y, fn((x - x0) / max(1, x1 - x0 - 1), (y - y0) / max(1, y1 - y0 - 1), x, y))

    def vgrad(self, box, top, bottom, noise=0.06):
        self.rect(box, lambda u, v, x, y: shade(mix(top, bottom, v), 1 + self.rnd.uniform(-noise, noise)))

    def border(self, box, c, inset=0, width=1):
        x0, y0, x1, y1 = box
        for i in range(width):
            a, b, cc, d = x0 + inset + i, y0 + inset + i, x1 - 1 - inset - i, y1 - 1 - inset - i
            for x in range(a, cc + 1):
                self.px(x, b, c)
                self.px(x, d, c)
            for y in range(b, d + 1):
                self.px(a, y, c)
                self.px(cc, y, c)

    def stamp(self, art, ox, oy, palette, k=1):
        for j, row in enumerate(art):
            for i, ch in enumerate(row):
                if ch in palette:
                    for a in range(k):
                        for b in range(k):
                            self.px(ox + i * k + a, oy + j * k + b, palette[ch])

    def save(self, name):
        self.img.save(OUT / name)


CAPE_FRONT = (2, 2, 22, 34)
CAPE_BACK = (24, 2, 44, 34)
CAPE_ALL = (0, 0, 44, 34)
ELYTRA = (44, 0, 92, 44)

# ---------------------------------------------------------------- emblems (pixel art)
SWORD = [
    "......YY......",
    ".....YWY......",
    "....YWY.......",
    "...YWY........",
    "..YWY.........",
    ".YWY..........",
]
SWORD = [
    "....W....",
    "...WSW...",
    "...WSW...",
    "...WSW...",
    "...WSW...",
    "...WSW...",
    "...WSW...",
    "...WSW...",
    "...WSW...",
    ".GGGGGGG.",
    "....B....",
    "....B....",
    "...GGG...",
]
CROWN = [
    "G...G...G",
    "GG.GGG.GG",
    "GGGGRGGGG",
    "GGGGGGGGG",
    ".GGGGGGG.",
]
EYE = [
    "...GGGGG...",
    ".GGgggggGG.",
    "GggggBggggG",
    "GgggBBBgggG",
    "GggggBggggG",
    ".GGgggggGG.",
    "...GGGGG...",
]
GEM = [
    "..CCCCC..",
    ".CWCCCCC.",
    "CWCCCCCCC",
    "CCCCCCCCD",
    ".CCCCCCD.",
    "..CCCCD..",
    "...CCD...",
    "....D....",
]
MOON = [
    "..YYYY..",
    ".YYY....",
    "YYY.....",
    "YYY.....",
    "YYY.....",
    "YYY.....",
    ".YYY....",
    "..YYYY..",
]
TREE = [
    "....G....",
    "...GGG...",
    "..GGgGG..",
    ".GGGGGgG.",
    "..GgGGG..",
    ".GGGGgGG.",
    "GGgGGGGGG",
    "....B....",
    "....B....",
]
FLAKE = [
    "..W.W.W..",
    "...WWW...",
    "W.W.W.W.W",
    ".WWWWWWW.",
    "WWW.W.WWW",
    ".WWWWWWW.",
    "W.W.W.W.W",
    "...WWW...",
    "..W.W.W..",
]
CROSS = [
    "..GGG..",
    "..GGG..",
    "GGGGGGG",
    "GGGGGGG",
    "..GGG..",
    "..GGG..",
    "..GGG..",
    "..GGG..",
]


def cape_lining(c, base):
    c.vgrad(CAPE_BACK, shade(base, 0.55), shade(base, 0.4), 0.04)
    c.vgrad((2, 0, 22, 2), shade(base, 0.8), shade(base, 0.8))
    c.vgrad((22, 0, 42, 2), shade(base, 0.6), shade(base, 0.6))
    c.vgrad((0, 2, 2, 34), shade(base, 0.7), shade(base, 0.6))
    c.vgrad((22, 2, 24, 34), shade(base, 0.7), shade(base, 0.6))


def elytra_from_cape(c, top, bottom):
    c.vgrad(ELYTRA, top, bottom, 0.05)


def cape(name, top, bottom, trim=None, emblem=None, palette=None, extra=None):
    c = Canvas(128, 64, name)
    c.vgrad(CAPE_FRONT, top, bottom)
    cape_lining(c, top)
    if trim:
        c.border(CAPE_FRONT, trim)
        c.border(CAPE_FRONT, shade(trim, 0.6), inset=1)
    if extra:
        extra(c)
    if emblem:
        w = len(emblem[0])
        h = len(emblem)
        c.stamp(emblem, CAPE_FRONT[0] + (20 - w) // 2, CAPE_FRONT[1] + 8 + (12 - h) // 2, palette)
    elytra_from_cape(c, top, bottom)
    c.save(f"cape_{name}.png")


GOLD = (250, 205, 60)
GOLD_D = (190, 130, 30)


def stars(c, box, n, cols):
    x0, y0, x1, y1 = box
    for _ in range(n):
        c.px(c.rnd.randrange(x0 + 1, x1 - 1), c.rnd.randrange(y0 + 1, y1 - 1), c.rnd.choice(cols) + (255,))


cape("crimson", (190, 30, 40), (110, 10, 20), trim=GOLD, emblem=SWORD,
     palette={"W": (235, 235, 240, 255), "S": (170, 180, 195, 255), "G": GOLD + (255,), "B": (110, 70, 40, 255)})

cape("midnight", (30, 40, 95), (8, 10, 30), emblem=MOON, palette={"Y": (255, 235, 150, 255)},
     extra=lambda c: stars(c, CAPE_FRONT, 22, [(255, 255, 255), (255, 240, 170), (170, 200, 255)]))

cape("ender", (30, 18, 45), (8, 5, 14), emblem=EYE,
     palette={"G": (40, 140, 110, 255), "g": (90, 230, 180, 255), "B": (10, 30, 25, 255)},
     extra=lambda c: stars(c, CAPE_FRONT, 16, [(200, 90, 255), (140, 60, 210)]))


def forest_extra(c):
    x0, y0, x1, y1 = CAPE_FRONT
    for x in range(x0, x1):  # hills
        h = int(5 + 3 * math.sin(x * 0.7))
        for y in range(y1 - h, y1):
            c.px(x, y, shade((30, 70, 30), 1 + c.rnd.uniform(-0.1, 0.1)))


cape("forest", (110, 180, 90), (40, 110, 45), emblem=TREE,
     palette={"G": (25, 90, 35, 255), "g": (60, 140, 60, 255), "B": (90, 60, 30, 255)}, extra=forest_extra)


def ermine(c):
    x0, y0, x1, y1 = CAPE_FRONT
    for y in range(y1 - 7, y1):
        for x in range(x0, x1):
            c.px(x, y, (240, 240, 235, 255))
    for (dx, dy) in [(3, 2), (9, 4), (15, 2), (6, 5), (12, 1), (17, 5)]:
        c.px(x0 + dx, y1 - 7 + dy, (25, 25, 25, 255))
        c.px(x0 + dx, y1 - 7 + dy + 1, (25, 25, 25, 255))


cape("royal", (120, 40, 160), (70, 20, 100), trim=GOLD, emblem=CROWN,
     palette={"G": GOLD + (255,), "R": (220, 30, 50, 255)}, extra=ermine)


def waves(c):
    x0, y0, x1, y1 = CAPE_FRONT
    for k in range(5):
        base = y0 + 6 + k * 6
        for x in range(x0, x1):
            y = base + int(round(1.5 * math.sin((x + k * 3) * 0.6)))
            c.px(x, y, (150, 230, 255, 255))
            c.px(x, y + 1, (90, 180, 230, 255))


cape("ocean", (40, 150, 210), (10, 50, 110), extra=waves)


def petals(c):
    x0, y0, x1, y1 = CAPE_FRONT
    for _ in range(26):
        x = c.rnd.randrange(x0 + 1, x1 - 2)
        y = c.rnd.randrange(y0 + 1, y1 - 2)
        col = c.rnd.choice([(255, 255, 255), (255, 200, 225), (255, 150, 190)])
        c.px(x, y, col + (255,))
        c.px(x + 1, y, col + (255,))
        c.px(x, y + 1, shade(col, 0.9))
    for i in range(18):  # branch
        c.px(x0 + 2 + i, y0 + 4 + i // 2, (95, 55, 50, 255))
        c.px(x0 + 2 + i, y0 + 5 + i // 2, (70, 40, 38, 255))


cape("cherry", (255, 190, 215), (230, 120, 170), extra=petals)


def flames(c):
    x0, y0, x1, y1 = CAPE_FRONT
    for x in range(x0, x1):
        h = 10 + int(6 * abs(math.sin(x * 0.9))) + c.rnd.randrange(0, 5)
        for i in range(h):
            y = y1 - 1 - i
            t = i / h
            col = mix((255, 230, 90), (220, 40, 10), t)
            c.px(x, y, col)


cape("inferno", (40, 10, 10), (15, 5, 5), extra=flames)


def aurora(c):
    x0, y0, x1, y1 = CAPE_FRONT
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = (y - y0) / (y1 - y0)
            band = 0.5 + 0.5 * math.sin(x * 0.45 + y * 0.18)
            col = mix(mix((40, 220, 170), (150, 70, 230), t), (20, 30, 60), 0.35 * band)
            c.px(x, y, col)
    stars(c, CAPE_FRONT, 10, [(255, 255, 255)])


cape("aurora", (60, 200, 170), (110, 60, 200), extra=aurora)


def sparkle(c):
    stars(c, CAPE_FRONT, 14, [(255, 255, 255), (200, 255, 255)])


cape("diamond", (110, 230, 230), (30, 140, 160), trim=(220, 255, 255), emblem=GEM,
     palette={"C": (90, 225, 225, 255), "W": (255, 255, 255, 255), "D": (30, 120, 140, 255)}, extra=sparkle)

# ---------------------------------------------------------------- wings (elytra region)


def wings(name, fn):
    c = Canvas(128, 64, "w" + name)
    x0, y0, x1, y1 = ELYTRA
    for y in range(y0, y1):
        for x in range(x0, x1):
            c.px(x, y, fn(c, (x - x0) / (x1 - x0 - 1), (y - y0) / (y1 - y0 - 1), x, y))
    # also give the cape area a matching look (vanilla uses it when no cape is chosen)
    for y in range(0, 34):
        for x in range(0, 44):
            c.px(x, y, fn(c, (x % 20) / 19, y / 33, x, y))
    c.save(f"wings_{name}.png")


def feather(base, edge, vein):
    def f(c, u, v, x, y):
        col = mix(base, edge, v ** 1.6)
        if (y % 6) == 5 and (x + y // 6) % 2 == 0:
            col = shade(col, 0.82)
        if x % 7 == 3 and v > 0.25:
            col = mix(col, vein, 0.35)
        return shade(col, 1 + c.rnd.uniform(-0.05, 0.05))
    return f


wings("phoenix", feather((255, 215, 70), (210, 50, 20), (255, 140, 30)))
wings("frost", feather((235, 250, 255), (120, 190, 240), (255, 255, 255)))


def scales(base, hi, dark=0.55):
    def f(c, u, v, x, y):
        r = y // 3
        sx = (x + (r % 2) * 2) % 4
        sy = y % 3
        col = mix(base, shade(base, 0.7), v)
        if sy == 2 or (sy == 1 and sx in (0, 3)):
            col = shade(col, dark)
        elif sy == 0 and sx in (1, 2):
            col = mix(col, hi, 0.55)
        return shade(col, 1 + c.rnd.uniform(-0.04, 0.04))
    return f


wings("dragon", scales((95, 30, 130), (220, 90, 240)))
wings("angel", scales((252, 252, 248), (255, 255, 255), dark=0.86))


def membrane(c, u, v, x, y):
    col = mix((90, 105, 140), (50, 60, 90), v)
    if x % 8 == 0 or (y % 11 == 0 and x % 2 == 0):
        col = (110, 230, 200)
    return shade(col, 1 + c.rnd.uniform(-0.05, 0.05)) if len(col) == 4 else col + (255,)


wings("phantom", membrane)


def void(c, u, v, x, y):
    col = mix((25, 10, 40), (5, 3, 10), v)
    if c.rnd.random() < 0.04:
        col = c.rnd.choice([(200, 120, 255), (255, 255, 255), (140, 80, 220)]) + (255,)
    return col


wings("void", void)


def monarch(c, u, v, x, y):
    col = mix((255, 165, 40), (225, 85, 10), v)
    lx = (x - 44) % 24
    if lx in (0, 23) or v > 0.88 or (lx * 2 + y) % 13 == 0 and v > 0.1:
        col = (25, 20, 20, 255)
    if v > 0.88 and (x + y) % 3 == 0:
        col = (250, 250, 250, 255)
    return col


wings("monarch", monarch)

# ---------------------------------------------------------------- shields (128x128)
PLATE = (2, 2, 26, 46)
PLATE_BACK = (28, 2, 52, 46)
HANDLE = (52, 0, 84, 24)


def shield(name, face_top, face_bottom, rim, wood=(120, 85, 50), emblem=None, palette=None, extra=None):
    c = Canvas(128, 128, "s" + name)
    c.vgrad((0, 0, 54, 48), shade(wood, 0.9), shade(wood, 0.7))  # edges
    c.vgrad(PLATE, face_top, face_bottom)
    # back: planks
    c.vgrad(PLATE_BACK, wood, shade(wood, 0.8))
    for y in range(PLATE_BACK[1], PLATE_BACK[3]):
        if y % 6 == 0:
            for x in range(PLATE_BACK[0], PLATE_BACK[2]):
                c.px(x, y, shade(wood, 0.6))
    c.vgrad(HANDLE, shade(wood, 0.8), shade(wood, 0.6))
    if extra:
        extra(c)
    if rim:
        c.border(PLATE, rim, width=2)
        c.border(PLATE, shade(rim, 0.65), inset=2)
        for (x, y) in [(4, 4), (23, 4), (4, 43), (23, 43)]:  # rivets
            c.px(x, y, shade(rim, 1.3))
    if emblem:
        k = 2 if len(emblem[0]) * 2 <= 18 else 1
        w = len(emblem[0]) * k
        h = len(emblem) * k
        c.stamp(emblem, PLATE[0] + (24 - w) // 2, PLATE[1] + (44 - h) // 2 - 2, palette, k)
    c.save(f"shield_{name}.png")


def planks(c):
    x0, y0, x1, y1 = PLATE
    for y in range(y0, y1):
        for x in range(x0, x1):
            col = (150, 110, 65) if ((x - x0) // 6) % 2 == 0 else (135, 98, 58)
            if (x - x0) % 6 == 0:
                col = (95, 68, 40)
            c.px(x, y, shade(col, 1 + c.rnd.uniform(-0.06, 0.06)))
    for x in range(x0, x1):  # iron band
        for y in (y0 + 14, y0 + 15, y0 + 30, y0 + 31):
            c.px(x, y, (165, 165, 170, 255))


shield("oak", (150, 110, 65), (130, 95, 55), rim=(175, 175, 180), extra=planks)
shield("knight", (200, 35, 45), (140, 20, 30), rim=GOLD, emblem=CROSS, palette={"G": GOLD + (255,)})


def runes(c):
    x0, y0, x1, y1 = PLATE
    for _ in range(9):
        x = c.rnd.randrange(x0 + 4, x1 - 5)
        y = c.rnd.randrange(y0 + 4, y1 - 5)
        for (dx, dy) in c.rnd.choice([[(0, 0), (0, 1), (0, 2), (1, 1)], [(0, 0), (1, 0), (1, 1), (1, 2)], [(0, 1), (1, 0), (2, 1), (1, 2)]]):
            c.px(x + dx, y + dy, (190, 100, 255, 255))


shield("obsidian", (40, 22, 60), (15, 8, 25), rim=(90, 60, 120), wood=(40, 30, 50), extra=runes)
shield("royal", (40, 70, 170), (20, 35, 100), rim=GOLD, emblem=CROWN,
       palette={"G": GOLD + (255,), "R": (220, 30, 50, 255)})
shield("emerald", (40, 170, 90), (15, 100, 50), rim=(220, 255, 230), emblem=GEM,
       palette={"C": (70, 230, 120, 255), "W": (220, 255, 230, 255), "D": (20, 110, 55, 255)})
shield("frost", (200, 235, 255), (110, 170, 230), rim=(240, 250, 255), emblem=FLAKE,
       palette={"W": (255, 255, 255, 255)}, wood=(140, 160, 180))


def shield_flames(c):
    x0, y0, x1, y1 = PLATE
    for x in range(x0, x1):
        h = 14 + int(7 * abs(math.sin(x * 0.8))) + c.rnd.randrange(0, 5)
        for i in range(h):
            c.px(x, y1 - 1 - i, mix((255, 220, 80), (210, 40, 10), i / h))


shield("inferno", (40, 12, 12), (20, 6, 6), rim=(60, 60, 60), wood=(50, 35, 30), extra=shield_flames)
print("ok")
