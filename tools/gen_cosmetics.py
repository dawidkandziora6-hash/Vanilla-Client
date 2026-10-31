"""Built-in cosmetics v3: custom shapes (transparency) and animations (vertical frame strips).

Layouts (2x vanilla):
  cape/wings frame 128x64: cape front (2,2)-(22,34), back (24,2)-(44,34), sides, elytra (44,0)-(92,44)
  shield frame 128x128:    plate front (2,2)-(26,46), back (28,2)-(52,46), handle (52,0)-(84,24)
  totem frame 32x32 (item texture)
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(sys.argv[1])
OUT.mkdir(parents=True, exist_ok=True)
for old in OUT.glob("*.png"):
    old.unlink()
T = 0  # transparent marker


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def mul(c, f):
    return tuple(clamp(v * f) for v in c[:3])


def hsv(h, s, v):
    import colorsys
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return (int(r * 255), int(g * 255), int(b * 255))


# ================================================================ face painter helpers

class Face:
    """A w×h pixel face: color function + alpha mask, written into an image region."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def fill(self, fn):
        for y in range(self.h):
            for x in range(self.w):
                self.px[y][x] = fn(x, y)

    def mask(self, keep):
        for y in range(self.h):
            for x in range(self.w):
                if not keep(x, y):
                    self.px[y][x] = None

    def outline(self, col):
        """Darken pixels that touch transparency (gives cut shapes a clean edge)."""
        edge = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is None:
                    continue
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if 0 <= nx < self.w and 0 <= ny < self.h and self.px[ny][nx] is None:
                        edge.append((x, y))
                        break
        for x, y in edge:
            self.px[y][x] = mix(self.px[y][x], col, 0.55)

    def stamp(self, art, ox, oy, pal, k=1):
        for j, row in enumerate(art):
            for i, ch in enumerate(row):
                if ch in pal:
                    for a in range(k):
                        for b in range(k):
                            self.set(ox + i * k + a, oy + j * k + b, pal[ch])

    def blit(self, img, ox, oy, mirror=False):
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][self.w - 1 - x if mirror else x]
                if c is not None:
                    img.putpixel((ox + x, oy + y), tuple(c[:3]) + (255,))


def edges(img, face, fx, fy, side, cap):
    """Fills the thin side/top/bottom faces of a box next to its front face (fx,fy)."""
    for y in range(face.h):
        for col, sx in ((0, fx - side), (face.w - 1, fx + face.w)):
            c = face.get(col, y)
            if c is not None:
                for i in range(side):
                    img.putpixel((sx + i, fy + y), mul(c, 0.7) + (255,))
    for x in range(face.w):
        c = face.get(x, 0)
        if c is not None:
            for i in range(cap):
                img.putpixel((fx + x, fy - cap + i), mul(c, 0.85) + (255,))
        c = face.get(x, face.h - 1)
        if c is not None:
            for i in range(cap):
                img.putpixel((fx + face.w + x, fy - cap + i), mul(c, 0.6) + (255,))


def strip(frames, name):
    w, h = frames[0].size
    out = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        out.paste(f, (0, i * h))
    out.save(OUT / name)


# ================================================================ emblems

SWORD = ["....W....", "...WSW...", "...WSW...", "...WSW...", "...WSW...", "...WSW...", "...WSW...",
         "...WSW...", "...WSW...", ".GGGGGGG.", "....B....", "....B....", "...GGG..."]
CROWN = ["G...G...G", "GG.GGG.GG", "GGGGRGGGG", "GGGGGGGGG", ".GGGGGGG."]
EYE = ["...GGGGG...", ".GGgggggGG.", "GggggBggggG", "GgggBBBgggG", "GggggBggggG", ".GGgggggGG.", "...GGGGG..."]
GEM = ["..CCCCC..", ".CWCCCCC.", "CWCCCCCCC", "CCCCCCCCD", ".CCCCCCD.", "..CCCCD..", "...CCD...", "....D...."]
MOON = ["..YYYY..", ".YYY....", "YYY.....", "YYY.....", "YYY.....", "YYY.....", ".YYY....", "..YYYY.."]
TREE = ["....G....", "...GGG...", "..GGgGG..", ".GGGGGgG.", "..GgGGG..", ".GGGGgGG.", "GGgGGGGGG", "....B....", "....B...."]
FLAKE = ["..W.W.W..", "...WWW...", "W.W.W.W.W", ".WWWWWWW.", "WWW.W.WWW", ".WWWWWWW.", "W.W.W.W.W", "...WWW...", "..W.W.W.."]
CROSS = ["..GGG..", "..GGG..", "GGGGGGG", "GGGGGGG", "..GGG..", "..GGG..", "..GGG..", "..GGG.."]
RUNE = [["X.X", "XXX", ".X."], ["XX.", ".XX", "X.X"], [".X.", "XXX", "X.X"], ["X..", "XXX", "..X"]]

GOLD = (250, 205, 60)
FRAMES = 8


# ================================================================ capes

def cape_image(face, lining, name_frames=None):
    """Writes a cape face into a 128x64 frame (front, mirrored back, matching sides)."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    face.blit(img, 2, 2)
    back = Face(20, 32)
    back.fill(lambda x, y: mul(lining, 0.85 + 0.15 * (y / 31)) if face.get(19 - x, y) is not None else None)
    back.blit(img, 24, 2)
    for y in range(32):
        if face.get(0, y) is not None:
            img.putpixel((0, 2 + y), mul(lining, 0.7) + (255,))
            img.putpixel((1, 2 + y), mul(lining, 0.7) + (255,))
        if face.get(19, y) is not None:
            img.putpixel((22, 2 + y), mul(lining, 0.7) + (255,))
            img.putpixel((23, 2 + y), mul(lining, 0.7) + (255,))
    for x in range(20):
        img.putpixel((2 + x, 0), mul(lining, 0.8) + (255,))
        img.putpixel((2 + x, 1), mul(lining, 0.8) + (255,))
        if face.get(x, 31) is not None:
            img.putpixel((22 + x, 0), mul(lining, 0.6) + (255,))
            img.putpixel((22 + x, 1), mul(lining, 0.6) + (255,))
    return img


def cape(name, frames_fn, lining, n=1):
    frames = []
    for f in range(n):
        rnd = random.Random(name + str(f))
        face = Face(20, 32)
        frames_fn(face, f, rnd)
        frames.append(cape_image(face, lining))
    strip(frames, f"cape_{name}.png")


def grad(top, bottom, rnd=None, noise=0.05):
    def fn(x, y):
        c = mix(top, bottom, y / 31)
        if rnd:
            c = mul(c, 1 + rnd.uniform(-noise, noise))
        return c
    return fn


def border(face, col, inset=0):
    for x in range(inset, face.w - inset):
        for y in (inset, face.h - 1 - inset):
            if face.get(x, y) is not None:
                face.set(x, y, col)
    for y in range(inset, face.h - inset):
        for x in (inset, face.w - 1 - inset):
            if face.get(x, y) is not None:
                face.set(x, y, col)


# 1 Crimson Knight – V-shaped bottom, gold trim, sword
def crimson(face, f, rnd):
    face.fill(grad((200, 32, 44), (115, 10, 22), rnd))
    face.mask(lambda x, y: y < 26 + min(x, 19 - x) * 0.6)
    border(face, GOLD)
    face.stamp(SWORD, 6, 7, {"W": (235, 235, 240), "S": (170, 180, 195), "G": GOLD, "B": (110, 70, 40)})
    face.outline((60, 10, 10))


cape("crimson", crimson, (120, 20, 30))


# 2 Midnight – swallowtail, twinkling stars, moon
STAR_POS = [(random.Random(i).randrange(1, 19), random.Random(i * 7).randrange(1, 30)) for i in range(26)]


def midnight(face, f, rnd):
    face.fill(grad((34, 44, 104), (8, 10, 30)))
    face.mask(lambda x, y: not (y > 24 and abs(x - 9.5) < (y - 24) * 0.9))
    for i, (x, y) in enumerate(STAR_POS):
        phase = (f + i * 3) % FRAMES
        b = [1.0, 0.8, 0.5, 0.25, 0.1, 0.25, 0.5, 0.8][phase]
        if face.get(x, y) is not None:
            face.set(x, y, mix((40, 50, 110), (255, 250, 220), b))
            if b > 0.9:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if face.get(x + dx, y + dy) is not None:
                        face.set(x + dx, y + dy, mix((40, 50, 110), (255, 250, 220), 0.45))
    face.stamp(MOON, 11, 3, {"Y": (255, 236, 160)})
    face.outline((5, 5, 20))


cape("midnight", midnight, (20, 26, 60), FRAMES)


# 3 Aurora – flowing bands
def aurora(face, f, rnd):
    t = f / FRAMES * math.tau

    def fn(x, y):
        band = math.sin(x * 0.35 + y * 0.22 + t) * 0.5 + 0.5
        band2 = math.sin(x * 0.2 - y * 0.15 + t * 0.5 + 1.3) * 0.5 + 0.5
        base = mix((10, 18, 40), (20, 10, 45), y / 31)
        glow = mix((40, 230, 170), (170, 80, 240), band2)
        return mix(base, glow, band ** 2 * 0.9)
    face.fill(fn)
    for x, y in STAR_POS[:8]:
        face.set(x, y, (255, 255, 255))
    face.outline((0, 0, 0))


cape("aurora", aurora, (20, 20, 50), FRAMES)


# 4 Ender – ragged bottom, drifting particles, eye
def ender(face, f, rnd):
    r0 = random.Random("ender-edge")
    edge = [27 + r0.randrange(0, 5) for _ in range(20)]
    face.fill(grad((32, 18, 50), (8, 4, 14)))
    face.mask(lambda x, y: y < edge[x])
    for i in range(14):
        rr = random.Random(i)
        x = rr.randrange(0, 20)
        y = (rr.randrange(0, 32) - f * 2 - i) % 32
        if face.get(x, y) is not None:
            face.set(x, y, (200, 100, 255) if i % 2 else (150, 70, 220))
    face.stamp(EYE, 4, 8, {"G": (40, 140, 110), "g": (90 + f * 12, 230, 180), "B": (10, 30, 25)})
    face.outline((0, 0, 0))


cape("ender", ender, (20, 10, 30), FRAMES)


# 5 Forest – leafy bottom edge, pine
def forest(face, f, rnd):
    face.fill(grad((120, 190, 100), (40, 110, 45), rnd))
    face.mask(lambda x, y: y < 27 + (2 if x % 4 in (1, 2) else 0) + (1 if x % 8 < 4 else 0))
    for x in range(20):
        h = int(5 + 3 * math.sin(x * 0.7))
        for y in range(28 - h, 32):
            if face.get(x, y) is not None:
                face.set(x, y, mul((30, 72, 30), 1 + rnd.uniform(-0.1, 0.1)))
    face.stamp(TREE, 5, 6, {"G": (25, 90, 35), "g": (70, 150, 70), "B": (90, 60, 30)})
    face.outline((15, 40, 15))


cape("forest", forest, (30, 70, 30))


# 6 Royal – rounded bottom, crown, ermine
def royal(face, f, rnd):
    face.fill(grad((130, 45, 170), (70, 20, 100), rnd))
    face.mask(lambda x, y: (x - 9.5) ** 2 / 110 + (y - 22) ** 2 / 100 < 1.0 or y < 22)
    for y in range(24, 32):
        for x in range(20):
            if face.get(x, y) is not None:
                face.set(x, y, (242, 242, 236))
    for dx, dy in ((4, 26), (9, 28), (14, 26), (7, 30), (12, 30)):
        face.set(dx, dy, (25, 25, 25))
        face.set(dx, dy + 1, (25, 25, 25))
    border(face, GOLD)
    face.stamp(CROWN, 5, 8, {"G": GOLD, "R": (220, 30, 50)}, 1)
    face.outline((40, 10, 60))


cape("royal", royal, (60, 20, 80))


# 7 Ocean – moving waves, wavy bottom
def ocean(face, f, rnd):
    t = f / FRAMES * math.tau
    face.fill(grad((50, 160, 220), (10, 50, 110)))
    face.mask(lambda x, y: y < 29 + round(1.6 * math.sin(x * 0.7 + t)))
    for k in range(5):
        base = 5 + k * 6
        for x in range(20):
            y = base + round(1.5 * math.sin((x + k * 3) * 0.6 + t))
            if face.get(x, y) is not None:
                face.set(x, y, (160, 235, 255))
            if face.get(x, y + 1) is not None:
                face.set(x, y + 1, (90, 185, 235))
    face.outline((5, 30, 70))


cape("ocean", ocean, (15, 60, 110), FRAMES)


# 8 Cherry – falling petals, scalloped bottom
def cherry(face, f, rnd):
    face.fill(grad((255, 196, 220), (232, 128, 176)))
    face.mask(lambda x, y: y < 28 + (2 if (x % 5) in (1, 2, 3) else 0))
    for i in range(18):
        rr = random.Random("p" + str(i))
        x = (rr.randrange(0, 20) + f // 2) % 20
        y = (rr.randrange(0, 32) + f * 2) % 32
        col = rr.choice([(255, 255, 255), (255, 214, 232), (255, 160, 200)])
        if face.get(x, y) is not None:
            face.set(x, y, col)
        if face.get(x + 1, y) is not None:
            face.set(x + 1, y, mul(col, 0.92))
    for i in range(16):
        face.set(2 + i, 4 + i // 2, (95, 55, 50))
    face.outline((150, 70, 110))


cape("cherry", cherry, (180, 90, 130), FRAMES)


# 9 Inferno – burning hem, flame tongues flicker every frame
def inferno(face, f, rnd):
    def height(x):
        a = 9 + 7 * abs(math.sin(x * 0.55 + f * 0.785))
        b = 4 * abs(math.sin(x * 1.3 - f * 1.57 + 0.7))
        return a + b + random.Random(f * 31 + x).randrange(0, 3)
    hs = [height(x) for x in range(20)]
    face.fill(grad((46, 12, 10), (24, 6, 6), rnd, 0.06))
    for x in range(20):
        h = hs[x]
        for i in range(int(h)):
            y = 31 - i
            t = i / h
            face.set(x, y, mix(mix((255, 250, 190), (255, 190, 40), min(1, t * 2)), (200, 30, 10), max(0, t * 1.4 - 0.4)))
    for i in range(6):
        rr = random.Random("ember" + str(i))
        x = rr.randrange(0, 20)
        y = (rr.randrange(0, 20) - f * 3) % 20
        face.set(x, y, (255, 160, 40))
    face.mask(lambda x, y: y < 30 or x % 3 != (f % 3))
    face.outline((40, 8, 0))


cape("inferno", inferno, (40, 10, 10), FRAMES)


# 10 Diamond – sparkles
def diamond(face, f, rnd):
    face.fill(grad((120, 236, 236), (30, 140, 160)))
    face.mask(lambda x, y: y < 29 or abs(x - 9.5) < (31 - y) * 3)
    border(face, (225, 255, 255))
    face.stamp(GEM, 6, 9, {"C": (90, 225, 225), "W": (255, 255, 255), "D": (30, 120, 140)})
    for i, (x, y) in enumerate(STAR_POS[:10]):
        if (f + i) % 4 == 0 and face.get(x, y) is not None:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                face.set(x + dx, y + dy, (255, 255, 255))
    face.outline((20, 90, 110))


cape("diamond", diamond, (40, 120, 140), FRAMES)


# 11 Wanderer – torn cloth with holes
def wanderer(face, f, rnd):
    r0 = random.Random("tatter")
    edge = [24 + r0.randrange(0, 8) for _ in range(20)]
    holes = {(r0.randrange(2, 18), r0.randrange(6, 24)) for _ in range(5)}
    face.fill(grad((122, 96, 70), (78, 58, 42), rnd, 0.08))
    face.mask(lambda x, y: y < edge[x] and (x, y) not in holes)
    for y in range(0, 32, 5):
        for x in range(20):
            if face.get(x, y) is not None and (x + y) % 3 == 0:
                face.set(x, y, mul(face.get(x, y), 0.85))
    face.outline((40, 28, 18))


cape("wanderer", wanderer, (60, 45, 32))


# 12 Prism – scrolling rainbow
def prism(face, f, rnd):
    face.fill(lambda x, y: hsv((y / 32 + f / FRAMES) % 1, 0.65, 0.95))
    face.mask(lambda x, y: y < 28 + (x % 2) * 2)
    face.outline((30, 30, 30))


cape("prism", prism, (60, 60, 60), FRAMES)


# 13 Void Rift – pulsing crack
def rift(face, f, rnd):
    face.fill(grad((14, 8, 22), (4, 2, 8), rnd, 0.1))
    pulse = 0.5 + 0.5 * math.sin(f / FRAMES * math.tau)
    x = 10
    for y in range(32):
        x += random.Random("rift" + str(y)).choice([-1, 0, 0, 1])
        x = max(3, min(16, x))
        for dx in range(-1, 2):
            c = mix((120, 40, 200), (240, 170, 255), pulse) if dx == 0 else mix((60, 20, 100), (160, 80, 230), pulse)
            face.set(x + dx, y, c)
    face.mask(lambda xx, y: y < 30 or xx % 3 != 0)
    face.outline((0, 0, 0))


cape("rift", rift, (10, 5, 16), FRAMES)


# ================================================================ wings (elytra region)

def wings_image(face, cape_tint):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    # front & back of the wing, edges left transparent for shaped wings
    face.blit(img, 48, 4)
    face.blit(img, 72, 4, mirror=True)
    edges(img, face, 48, 4, 4, 4)
    # matching cape area (vanilla uses it when no cape is chosen)
    for y in range(32):
        for x in range(20):
            c = face.get(x, min(39, y + 4))
            if c is not None:
                img.putpixel((2 + x, 2 + y), tuple(c) + (255,))
    return img


def wings(name, fn, n=1):
    frames = []
    for f in range(n):
        rnd = random.Random(name + str(f))
        face = Face(20, 40)
        fn(face, f, rnd)
        frames.append(wings_image(face, None))
    strip(frames, f"wings_{name}.png")


def scallop_mask(depth=5, period=5):
    return lambda x, y: y < 40 - depth + round(depth * abs(math.sin(math.pi * x / period)))


def feathers(face, light, dark, tip=None):
    def fn(x, y):
        r = y // 4
        sx = (x + (r % 2) * 2) % 4
        c = mix(light, dark, (y / 39) ** 1.4)
        if y % 4 == 3 or sx == 0:
            c = mul(c, 0.86)
        if tip and y > 32:
            c = mix(c, tip, (y - 32) / 8)
        return c
    face.fill(fn)


def angel(face, f, rnd):
    feathers(face, (255, 255, 252), (210, 210, 205), (255, 220, 120))
    face.mask(scallop_mask(7, 4))
    face.outline((150, 150, 150))


def dragon(face, f, rnd):
    def fn(x, y):
        r = y // 3
        sx = (x + (r % 2) * 2) % 4
        c = mix((110, 36, 150), (55, 16, 80), y / 39)
        if y % 3 == 2 or sx == 0:
            c = mul(c, 0.6)
        elif sx in (1, 2) and y % 3 == 0:
            c = mix(c, (230, 110, 250), 0.5)
        return c
    face.fill(fn)
    for bone_x in (0, 7, 14):
        for y in range(40):
            face.set(bone_x + y // 10, y, (40, 12, 55))
    face.mask(lambda x, y: y < 33 + round(6 * abs(math.sin(math.pi * x / 7))))
    face.outline((20, 5, 30))


def phoenix(face, f, rnd):
    tips = [round(4 + 3 * abs(math.sin(x * 0.8 + f * 0.9)) + random.Random(f * 13 + x).randrange(0, 2)) for x in range(20)]
    feathers(face, (255, 225, 80), (215, 55, 20))
    face.mask(lambda x, y: y < 33 + tips[x] % 7)
    for x in range(20):
        y = 32 + tips[x] % 7
        for d in range(3):
            if face.get(x, y - d) is not None:
                face.set(x, y - d, mix((255, 245, 160), (255, 120, 20), d / 3))
    face.outline((90, 20, 0))


def frost(face, f, rnd):
    feathers(face, (240, 252, 255), (120, 185, 240))
    face.mask(lambda x, y: y < 32 + (8 if x % 5 == 2 else 4 if x % 5 in (1, 3) else 0))
    for i, (x, y) in enumerate(STAR_POS[:9]):
        if (f + i) % 4 == 0:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                if face.get(x + dx, y + dy) is not None:
                    face.set(x + dx, y + dy, (255, 255, 255))
    face.outline((70, 120, 180))


def phantom(face, f, rnd):
    pulse = 0.5 + 0.5 * math.sin(f / FRAMES * math.tau)
    face.fill(lambda x, y: mix((95, 110, 150), (50, 60, 90), y / 39))
    for vx in (2, 9, 16):
        for y in range(40):
            face.set(vx + (y // 8), y, mix((60, 160, 140), (140, 255, 220), pulse))
    holes = {(5, 22), (12, 28), (6, 32), (14, 18), (11, 12)}
    face.mask(lambda x, y: y < 34 + round(5 * abs(math.sin(math.pi * x / 7))) and (x, y) not in holes)
    face.outline((25, 30, 50))


def void(face, f, rnd):
    face.fill(lambda x, y: mix((30, 12, 46), (6, 3, 12), y / 39))
    for i in range(22):
        rr = random.Random("v" + str(i))
        x, y = rr.randrange(0, 20), rr.randrange(0, 40)
        b = [1, .7, .4, .2, .1, .2, .4, .7][(f + i) % FRAMES]
        face.set(x, y, mix((30, 12, 46), rr.choice([(220, 140, 255), (255, 255, 255)]), b))
    face.mask(lambda x, y: y < 31 + random.Random("vw" + str(x)).randrange(0, 9))
    face.outline((0, 0, 0))


def monarch(face, f, rnd):
    def fn(x, y):
        c = mix((255, 170, 45), (228, 88, 10), y / 39)
        if (x * 2 + y) % 11 == 0 or x in (0, 19):
            c = (25, 20, 20)
        return c
    face.fill(fn)
    face.mask(lambda x, y: ((x - 10) / 10.5) ** 2 + ((y - 13) / 13.5) ** 2 < 1.0 or ((x - 9) / 8) ** 2 + ((y - 31) / 9) ** 2 < 1.0)
    face.outline((25, 20, 20))
    for y in range(40):
        for x in range(20):
            c = face.get(x, y)
            if c == (25, 20, 20) or c is None:
                continue
            nb = [face.get(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(n is None for n in nb) and (x + y) % 3 == 0:
                face.set(x, y, (250, 250, 250))


def fairy(face, f, rnd):
    t = f / FRAMES
    face.fill(lambda x, y: hsv(0.75 + 0.25 * math.sin((x + y) * 0.15 + t * math.tau) * 0.5, 0.35, 1.0))
    face.mask(lambda x, y: ((x - 10) / 10.5) ** 2 + ((y - 12) / 12.5) ** 2 < 1.0 or ((x - 8) / 7) ** 2 + ((y - 30) / 10) ** 2 < 1.0)
    for i, (x, y) in enumerate(STAR_POS[:12]):
        if (f + i) % 3 == 0 and face.get(x, y) is not None:
            face.set(x, y, (255, 255, 255))
    face.outline((170, 120, 200))


def bat(face, f, rnd):
    face.fill(lambda x, y: mix((52, 46, 58), (22, 18, 26), y / 39))
    for bx in (0, 6, 12, 18):
        for y in range(40):
            face.set(min(19, bx + y // 14), y, (12, 10, 14))
    face.mask(lambda x, y: y < 33 + round(6 * abs(math.sin(math.pi * x / 6))))
    face.outline((0, 0, 0))


wings("angel", angel)
wings("dragon", dragon)
wings("phoenix", phoenix, FRAMES)
wings("frost", frost, FRAMES)
wings("phantom", phantom, FRAMES)
wings("void", void, FRAMES)
wings("monarch", monarch)
wings("fairy", fairy, FRAMES)
wings("bat", bat)

# ================================================================ shields


def shield_image(face, wood=(120, 85, 50), shaped=True):
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    face.blit(img, 2, 2)
    back = Face(24, 44)
    back.fill(lambda x, y: mul(wood, 0.8 + (0.1 if y % 6 == 0 else 0)) if face.get(23 - x, y) is not None else None)
    back.blit(img, 28, 2)
    edges(img, face, 2, 2, 2, 2)
    if not shaped:
        for x in range(54):
            for y in range(2):
                img.putpixel((x, y), mul(wood, 0.8) + (255,))
        for y in range(2, 46):
            for x in (0, 1, 26, 27):
                img.putpixel((x, y), mul(wood, 0.7) + (255,))
    for y in range(24):
        for x in range(52, 84):
            img.putpixel((x, y), mul(wood, 0.65 + 0.1 * ((x + y) % 3 == 0)) + (255,))
    return img


def shield(name, fn, wood=(120, 85, 50), n=1, shaped=True):
    frames = []
    for f in range(n):
        rnd = random.Random(name + str(f))
        face = Face(24, 44)
        fn(face, f, rnd)
        frames.append(shield_image(face, wood, shaped))
    strip(frames, f"shield_{name}.png")


def heater(x, y):
    return y < 26 or ((x - 11.5) / 12) ** 2 + ((y - 26) / 18) ** 2 < 1.0


def kite(x, y):
    return abs(x - 11.5) < 12 * (1 - max(0, y - 14) / 30) and (y > 3 or abs(x - 11.5) < 10)


def rim(face, col, w=2):
    for y in range(face.h):
        for x in range(face.w):
            if face.get(x, y) is None:
                continue
            near = any(face.get(x + dx, y + dy) is None for dx in range(-w, w + 1) for dy in range(-w, w + 1)
                       if abs(dx) + abs(dy) <= w)
            if near:
                face.set(x, y, col)


def knight(face, f, rnd):
    face.fill(grad((205, 36, 46), (140, 20, 30), rnd))
    face.mask(heater)
    rim(face, GOLD)
    face.stamp(CROSS, 5, 8, {"G": GOLD}, 2)
    face.outline((90, 60, 10))


def buckler(face, f, rnd):
    def fn(x, y):
        c = (150, 110, 65) if (x // 5) % 2 == 0 else (132, 96, 56)
        return mul(c, 1 + rnd.uniform(-0.06, 0.06))
    face.fill(fn)
    face.mask(lambda x, y: (x - 11.5) ** 2 + (y - 21.5) ** 2 < 12 ** 2)
    rim(face, (170, 170, 178))
    for y in range(18, 26):
        for x in range(8, 16):
            if (x - 11.5) ** 2 + (y - 21.5) ** 2 < 4 ** 2:
                face.set(x, y, (200, 200, 208) if y < 22 else (150, 150, 158))
    face.outline((60, 60, 66))


def royal_s(face, f, rnd):
    face.fill(grad((44, 76, 180), (20, 36, 100), rnd))
    face.mask(kite)
    rim(face, GOLD)
    face.stamp(CROWN, 3, 12, {"G": GOLD, "R": (220, 30, 50)}, 2)
    face.outline((20, 20, 60))


def emerald(face, f, rnd):
    face.fill(grad((44, 176, 96), (16, 100, 52), rnd))
    face.mask(lambda x, y: not ((x < 3 or x > 20) and (y < 3 or y > 40)))
    rim(face, (220, 255, 230), 1)
    face.stamp(GEM, 3, 14, {"C": (80, 235, 130), "W": (230, 255, 236), "D": (20, 110, 55)}, 2)
    glint = (f * 6) % 60 - 10
    for y in range(44):
        for x in range(24):
            if abs((x + y) - glint) < 2 and face.get(x, y) is not None:
                face.set(x, y, mix(face.get(x, y), (255, 255, 255), 0.6))
    face.outline((10, 60, 30))


def frost_s(face, f, rnd):
    face.fill(grad((214, 240, 255), (120, 176, 232), rnd))
    face.mask(lambda x, y: abs(x - 11.5) + abs(y - 21.5) * 0.55 < 13 or (y < 30 and abs(x - 11.5) < 9))
    rim(face, (250, 254, 255), 1)
    face.stamp(FLAKE, 3, 13, {"W": (255, 255, 255)}, 2)
    for i, (x, y) in enumerate(STAR_POS[:8]):
        if (f + i) % 4 == 0 and face.get(x, y + 6) is not None:
            face.set(x, y + 6, (255, 255, 255))
    face.outline((90, 140, 200))


def obsidian(face, f, rnd):
    face.fill(grad((44, 24, 66), (16, 8, 26), rnd, 0.1))
    face.mask(heater)
    rim(face, (80, 54, 110))
    pulse = 0.5 + 0.5 * math.sin(f / FRAMES * math.tau)
    rr = random.Random("runes")
    for i in range(7):
        r = RUNE[i % len(RUNE)]
        x, y = rr.randrange(3, 18), rr.randrange(4, 34)
        face.stamp(r, x, y, {"X": mix((90, 40, 150), (220, 150, 255), (pulse + i * 0.15) % 1)})
    face.outline((0, 0, 0))


def inferno_s(face, f, rnd):
    tips = [round(4 * abs(math.sin(x * 0.8 + f)) + random.Random(f * 7 + x).randrange(0, 3)) for x in range(24)]
    face.fill(grad((44, 12, 12), (22, 6, 6)))
    for x in range(24):
        h = 18 + tips[x]
        for i in range(h):
            face.set(x, 43 - i, mix((255, 225, 90), (210, 40, 10), i / h))
    face.mask(lambda x, y: y > 6 - tips[x] % 5)
    rim(face, (60, 60, 60), 1)
    face.outline((30, 8, 0))


def dragon_s(face, f, rnd):
    def fn(x, y):
        r = y // 3
        sx = (x + (r % 2) * 2) % 4
        c = mix((70, 150, 70), (30, 80, 40), y / 43)
        if y % 3 == 2 or sx == 0:
            c = mul(c, 0.6)
        return c
    face.fill(fn)
    face.mask(heater)
    rim(face, (200, 180, 120))
    face.outline((20, 40, 20))


def tower(face, f, rnd):
    face.fill(grad((120, 120, 128), (84, 84, 92), rnd))
    rim(face, (60, 60, 66), 1)
    for y in (6, 21, 36):
        for x in range(24):
            face.set(x, y, (150, 150, 158))
    for x, y in ((4, 4), (19, 4), (4, 39), (19, 39), (11, 13), (11, 29)):
        face.set(x, y, (210, 210, 220))
        face.set(x + 1, y, (170, 170, 180))


shield("knight", knight)
shield("buckler", buckler)
shield("royal", royal_s)
shield("emerald", emerald, n=FRAMES)
shield("frost", frost_s, wood=(140, 160, 180), n=FRAMES)
shield("obsidian", obsidian, wood=(40, 30, 50), n=FRAMES)
shield("inferno", inferno_s, wood=(50, 35, 30), n=FRAMES)
shield("dragon", dragon_s)
shield("tower", tower, wood=(80, 80, 86), shaped=False)

# ================================================================ totems (32x32 item textures)

TOTEM = [
    "................",
    ".....HHHHHH.....",
    "....HhhhhhhH....",
    "....HhEhhEhH....",
    "....HhhhhhhH....",
    ".....HhMMhH.....",
    "..WW.HHHHHH.WW..",
    ".WwwWBbbbbbBWwwW",
    ".WwwWBbGGbbBWwwW",
    "..WWwBbGGbbBwWW.",
    "....WBbbbbbBW...",
    ".....BbbbbbB....",
    ".....BBbbbBB....",
    "......BbbbB.....",
    "......BBBBB.....",
    "................",
]

TOTEMS = {
    "king": ((150, 105, 20), (250, 205, 70), (40, 20, 10), (120, 70, 10), (165, 115, 25), (245, 200, 80), (60, 220, 100), (190, 140, 40), (255, 225, 120)),
    "diamond": ((30, 120, 140), (120, 235, 235), (10, 40, 60), (20, 90, 110), (35, 135, 150), (130, 240, 240), (255, 255, 255), (40, 150, 170), (190, 255, 255)),
    "emerald": ((20, 110, 50), (80, 220, 120), (10, 40, 20), (15, 80, 40), (25, 125, 60), (90, 230, 130), (255, 230, 90), (30, 140, 70), (150, 255, 170)),
    "ender": ((25, 15, 40), (90, 60, 140), (110, 255, 200), (60, 30, 90), (30, 20, 50), (120, 80, 180), (200, 100, 255), (45, 25, 70), (160, 110, 230)),
    "blood": ((110, 10, 15), (220, 40, 50), (255, 230, 120), (70, 5, 10), (120, 12, 18), (230, 60, 60), (255, 200, 60), (90, 10, 15), (200, 50, 60)),
    "frost": ((120, 170, 210), (235, 250, 255), (30, 60, 110), (90, 140, 190), (130, 180, 220), (240, 252, 255), (90, 180, 255), (150, 200, 235), (255, 255, 255)),
}


def totem_frame(pal, f, glow_eyes=False, pulse_gem=True):
    H, h, E, M, B, b, G, W, w = pal
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    t = f / FRAMES * math.tau
    colors = {"H": H, "h": h, "E": E, "M": M, "B": B, "b": b, "G": G, "W": W, "w": w}
    for y, row in enumerate(TOTEM):
        for x, ch in enumerate(row):
            if ch not in colors:
                continue
            c = colors[ch]
            if ch == "G" and pulse_gem:
                c = mix(c, (255, 255, 255), 0.25 + 0.25 * math.sin(t))
            if ch == "E" and glow_eyes:
                c = mix(c, (255, 255, 255), 0.5 + 0.5 * math.sin(t * 2))
            for dy in range(2):
                for dx in range(2):
                    shade = 1.0 if (dx == 0 and dy == 0) else 0.92
                    img.putpixel((x * 2 + dx, y * 2 + dy), mul(c, shade) + (255,))
    # moving glint across the figure
    gx = (f * 6) % 48 - 8
    for y in range(32):
        for x in range(32):
            p = img.getpixel((x, y))
            if p[3] and abs((x + y) - gx - 8) < 2:
                img.putpixel((x, y), mix(p, (255, 255, 255), 0.45) + (255,))
    return img


for name, pal in TOTEMS.items():
    frames = [totem_frame(pal, f, glow_eyes=name in ("ender", "blood")) for f in range(FRAMES)]
    strip(frames, f"totem_{name}.png")

# 3D statue: model texture (16x16 color blocks) + an isometric preview for the menu
model = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
blocks = {(0, 0): (250, 205, 70), (8, 0): (205, 150, 35), (0, 8): (255, 236, 150), (8, 8): (60, 220, 100)}
for (ox, oy), c in blocks.items():
    for y in range(8):
        for x in range(8):
            n = 1 + random.Random(x * 9 + y + ox * 3 + oy).uniform(-0.06, 0.06)
            model.putpixel((ox + x, oy + y), mul(c, n) + (255,))
# face on the head block (eyes + mouth)
for (x, y) in ((2, 3), (5, 3)):
    model.putpixel((x, y), (40, 20, 10, 255))
for x in range(3, 5):
    model.putpixel((x, 5), (120, 70, 10, 255))
model.save(OUT / "totem_statue_model.png")

# preview: simple 3D-ish render (front + right side + top) at 32x32
prev = Image.new("RGBA", (32, 32), (0, 0, 0, 0))


def box(x0, y0, w, h, d, col):
    for y in range(h):
        for x in range(w):
            prev.putpixel((x0 + x, y0 + y), mul(col, 0.95) + (255,))
    for i in range(d):
        for y in range(h):
            prev.putpixel((x0 + w + i, y0 + y - i - 1), mul(col, 0.7) + (255,))
        for x in range(w):
            prev.putpixel((x0 + x + i + 1, y0 - i - 1), mul(col, 1.15) + (255,))


box(8, 27, 14, 3, 3, (205, 150, 35))
box(2, 13, 6, 9, 2, (255, 236, 150))
box(22, 13, 6, 9, 2, (255, 236, 150))
box(10, 13, 10, 14, 3, (240, 196, 60))
box(11, 4, 9, 9, 3, (250, 205, 70))
prev.putpixel((13, 7), (40, 20, 10, 255))
prev.putpixel((17, 7), (40, 20, 10, 255))
for x in range(14, 17):
    prev.putpixel((x, 10), (120, 70, 10, 255))
for y in range(17, 21):
    for x in range(13, 17):
        prev.putpixel((x, y), (60, 220, 100, 255))
prev.save(OUT / "totem_statue.png")
print("ok")
