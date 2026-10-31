"""Vanilla Client cosmetics v5 – HD (4x) capes, wings and shields.

Every design = background + lighting/folds + custom outline + trim + emblem + glow,
animated ones are vertical strips of NF frames (played at 10 fps by the mod).

Layouts at 4x vanilla:
  cape/wings frame 256x128: cape front (4,4) 40x64, back (48,4), sides (0,4)/(44,4) 4 wide, top (4,0), bottom (44,0)
                            elytra front (96,8) 40x80, back (144,8), sides (88,8)/(136,8) 8 wide, top (96,0), bottom (136,0)
  shield frame 256x256:     plate front (4,4) 48x88, back (56,4), sides (0,4)/(52,4), top (4,0), bottom (52,0), handle (104,0) 64x48
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = Path(sys.argv[1])
OUT.mkdir(parents=True, exist_ok=True)
for old in OUT.glob("*.png"):
    old.unlink()
NF = 12
TAU = math.tau


def C(hexs):
    hexs = hexs.lstrip("#")
    return np.array([int(hexs[i:i + 2], 16) / 255 for i in (0, 2, 4)])


# ============================================================ noise

def vnoise(h, w, cells, seed, ox=0.0, oy=0.0, octaves=3):
    """Tileable fractal value noise in 0..1. ox/oy scroll in units of the full period (0..1 = one loop)."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        cx = max(1, cells * 2 ** o)
        cs = w / cx
        cy = max(1, int(math.ceil(h / cs)))
        g = rng.random((cy, cx))
        ys = (np.arange(h)[:, None] / cs + oy * cy) % cy
        xs = (np.arange(w)[None, :] / cs + ox * cx) % cx
        x0, y0 = np.floor(xs).astype(int), np.floor(ys).astype(int)
        fx, fy = xs - x0, ys - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        x1, y1 = (x0 + 1) % cx, (y0 + 1) % cy
        a = g[y0 % cy, x0 % cx] * (1 - fx) + g[y0 % cy, x1] * fx
        b = g[y1, x0 % cx] * (1 - fx) + g[y1, x1] * fx
        out += amp * (a * (1 - fy) + b * fy)
        tot += amp
        amp *= 0.5
    return out / tot


# ============================================================ face

class Face:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.c = np.zeros((h, w, 3))
        self.a = np.ones((h, w), bool)
        self.e = np.zeros((h, w, 3))  # emissive (glows)
        self.Y, self.X = np.mgrid[0:h, 0:w].astype(float)

    # ---- fills
    def grad(self, top, bot, power=1.0):
        t = (self.Y / (self.h - 1)) ** power
        self.c = top * (1 - t[..., None]) + bot * t[..., None]
        return self

    def tint(self, col, amt):
        """amt: scalar or (h,w) map."""
        amt = np.asarray(amt, float)
        if amt.ndim == 2:
            amt = amt[..., None]
        self.c = self.c * (1 - amt) + col * amt
        return self

    def mul(self, m):
        m = np.asarray(m, float)
        if m.ndim == 2:
            m = m[..., None]
        self.c = self.c * m
        return self

    def cloth(self, seed, amt=0.06):
        """Fine woven texture."""
        n = np.random.default_rng(seed).random((self.h, self.w))
        weave = ((self.X + self.Y) % 2) * 0.5 + 0.5
        return self.mul(1 + (n - 0.5) * amt + (weave - 0.75) * amt * 0.6)

    def folds(self, n=3, amt=0.12, phase=0.0):
        s = np.sin(TAU * n * self.X / self.w + phase)
        return self.mul(1 + amt * s)

    def light(self, top=1.12, bottom=0.82):
        return self.mul(top + (bottom - top) * self.Y / (self.h - 1))

    # ---- shape
    def shape(self, mask):
        self.a &= mask
        return self

    def erode(self, k):
        m = self.a.copy()
        for _ in range(k):
            p = np.pad(m, 1, constant_values=False)
            m = m & p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
        return m

    def band(self, k):
        return self.a & ~self.erode(k)

    def trim(self, col, k=2, pattern=True, glow=0.0):
        b = self.band(k)
        inner = self.band(k + 1) & ~b
        c = np.broadcast_to(col, self.c.shape).copy()
        if pattern:
            stripe = (((self.X + self.Y) // 2) % 3 == 0)
            c = np.where(stripe[..., None], c * 1.25, c)
            c = np.where(((self.X * 2 + self.Y) % 7 == 0)[..., None], c * 0.75, c)
        self.c = np.where(b[..., None], c, self.c)
        self.c = np.where(inner[..., None], self.c * 0.55, self.c)  # stitched shadow line
        if glow:
            self.e += b[..., None] * col * glow
        return self

    def outline(self, col=None, amt=0.6):
        b = self.band(1)
        col = np.zeros(3) if col is None else col
        self.c = np.where(b[..., None], self.c * (1 - amt) + col * amt, self.c)
        return self

    # ---- drawing
    def layer(self, draw_fn, ss=4):
        """Anti-aliased coverage map from PIL drawing in face coordinates (scaled by ss)."""
        im = Image.new("L", (self.w * ss, self.h * ss), 0)
        draw_fn(ImageDraw.Draw(im), ss)
        im = im.resize((self.w, self.h), Image.LANCZOS)
        return np.asarray(im, float) / 255

    def paint(self, cov, col, glow=0.0, shadow=0.0):
        if shadow:
            sh = np.roll(np.roll(cov, 1, 0), 1, 1)
            self.tint(np.zeros(3), sh * shadow)
        self.tint(col, cov)
        if glow:
            self.e += cov[..., None] * col * glow
        return self

    def dot(self, x, y, col, r=0.8, glow=0.0):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.c[y, x] = col
            if glow:
                self.e[y, x] += col * glow
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= x + dx < self.w and 0 <= y + dy < self.h:
                        self.c[y + dy, x + dx] = self.c[y + dy, x + dx] * (1 - r * 0.5) + col * r * 0.5
        return self

    def finish(self, glow_radius=2.0):
        c = np.clip(self.c, 0, 1)
        if self.e.max() > 0:
            em = Image.fromarray((np.clip(self.e, 0, 1) * 255).astype(np.uint8))
            bl = np.asarray(em.filter(ImageFilter.GaussianBlur(glow_radius)), float) / 255
            c = 1 - (1 - c) * (1 - np.clip(bl * 1.4 + self.e * 0.6, 0, 1))  # screen blend
        return np.clip(c, 0, 1), self.a

    def rgba(self):
        c, a = self.finish()
        out = np.zeros((self.h, self.w, 4), np.uint8)
        out[..., :3] = (c * 255).astype(np.uint8)
        out[..., 3] = a * 255
        return out


# ============================================================ shapes (return bool masks)

def m_straight(f):
    return np.ones((f.h, f.w), bool)


def m_vcut(f, depth=10):
    return f.Y < f.h - depth + np.abs(f.X - (f.w - 1) / 2) * (2 * depth / f.w)


def m_swallow(f, depth=14):
    return ~((f.Y > f.h - depth) & (np.abs(f.X - (f.w - 1) / 2) < (f.Y - (f.h - depth)) * (f.w / 2 / depth)))


def m_round(f, r=None):
    r = r or f.w / 2
    cy = f.h - r
    return (f.Y < cy) | (((f.X - (f.w - 1) / 2) / (f.w / 2)) ** 2 + ((f.Y - cy) / r) ** 2 < 1)


def m_ragged(f, seed, amp=8, holes_n=0):
    rng = np.random.default_rng(seed)
    edge = f.h - amp + np.convolve(rng.integers(0, amp, f.w + 4), np.ones(3) / 3, "valid")[:f.w]
    holes = np.ones((f.h, f.w), bool)
    for _ in range(holes_n):
        hx, hy, hr = rng.integers(4, f.w - 4), rng.integers(f.h // 3, f.h - amp - 4), rng.uniform(1.2, 2.2)
        holes &= (f.X - hx) ** 2 + (f.Y - hy) ** 2 > hr * hr
    return (f.Y < edge[None, :]) & holes


def m_scallop(f, n=5, depth=6):
    p = f.w / n
    return f.Y < f.h - depth + depth * np.abs(np.sin(math.pi * f.X / p))


def m_points(f, n=4, depth=10):
    p = f.w / n
    tri = 1 - np.abs(((f.X + 0.5) % p) / p * 2 - 1)
    return f.Y < f.h - depth + depth * tri


def m_flames(f, t, depth=12, seed=1):
    xs = f.X[0]
    e = (np.sin(xs * 0.55 + t * TAU) + np.sin(xs * 1.3 - t * TAU * 2 + 0.7) * 0.6 + 1.6) / 3.2
    rng = np.random.default_rng(seed)
    e = e + rng.random(f.w) * 0.15
    return f.Y < f.h - depth + e[None, :] * depth


# ============================================================ emblems (normalized polygons, drawn AA)

def poly(points, cx, cy, s, rot=0.0):
    cr, sr = math.cos(rot), math.sin(rot)
    return lambda d, ss: d.polygon([((cx + (x * cr - y * sr) * s) * ss, (cy + (x * sr + y * cr) * s) * ss) for x, y in points], fill=255)


def circle(cx, cy, r, ring=0.0):
    def fn(d, ss):
        d.ellipse([(cx - r) * ss, (cy - r) * ss, (cx + r) * ss, (cy + r) * ss], fill=255)
        if ring:
            d.ellipse([(cx - r + ring) * ss, (cy - r + ring) * ss, (cx + r - ring) * ss, (cy + r - ring) * ss], fill=0)
    return fn


def crescent(cx, cy, r, off=0.45):
    def fn(d, ss):
        d.ellipse([(cx - r) * ss, (cy - r) * ss, (cx + r) * ss, (cy + r) * ss], fill=255)
        o = r * off
        d.ellipse([(cx - r + o) * ss, (cy - r - o * 0.3) * ss, (cx + r + o) * ss, (cy + r - o * 0.3) * ss], fill=0)
    return fn


def star_pts(n=5, inner=0.45):
    return [((1 if i % 2 == 0 else inner) * math.sin(math.pi * i / n), -(1 if i % 2 == 0 else inner) * math.cos(math.pi * i / n)) for i in range(2 * n)]


SWORD = [(-0.08, -1), (0.08, -1), (0.12, -0.85), (0.12, 0.3), (0.5, 0.3), (0.5, 0.42), (0.1, 0.42), (0.1, 0.75),
         (0.18, 0.78), (0.18, 0.92), (-0.18, 0.92), (-0.18, 0.78), (-0.1, 0.75), (-0.1, 0.42), (-0.5, 0.42), (-0.5, 0.3), (-0.12, 0.3), (-0.12, -0.85)]
CROWN = [(-1, 0.6), (-1, -0.5), (-0.6, 0.0), (-0.3, -0.8), (0, -0.1), (0.3, -0.8), (0.6, 0.0), (1, -0.5), (1, 0.6)]
FLAME = [(0, -1), (0.25, -0.55), (0.55, -0.35), (0.6, 0.2), (0.45, 0.7), (0, 1), (-0.45, 0.7), (-0.6, 0.2), (-0.4, -0.2), (-0.25, 0.05), (-0.15, -0.45)]
BOLT = [(0.2, -1), (-0.5, 0.1), (-0.05, 0.1), (-0.3, 1), (0.55, -0.2), (0.08, -0.2), (0.35, -1)]
GEM = [(0, -1), (0.75, -0.35), (0.5, 0.35), (0, 1), (-0.5, 0.35), (-0.75, -0.35)]
LEAF = [(0, -1), (0.45, -0.5), (0.5, 0.1), (0.2, 0.7), (0, 1), (-0.2, 0.7), (-0.5, 0.1), (-0.45, -0.5)]
HEART = [(0, 1), (-0.9, 0.0), (-1, -0.45), (-0.75, -0.85), (-0.35, -0.85), (0, -0.45), (0.35, -0.85), (0.75, -0.85), (1, -0.45), (0.9, 0.0)]
TRIDENT = [(-0.06, 1), (0.06, 1), (0.06, -0.3), (0.45, -0.45), (0.45, -1), (0.33, -0.7), (0.33, -0.55), (0.06, -0.45), (0.06, -1.05),
           (-0.06, -1.05), (-0.06, -0.45), (-0.33, -0.55), (-0.33, -0.7), (-0.45, -1), (-0.45, -0.45), (-0.06, -0.3)]
CHEVRON = [(-1, 0.2), (0, -0.6), (1, 0.2), (1, 0.6), (0, -0.2), (-1, 0.6)]
LAMBDA = [(-0.1, -1), (0.12, -1), (0.8, 1), (0.52, 1), (0, -0.35), (-0.52, 1), (-0.8, 1)]


def emblem(f, shape, col, glow=0.0, shadow=0.5, highlight=True):
    cov = f.layer(shape)
    f.paint(cov, col, glow=glow, shadow=shadow)
    if highlight:
        top = cov * np.clip(1 - np.roll(cov, 1, 0) * 1.0, 0, 1)
        f.tint(np.ones(3), np.clip(top, 0, 1) * 0.45)
    return cov


def creeper_face(f, x0, y0, k, col):
    grid = ["11..11", "11..11", "..11..", ".1111.", ".1..1."]
    for j, row in enumerate(grid):
        for i, ch in enumerate(row):
            if ch == "1":
                f.c[y0 + j * k:y0 + (j + 1) * k, x0 + i * k:x0 + (i + 1) * k] = col


# ============================================================ effects

def stars(f, t, seed, n=30, col=None, glow=1.0, region=None):
    rng = np.random.default_rng(seed)
    col = np.array([1.0, 0.97, 0.88]) if col is None else col
    for i in range(n):
        x, y = rng.uniform(0, f.w), rng.uniform(0, f.h if region is None else region)
        ph = rng.uniform(0, 1)
        b = 0.5 + 0.5 * math.cos(TAU * (t + ph))
        if b > 0.25:
            f.dot(x, y, col * (0.4 + 0.6 * b), r=b, glow=glow * b ** 3)


def particles(f, t, seed, n, col, speed=1.0, drift=0.0, up=True, glow=1.0, size=1):
    rng = np.random.default_rng(seed)
    for i in range(n):
        x0, y0, ph = rng.uniform(0, f.w), rng.uniform(0, f.h), rng.uniform(0, 1)
        k = (t * speed + ph) % 1
        y = (y0 - k * f.h) % f.h if up else (y0 + k * f.h) % f.h
        x = (x0 + math.sin(TAU * (k + ph)) * drift) % f.w
        fade = math.sin(math.pi * k)
        for s in range(size):
            f.dot(x + s, y, col * (0.5 + 0.5 * fade), r=fade, glow=glow * fade)


def shimmer(f, t, width=6, strength=0.5, angle=1.0):
    """Diagonal light sweep."""
    pos = (t * 1.6 - 0.3) * (f.w + f.h * angle)
    d = np.abs(f.X + f.Y * angle - pos)
    f.tint(np.ones(3), np.clip(1 - d / width, 0, 1) * strength)


def flame_field(f, t, seed, height=0.55, cols=("#fff0a8", "#ffa21c", "#d8360a", "#4a0804")):
    n = vnoise(f.h, f.w, 4, seed, oy=t, octaves=3)
    yy = f.Y / (f.h - 1)
    v = np.clip((yy - (1 - height)) / height + (n - 0.5) * 0.9, 0, 1)
    stops = [C(c) for c in cols][::-1]  # dark -> bright
    seg = np.clip(v * 0.999 * (len(stops) - 1), 0, len(stops) - 1.001)
    i = np.floor(seg).astype(int)
    fr = (seg - i)[..., None]
    st = np.array(stops)
    col = st[i] * (1 - fr) + st[i + 1] * fr
    col = col * (0.82 + 0.18 * (1 - yy))[..., None]  # hottest part stays orange, not white
    smooth = np.clip(v * 1.3, 0, 1) * 0.92
    return col, smooth


# ============================================================ writers

def put(img, arr, x, y, mirror=False):
    if mirror:
        arr = arr[:, ::-1]
    img.paste(Image.fromarray(arr), (x, y), Image.fromarray(arr[..., 3]))


def sides(img, arr, fx, fy, side, cap, dark=0.65):
    """Edge faces of the box next to the front face."""
    h, w = arr.shape[:2]
    left = np.repeat(arr[:, :1], side, 1).copy()
    right = np.repeat(arr[:, -1:], side, 1).copy()
    top = np.repeat(arr[:1], cap, 0).copy()
    # bottom face = the face's own last row: transparent where the shape is cut,
    # so no stray strip floats under V-cuts, rags or round shields
    bottom = np.repeat(arr[-1:], cap, 0).copy()
    for part, xx, yy in ((left, fx - side, fy), (right, fx + w, fy), (top, fx, fy - cap), (bottom, fx + w, fy - cap)):
        part[..., :3] = (part[..., :3] * dark).astype(np.uint8)
        put(img, part, xx, yy)


def back_of(arr, col):
    b = arr.copy()
    b[..., :3] = (np.array(col) * 255).astype(np.uint8)
    noise = np.random.default_rng(3).random(arr.shape[:2]) * 0.08 + 0.92
    b[..., :3] = (b[..., :3] * noise[..., None]).astype(np.uint8)
    return b


def cape_frame(face, lining):
    img = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    arr = face.rgba()
    put(img, arr, 4, 4)
    put(img, back_of(arr, lining), 48, 4, mirror=True)
    sides(img, arr, 4, 4, 4, 4)
    return img


def wing_silhouette(face):
    """Real wing outline: rounded shoulder, tapering tip – applied to every wing design."""
    w, h = face.w, face.h
    X, Y = face.X, face.Y
    shoulder = ~((X < 3) & (Y < 3 - X)) & ~((X > w - 4) & (Y < X - (w - 4)))
    tip_r = 18
    tip = (Y < h - tip_r) | (((X - (w - 1) / 2) / (w / 2)) ** 2 + ((Y - (h - tip_r)) / tip_r) ** 2 < 1)
    face.shape(shoulder & tip)


def wing_frame(face):
    img = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    wing_silhouette(face)
    face.outline(None, 0.35)
    arr = face.rgba()
    put(img, arr, 96, 8)
    back = arr.copy()
    back[..., :3] = (back[..., :3] * 0.88).astype(np.uint8)
    put(img, back, 144, 8, mirror=True)
    # no side/top/bottom faces: the wing reads as a thin, flat sheet instead of a thick slab
    put(img, arr[8:72], 4, 4)
    return img


def shield_frame(face, wood):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    arr = face.rgba()
    put(img, arr, 4, 4)
    put(img, back_of(arr, wood), 56, 4, mirror=True)
    sides(img, arr, 4, 4, 4, 4)
    handle = np.zeros((48, 64, 4), np.uint8)
    handle[..., :3] = (np.array(wood) * 0.7 * 255).astype(np.uint8)
    handle[..., 3] = 255
    put(img, handle, 104, 0)
    return img


def save(frames, name):
    w, h = frames[0].size
    out = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        out.paste(fr, (0, i * h))
    out.save(OUT / name, optimize=True)


def make(kind, name, fn, animated, extra):
    frames = []
    for i in range(NF if animated else 1):
        t = i / NF
        if kind == "cape":
            f = Face(40, 64)
            fn(f, t)
            frames.append(cape_frame(f, extra))
        elif kind == "wings":
            f = Face(40, 80)
            fn(f, t)
            frames.append(wing_frame(f))
        else:
            f = Face(48, 88)
            fn(f, t)
            frames.append(shield_frame(f, extra))
    save(frames, f"{kind}_{name}.png")


DESIGNS = []


def design(kind, name, animated=False, extra=(0.2, 0.2, 0.2)):
    def reg(fn):
        DESIGNS.append((kind, name, fn, animated, extra))
        return fn
    return reg


GOLD, GOLD_D = C("#f6c945"), C("#a8701a")
SILVER = C("#d8dde6")
WHITE = np.ones(3)

# ============================================================ CAPES


@design("cape", "crimson", extra=C("#4a0b10"))
def _(f, t):
    f.grad(C("#d22a36"), C("#6e0b16")).cloth(1).folds(3, 0.1).light()
    f.shape(m_vcut(f, 12))
    f.trim(GOLD, 3)
    emblem(f, poly(SWORD, 20, 27, 17), SILVER)
    emblem(f, poly(CROWN, 20, 8, 5), GOLD)
    f.outline(C("#2a0004"))


@design("cape", "midnight", True, C("#0c1030"))
def _(f, t):
    f.grad(C("#2b3f9a"), C("#06081c"), 0.8).cloth(2, 0.04).folds(3, 0.08)
    neb = vnoise(f.h, f.w, 3, 5, ox=t * 0.0, oy=0)
    f.tint(C("#6a3fb0"), np.clip(neb - 0.45, 0, 1) * 1.2)
    f.shape(m_swallow(f, 16))
    stars(f, t, 3, 34)
    emblem(f, crescent(28, 12, 6), C("#fff0b8"), glow=0.6)
    f.trim(C("#9fb0ff"), 1, pattern=False, glow=0.3)
    f.outline(C("#02030a"))


@design("cape", "aurora", True, C("#0a1430"))
def _(f, t):
    f.grad(C("#08122e"), C("#140a2a")).folds(3, 0.06)
    for k, (col, sp) in enumerate(((C("#3dffb0"), 1), (C("#8a5cff"), -1), (C("#2ad0ff"), 1))):
        y0 = 12 + k * 14
        wave = y0 + 6 * np.sin(f.X * 0.25 + TAU * t * sp + k) + 3 * np.sin(f.X * 0.6 - TAU * t * 2 * sp)
        d = f.Y - wave
        band = np.clip(1 - np.abs(d) / 7, 0, 1) ** 1.5 * (f.Y > wave - 2 - (1 - np.abs(d) / 7) * 0)
        curtain = np.clip(1 - np.clip(d, 0, 20) / 20, 0, 1) * (d > 0)
        f.tint(col, np.clip(band * 0.85 + curtain * 0.25, 0, 1))
        f.e += (band[..., None] * col) * 0.35
    stars(f, t, 4, 14, glow=0.6, region=20)
    f.shape(m_scallop(f, 4, 5))
    f.outline(C("#000814"))


@design("cape", "inferno", True, C("#2a0806"))
def _(f, t):
    f.grad(C("#3a0c08"), C("#140302")).cloth(3)
    col, v = flame_field(f, t, 7, 0.75)
    f.tint(col, v)
    f.e += col * (v[..., None] ** 3) * 0.45
    particles(f, t, 8, 10, C("#ffb040"), speed=1, drift=2, glow=1.2)
    emblem(f, poly(FLAME, 20, 22, 9), C("#ffe9a0"), glow=0.9, shadow=0.3)
    f.shape(m_flames(f, t, 12))
    f.outline(C("#300400"), 0.4)


@design("cape", "ocean", True, C("#06263f"))
def _(f, t):
    f.grad(C("#2fb6e8"), C("#062a5a"), 0.9).cloth(4, 0.04)
    for k in range(6):
        y = 6 + k * 10 + 2.5 * np.sin(f.X * 0.32 + TAU * t + k * 1.7)
        crest = np.clip(1 - np.abs(f.Y - y) / 1.3, 0, 1)
        f.tint(C("#c8f4ff"), crest * 0.8)
        f.tint(C("#0a3a70"), np.clip(1 - np.abs(f.Y - y - 3) / 2, 0, 1) * 0.35)
    particles(f, t, 9, 9, C("#e8fbff"), up=True, drift=1.5, glow=0.4)
    emblem(f, poly(TRIDENT, 20, 32, 14), GOLD, glow=0.25)
    f.shape(m_points(f, 5, 6) | (f.Y < f.h - 7 + 2 * np.sin(f.X * 0.6 + TAU * t)))
    f.shape(f.Y < f.h - 2 + 2 * np.sin(f.X * 0.6 + TAU * t))
    f.outline(C("#021a33"))


@design("cape", "ender", True, C("#120818"))
def _(f, t):
    f.grad(C("#2a1240"), C("#07030c")).cloth(5)
    neb = vnoise(f.h, f.w, 3, 11, oy=t)
    f.tint(C("#7a2cc0"), np.clip(neb - 0.5, 0, 1) * 1.6)
    particles(f, t, 12, 16, C("#d080ff"), speed=1, drift=2, glow=1.0)
    pulse = 0.6 + 0.4 * math.sin(TAU * t)
    emblem(f, lambda d, s: d.ellipse([10 * s, 18 * s, 30 * s, 30 * s], fill=255), C("#1d8f74"), shadow=0.6)
    emblem(f, lambda d, s: d.ellipse([13 * s, 19.5 * s, 27 * s, 28.5 * s], fill=255), C("#4cffc8") * pulse, glow=pulse, highlight=False)
    emblem(f, lambda d, s: d.ellipse([18 * s, 20.5 * s, 22 * s, 27.5 * s], fill=255), C("#062a22"), shadow=0, highlight=False)
    f.shape(m_ragged(f, 13, 10))
    f.outline()


@design("cape", "cherry", True, C("#7a2c50"))
def _(f, t):
    f.grad(C("#ffd3e6"), C("#e06aa6")).cloth(6, 0.04).folds(3, 0.07)
    branch = lambda d, s: (d.line([(0, 6 * s), (14 * s, 12 * s), (26 * s, 9 * s), (40 * s, 16 * s)], fill=255, width=int(2.2 * s)),
                           d.line([(14 * s, 12 * s), (18 * s, 20 * s)], fill=255, width=int(1.5 * s)))
    emblem(f, branch, C("#5a2a26"), shadow=0.3)
    for (x, y) in ((6, 5), (13, 10), (22, 8), (31, 12), (18, 19), (37, 16)):
        emblem(f, circle(x, y, 2.2), C("#fff2f8"), shadow=0.2, highlight=False)
        f.dot(x, y, C("#ff5a9a"))
    rng = np.random.default_rng(14)
    for i in range(14):
        x0, y0, ph = rng.uniform(0, 40), rng.uniform(0, 64), rng.uniform(0, 1)
        k = (t + ph) % 1
        x, y = (x0 + k * 10) % 40, (y0 + k * 64) % 64
        for dx in (0, 1):
            f.dot(x + dx, y + (dx * 0.5), C("#ffffff") if i % 3 else C("#ff8cbc"))
    f.shape(m_scallop(f, 6, 5))
    f.outline(C("#7a2050"), 0.5)


@design("cape", "diamond", True, C("#0f4a58"))
def _(f, t):
    f.grad(C("#8ff4f4"), C("#1a7f96")).cloth(7, 0.03)
    tri = ((f.X // 5 + f.Y // 5) % 2 == 0)
    f.mul(np.where(tri, 1.06, 0.94))
    emblem(f, poly(GEM, 20, 28, 12), C("#c8ffff"), glow=0.3)
    gem_shade = f.layer(poly([(0, -1), (0.75, -0.35), (0, -0.2)], 20, 28, 12))
    f.tint(WHITE, gem_shade * 0.6)
    shimmer(f, t, 5, 0.55)
    stars(f, t, 15, 12, glow=1.2)
    f.shape(m_points(f, 1, 16))
    f.trim(C("#e6ffff"), 2, glow=0.2)
    f.outline(C("#0a4050"))


@design("cape", "prism", True, C("#303030"))
def _(f, t):
    hue = (f.Y / 64 + f.X / 160 + t) % 1
    import colorsys
    rgb = np.vectorize(lambda h: colorsys.hsv_to_rgb(h, 0.6, 1.0), otypes=[float, float, float])(hue)
    f.c = np.stack(rgb, -1)
    f.cloth(16, 0.04).folds(3, 0.1)
    shimmer(f, t, 4, 0.35)
    f.shape(m_points(f, 4, 8))
    f.trim(WHITE, 1, pattern=False, glow=0.3)
    f.outline()


@design("cape", "rift", True, C("#08040e"))
def _(f, t):
    f.grad(C("#16091f"), C("#040208")).cloth(17, 0.1)
    rng = np.random.default_rng(18)
    pts, x = [], 20.0
    for y in range(0, 66, 4):
        pts.append((x, y))
        x = float(np.clip(x + rng.uniform(-4, 4), 8, 32))
    pulse = 0.55 + 0.45 * math.sin(TAU * t)
    glow_c = C("#c060ff") * pulse + C("#ffffff") * (1 - pulse) * 0.2
    emblem(f, lambda d, s: d.line([(px * s, py * s) for px, py in pts], fill=255, width=int(3.2 * s)), C("#6a1fb0"), glow=pulse * 0.6, highlight=False)
    emblem(f, lambda d, s: d.line([(px * s, py * s) for px, py in pts], fill=255, width=int(1.2 * s)), glow_c, glow=1.2, highlight=False)
    particles(f, t, 19, 10, C("#e0a0ff"), drift=3, glow=0.8)
    f.shape(m_ragged(f, 20, 6))
    f.outline()


@design("cape", "royal", extra=C("#2a0a3a"))
def _(f, t):
    f.grad(C("#8a2cc0"), C("#3c0c5c")).cloth(21).folds(4, 0.12).light()
    damask = ((np.sin(f.X * 0.9) * np.sin(f.Y * 0.9)) > 0.6)
    f.mul(np.where(damask, 1.12, 1.0))
    ermine = f.Y > 52
    f.c = np.where(ermine[..., None], C("#f4f2ea") * (0.95 + 0.05 * ((f.X + f.Y) % 2)[..., None]), f.c)
    for x in range(4, 40, 8):
        y = 55 + (x // 8 % 2) * 4
        f.c[y:y + 3, x:x + 1] = C("#141414")
        f.c[y:y + 1, x - 1:x + 2] = C("#141414")
    emblem(f, poly(CROWN, 20, 22, 9), GOLD)
    for x in (13, 20, 27):
        f.dot(x, 18, C("#e02040"))
    f.shape(m_round(f, 18))
    f.trim(GOLD, 2)
    f.outline(C("#1a0024"))


@design("cape", "forest", True, C("#1e3a18"))
def _(f, t):
    f.grad(C("#6fbf5a"), C("#1c4a20")).cloth(22).folds(3, 0.08)
    for k, (h, col) in enumerate(((22, C("#2c6a2c")), (14, C("#1c4a1e")), (8, C("#123414")))):
        ridge = f.h - h - 6 * np.abs(np.sin(f.X * 0.25 + k))
        f.c = np.where((f.Y > ridge)[..., None], col, f.c)
    for x in range(2, 40, 6):
        top = f.h - 30 + (x * 7) % 9
        for j in range(12):
            w = j // 3
            f.c[top + j, max(0, x - w):x + w + 1] = C("#0f3a18") * (0.85 + 0.15 * (j % 3 == 0))
    particles(f, t, 23, 8, C("#ffe880"), speed=0.5, drift=4, glow=1.5)
    f.shape(m_points(f, 7, 5))
    f.outline(C("#0a200a"))


@design("cape", "wanderer", extra=C("#2a2016"))
def _(f, t):
    f.grad(C("#8a6a4a"), C("#4a3624")).cloth(24, 0.14).folds(2, 0.14).light()
    patch = (np.abs(f.X - 28) < 5) & (np.abs(f.Y - 20) < 5)
    f.c = np.where(patch[..., None], C("#5a6a3a"), f.c)
    st = patch & ((np.abs(f.X - 28) == 5) | (np.abs(f.Y - 20) == 5)) & ((f.X + f.Y) % 2 == 0)
    f.c = np.where(st[..., None], C("#d8c8a0"), f.c)
    f.shape(m_ragged(f, 25, 12, 4))
    f.outline(C("#1a1008"))


@design("cape", "storm", True, C("#151820"))
def _(f, t):
    f.grad(C("#3c4252"), C("#12141a")).cloth(26)
    cloud = vnoise(f.h, f.w, 3, 27, ox=t, octaves=4)
    f.tint(C("#6a7288"), np.clip(cloud - 0.4, 0, 1) * 1.4)
    flash = 1.0 if (t * NF) % 6 in (0, 1) else 0.0
    if flash:
        f.tint(C("#c8d4ff"), 0.15)
    emblem(f, poly(BOLT, 20, 34, 14), C("#fff6a0") if flash else C("#d8c860"), glow=1.4 if flash else 0.4)
    particles(f, t * 2, 28, 18, C("#a8c0e8"), up=False, speed=2, glow=0)
    f.shape(m_ragged(f, 29, 6))
    f.outline()


@design("cape", "galaxy", True, C("#05040e"))
def _(f, t):
    cx, cy = 20, 30
    dx, dy = f.X - cx, f.Y - cy
    r = np.sqrt(dx * dx + dy * dy) + 1e-6
    ang = np.arctan2(dy, dx)
    arm = (np.sin(2 * ang + r * 0.35 - TAU * t) + 1) / 2
    core = np.clip(1 - r / 9, 0, 1)
    f.c = np.zeros_like(f.c) + C("#05040e")
    f.tint(C("#5a3cd0"), np.clip(arm * np.clip(1 - r / 30, 0, 1), 0, 1) * 0.9)
    f.tint(C("#ff7ad0"), np.clip(arm - 0.6, 0, 1) * np.clip(1 - r / 22, 0, 1) * 1.5)
    f.tint(C("#fff2d0"), core ** 1.5)
    f.e += (core ** 2)[..., None] * C("#ffe0f0") * 0.8
    stars(f, t, 30, 28, glow=0.8)
    f.shape(m_swallow(f, 12))
    f.trim(C("#b8a0ff"), 1, pattern=False, glow=0.4)
    f.outline()


@design("cape", "magma", True, C("#1a0806"))
def _(f, t):
    rock = vnoise(f.h, f.w, 5, 31, octaves=3)
    f.c = (C("#2a1610") * (0.7 + 0.6 * rock)[..., None])
    cells = vnoise(f.h, f.w, 4, 32, octaves=1)
    crack = np.clip(1 - np.abs(cells - 0.5) * 14, 0, 1)
    pulse = 0.6 + 0.4 * math.sin(TAU * t + 0.0)
    lava = C("#ff7a1a") * pulse + C("#ffd040") * (1 - pulse)
    f.tint(lava, crack)
    f.e += crack[..., None] * lava * pulse
    f.shape(m_ragged(f, 33, 5))
    f.outline()


@design("cape", "frost", True, C("#9cc6e8"))
def _(f, t):
    f.grad(C("#eef9ff"), C("#7fb4e6")).cloth(34, 0.04).folds(3, 0.07)
    for k in range(5):
        x = 4 + k * 8
        emblem(f, poly([(0, -1), (0.35, 0), (0, 1), (-0.35, 0)], x, 12 + (k % 2) * 6, 5), WHITE, shadow=0.15, highlight=False)
    sf = lambda d, s: [d.line([((20 + 11 * math.cos(a)) * s, (34 + 11 * math.sin(a)) * s),
                               ((20 - 11 * math.cos(a)) * s, (34 - 11 * math.sin(a)) * s)], fill=255, width=int(1.6 * s)) for a in (0, math.pi / 3, 2 * math.pi / 3)]
    emblem(f, sf, WHITE, glow=0.4)
    particles(f, t, 35, 14, WHITE, up=False, drift=2, glow=0.3)
    icicles = f.Y < f.h - 3 - 9 * (((f.X * 7) % 5) / 5) * ((f.X % 3) == 1)
    f.shape(icicles)
    f.trim(C("#ffffff"), 1, pattern=False, glow=0.2)
    f.outline(C("#4a7aa8"))


@design("cape", "creeper", extra=C("#1a3a12"))
def _(f, t):
    rng = np.random.default_rng(36)
    pix = rng.choice([0.0, 0.5, 1.0], size=(16, 10))
    pix = np.kron(pix, np.ones((4, 4)))
    f.c = C("#3a8a2a") * (1 - pix[..., None]) * 1.0 + C("#6ccf4a") * pix[..., None] * 0.6 + C("#2a6a1e") * 0.2
    f.light()
    creeper_face(f, 8, 14, 4, C("#0c0c0c"))
    f.shape(m_straight(f))
    f.outline(C("#0a2006"))


@design("cape", "sun", True, C("#6a3a00"))
def _(f, t):
    f.grad(C("#ffd860"), C("#e0701a")).cloth(37, 0.04).folds(3, 0.08)
    cx, cy = 20, 24
    ang = np.arctan2(f.Y - cy, f.X - cx)
    rays = (np.sin(ang * 12 + TAU * t) > 0.3) & (np.hypot(f.X - cx, f.Y - cy) > 8)
    f.tint(C("#fff6c0"), rays * 0.35 * np.clip(1 - np.hypot(f.X - cx, f.Y - cy) / 40, 0, 1))
    emblem(f, circle(cx, cy, 7.5), C("#fff2a0"), glow=0.9)
    emblem(f, circle(cx, cy, 7.5, ring=1.2), C("#ffb020"), shadow=0, highlight=False)
    f.shape(m_points(f, 5, 9))
    f.trim(GOLD, 2)
    f.outline(C("#6a2a00"))


@design("cape", "toxic", True, C("#102a06"))
def _(f, t):
    f.grad(C("#2a3a1a"), C("#0a1206")).cloth(38)
    goo = vnoise(f.h, f.w, 3, 39, oy=-t)
    f.tint(C("#7aff2a"), np.clip(goo - 0.55, 0, 1) * 2.2)
    f.e += np.clip(goo - 0.6, 0, 1)[..., None] * C("#7aff2a") * 1.5
    particles(f, t, 40, 12, C("#c8ff7a"), up=True, drift=1, glow=1.0)
    drip = f.Y < f.h - 6 + 6 * ((np.sin(f.X * 1.1) > 0.6) * np.clip(np.sin(TAU * t + f.X * 0.3), 0, 1) * 1.0)
    f.shape(m_scallop(f, 5, 4) | drip & (f.Y < f.h - 1))
    f.outline()


@design("cape", "redstone", True, C("#1a0a0a"))
def _(f, t):
    f.grad(C("#3a3a3e"), C("#1c1c20")).cloth(41, 0.1)
    grid = ((f.X % 10 == 5) & (f.Y % 4 != 0)) | ((f.Y % 16 == 8) & (f.X % 2 == 0))
    dist = (f.Y + (f.X // 10) * 7) % 32
    pulse = np.clip(1 - np.abs(dist - t * 32) / 6, 0, 1)
    f.c = np.where(grid[..., None], C("#5a0a0a") + C("#ff2a1a")[None, None] * pulse[..., None], f.c)
    f.e += (grid * pulse)[..., None] * C("#ff3010")
    for (x, y) in ((5, 8), (15, 24), (25, 40), (35, 56)):
        emblem(f, circle(x, y, 2.2), C("#ff4020"), glow=0.8, highlight=False)
    f.shape(m_straight(f) & (f.Y < f.h - 2 + (f.X % 10 < 5) * 2))
    f.trim(C("#6a6a70"), 2, pattern=False)
    f.outline()


@design("cape", "phantom", True, C("#1a2030"))
def _(f, t):
    f.grad(C("#5a6a90"), C("#1a2234")).cloth(42).folds(2, 0.1)
    for k in range(3):
        x0 = 6 + k * 14
        vein = np.abs(f.X - (x0 + np.sin(f.Y * 0.2 + k) * 2)) < 0.8
        f.c = np.where(vein[..., None], C("#2a3a5a"), f.c)
    pulse = 0.5 + 0.5 * math.sin(TAU * t)
    for x in (14, 26):
        emblem(f, poly([(-1, 0), (0, -0.4), (1, 0), (0, 0.4)], x, 16, 4), C("#7affd8") * (0.6 + 0.4 * pulse), glow=pulse * 1.2, highlight=False)
    f.shape(m_ragged(f, 43, 14))
    f.outline()


@design("cape", "honey", extra=C("#5a3a06"))
def _(f, t):
    f.grad(C("#ffd04a"), C("#d88a10")).cloth(44, 0.03)
    cs = []
    for i in range(-1, 10):
        for j in range(-1, 14):
            cs.append((i * 5.2, j * 6.0 + (i % 2) * 3.0))
    cs = np.array(cs)
    d = np.sqrt((f.X[..., None] - cs[:, 0]) ** 2 + (f.Y[..., None] - cs[:, 1]) ** 2)
    d.sort(axis=-1)
    wall = (d[..., 1] - d[..., 0]) < 0.9
    f.mul(np.where(wall, 0.62, 1.0))
    f.tint(C("#fff0a0"), np.clip(1 - d[..., 0] / 2.6, 0, 1) * 0.45)
    drip = np.zeros((f.h, f.w), bool)
    for x, l in ((6, 9), (15, 5), (22, 12), (31, 7), (37, 4)):
        drip |= (np.abs(f.X - x) <= 1.5) & (f.Y >= f.h - 10) & (f.Y < f.h - 10 + l)
    f.shape((f.Y < f.h - 10) | drip)
    f.outline(C("#6a3a00"))


@design("cape", "dragon", True, C("#200a20"))
def _(f, t):
    q = f.X / 5.0
    r = (f.Y + (np.floor(q) % 2) * 2.5) / 5.0
    sx, sy = (q % 1) - 0.5, (r % 1)
    scale = np.clip(1 - (sx * sx * 3 + (sy - 0.2) ** 2 * 1.4), 0, 1)
    hue = (f.Y / 64 * 0.3 + t) % 1
    base = C("#1a5a4a") * (1 - np.sin(TAU * hue)[..., None] * 0.5 + 0.5) * 0.5 + C("#5a1a7a") * (np.sin(TAU * hue)[..., None] * 0.5 + 0.5)
    f.c = base * (0.45 + 0.75 * scale[..., None])
    f.tint(WHITE, np.clip(scale - 0.85, 0, 1) * 2.5)
    shimmer(f, t, 6, 0.25)
    f.shape(m_points(f, 4, 10))
    f.trim(C("#c8a050"), 2)
    f.outline()


# ============================================================ WINGS


def feathers(f, light, dark, tips=None, rows=7, t=0.0, wobble=0.0):
    rh = f.h / rows
    row = (f.Y // rh)
    yy = (f.Y % rh) / rh
    xoff = (row % 2) * 3
    fw = 6
    fx = ((f.X + xoff) % fw) / fw
    edge_y = 0.62 + 0.38 * (1 - (2 * fx - 1) ** 2)  # rounded feather tip
    d = yy - edge_y
    f.c = light * (1 - (f.Y / f.h)[..., None]) + dark * (f.Y / f.h)[..., None]
    shade = np.where(d > 0, 0.72, 0.8 + 0.3 * (1 - yy))
    shade = np.where(np.abs(d) < 0.09, 0.55, shade)
    f.mul(shade)
    quill = (np.abs(fx - 0.5) < 0.09) & (d < 0)
    f.mul(np.where(quill, 1.12, 1.0))
    if tips is not None:
        f.tint(tips, np.clip((f.Y / f.h - 0.6) * 2.5, 0, 1) * 0.8)


def membrane(f, base, dark, bone, bones=4, veins=True):
    f.c = base * (1 - (f.Y / f.h)[..., None] * 0.5) + dark * (f.Y / f.h)[..., None] * 0.5
    for k in range(bones):
        x_end = (k + 0.5) * f.w / bones
        x = 2 + (x_end - 2) * (f.Y / f.h) ** 0.8
        w = 1.4 - 0.8 * f.Y / f.h
        b = np.abs(f.X - x) < w
        f.c = np.where(b[..., None], bone * (0.9 + 0.2 * ((f.Y // 3) % 2))[..., None], f.c)
    if veins:
        v = vnoise(f.h, f.w, 4, 50, octaves=2)
        f.mul(np.where(np.abs(v - 0.5) < 0.02, 0.75, 1.0))


def scallop_bottom(f, n, depth):
    return m_scallop(f, n, depth)


@design("wings", "angel")
def _(f, t):
    feathers(f, C("#ffffff"), C("#d8dcea"), tips=C("#f2e6b8"))
    f.shape(m_scallop(f, 6, 8))
    f.outline(C("#9098b0"), 0.5)


@design("wings", "archangel", True)
def _(f, t):
    feathers(f, C("#fffbe8"), C("#f0d880"), tips=GOLD)
    shimmer(f, t, 6, 0.45)
    particles(f, t, 51, 8, C("#fff4b0"), up=True, drift=2, glow=1.2)
    f.trim(GOLD, 1, pattern=False, glow=0.6)
    f.shape(m_points(f, 5, 12))
    f.outline(C("#a07a20"), 0.5)


@design("wings", "phoenix", True)
def _(f, t):
    feathers(f, C("#ffe070"), C("#e0300c"))
    col, v = flame_field(f, t, 52, 0.45)
    f.tint(col, v * 0.8)
    f.e += col * (v[..., None] ** 3) * 0.5
    particles(f, t, 53, 10, C("#ffc040"), glow=1.2)
    f.shape(m_flames(f, t, 16, 54))
    f.outline(C("#5a0a00"), 0.4)


@design("wings", "dragon")
def _(f, t):
    membrane(f, C("#7a2aa8"), C("#2a0a40"), C("#24082e"))
    q = f.X / 4.0
    r = (f.Y + (np.floor(q) % 2) * 2) / 4.0
    f.mul(np.where(((r % 1) > 0.8) & (f.Y < 20), 0.7, 1.0))
    f.shape(m_scallop(f, 4, 12))
    f.outline(C("#100018"))


@design("wings", "enderdragon", True)
def _(f, t):
    membrane(f, C("#2a2430"), C("#08060c"), C("#141018"))
    pulse = 0.5 + 0.5 * math.sin(TAU * t)
    v = vnoise(f.h, f.w, 3, 55, oy=t)
    glow = np.clip(1 - np.abs(v - 0.5) * 22, 0, 1)
    f.tint(C("#d070ff"), glow * (0.5 + 0.5 * pulse))
    f.e += glow[..., None] * C("#c050ff") * pulse
    particles(f, t, 56, 10, C("#e0a0ff"), glow=1.0)
    f.shape(m_scallop(f, 4, 14))
    f.outline()


@design("wings", "bat")
def _(f, t):
    membrane(f, C("#4a4250"), C("#1a161e"), C("#120e14"), bones=5)
    f.shape(m_scallop(f, 5, 10))
    f.outline()


@design("wings", "demon", True)
def _(f, t):
    membrane(f, C("#5a0a0a"), C("#1a0202"), C("#0a0202"), bones=4)
    v = vnoise(f.h, f.w, 4, 57, octaves=2)
    lava = np.clip(1 - np.abs(v - 0.5) * 18, 0, 1)
    pulse = 0.55 + 0.45 * math.sin(TAU * t)
    f.tint(C("#ff6a10"), lava * pulse)
    f.e += lava[..., None] * C("#ff5010") * pulse
    f.shape(m_points(f, 4, 14))
    f.outline()


@design("wings", "monarch")
def _(f, t):
    f.c = np.zeros_like(f.c) + C("#ff9a20")
    f.tint(C("#ffd060"), np.clip(1 - f.Y / 40, 0, 1) * 0.5)
    for k in range(5):
        x = 2 + k * 9
        f.c = np.where((np.abs(f.X - x - f.Y * 0.15) < 0.9)[..., None], C("#1a1210"), f.c)
    f.c = np.where((np.abs(f.Y - 42) < 1.0)[..., None], C("#1a1210"), f.c)
    upper = ((f.X - 20) / 21) ** 2 + ((f.Y - 21) / 21.5) ** 2 < 1
    lower = ((f.X - 18) / 16) ** 2 + ((f.Y - 62) / 17) ** 2 < 1
    f.shape(upper | lower)
    b = f.band(3)
    f.c = np.where(b[..., None], C("#1a1210"), f.c)
    dots = b & ((f.X + f.Y) % 5 == 0) & ~f.band(1)
    f.c = np.where(dots[..., None], WHITE, f.c)


@design("wings", "fairy", True)
def _(f, t):
    import colorsys
    hue = (0.75 + 0.15 * np.sin(f.X * 0.1 + f.Y * 0.05 + TAU * t))
    rgb = np.vectorize(lambda h: colorsys.hsv_to_rgb(h % 1, 0.3, 1.0), otypes=[float, float, float])(hue)
    f.c = np.stack(rgb, -1)
    for k in range(4):
        vein = np.abs(np.arctan2(f.Y - 0, f.X - 20) - (0.6 + k * 0.6)) < 0.03
        f.c = np.where(vein[..., None], C("#b080d0"), f.c)
    shimmer(f, t, 5, 0.4)
    stars(f, t, 58, 16, glow=1.0)
    upper = ((f.X - 20) / 21) ** 2 + ((f.Y - 24) / 25) ** 2 < 1
    lower = ((f.X - 16) / 13) ** 2 + ((f.Y - 64) / 16) ** 2 < 1
    f.shape(upper | lower)
    f.trim(C("#fff0ff"), 1, pattern=False, glow=0.5)


@design("wings", "frost", True)
def _(f, t):
    feathers(f, C("#ffffff"), C("#7ab4ee"), tips=C("#cfeaff"))
    shimmer(f, t, 4, 0.5)
    stars(f, t, 59, 10, glow=1.0)
    f.shape(f.Y < f.h - 4 - 14 * (((f.X * 5) % 7) / 7) * ((f.X % 4) == 1))
    f.outline(C("#3a6aa0"), 0.5)


@design("wings", "phantom", True)
def _(f, t):
    membrane(f, C("#6a7aa0"), C("#28304a"), C("#1a2034"), bones=3)
    pulse = 0.5 + 0.5 * math.sin(TAU * t)
    for k in range(3):
        x_end = (k + 0.5) * f.w / 3
        x = 2 + (x_end - 2) * (f.Y / f.h) ** 0.8 + 3
        v = np.abs(f.X - x) < 0.6
        f.c = np.where(v[..., None], C("#7affd8") * (0.5 + 0.5 * pulse), f.c)
        f.e += v[..., None] * C("#7affd8") * pulse
    f.shape(m_ragged(f, 60, 16))
    f.outline()


@design("wings", "void", True)
def _(f, t):
    f.grad(C("#24103a"), C("#05020a"))
    neb = vnoise(f.h, f.w, 3, 61, oy=t)
    f.tint(C("#6a2ab0"), np.clip(neb - 0.5, 0, 1) * 1.6)
    stars(f, t, 62, 30, glow=1.0)
    f.shape(m_ragged(f, 63, 10))
    f.outline()


@design("wings", "mech", True)
def _(f, t):
    f.grad(C("#c8ccd4"), C("#6a6e78"))
    plate = (f.Y % 10 == 0) | ((f.X + (f.Y // 10) * 5) % 10 == 0)
    f.mul(np.where(plate, 0.6, 1.0))
    rivet = (f.Y % 10 == 2) & ((f.X + (f.Y // 10) * 5) % 10 == 2)
    f.c = np.where(rivet[..., None], WHITE, f.c)
    pulse = (np.sin(TAU * t - f.Y * 0.15) * 0.5 + 0.5)
    core = (np.abs(f.X - 20) < 1.2)
    f.c = np.where(core[..., None], C("#ff2a1a") * pulse[..., None] + C("#400000"), f.c)
    f.e += (core * pulse)[..., None] * C("#ff3010")
    shimmer(f, t, 5, 0.3)
    f.shape(m_points(f, 4, 16))
    f.outline(C("#2a2a30"))


@design("wings", "vine")
def _(f, t):
    f.grad(C("#5aa040"), C("#1e4a1a")).cloth(64, 0.08)
    for (x, y, rot) in ((8, 10, 0.4), (24, 18, -0.5), (12, 34, 0.6), (30, 44, -0.3), (16, 58, 0.2), (28, 68, -0.6)):
        emblem(f, poly(LEAF, x, y, 7, rot), C("#8ad86a"), shadow=0.35)
    vine = lambda d, s: d.line([(20 * s, 0), (10 * s, 20 * s), (26 * s, 40 * s), (14 * s, 62 * s), (22 * s, 80 * s)], fill=255, width=int(1.5 * s))
    emblem(f, vine, C("#3a2a10"), highlight=False)
    f.shape(m_points(f, 5, 12))
    f.outline(C("#0a2006"))


@design("wings", "prism", True)
def _(f, t):
    import colorsys
    hue = (f.Y / 80 + t) % 1
    rgb = np.vectorize(lambda h: colorsys.hsv_to_rgb(h, 0.55, 1.0), otypes=[float, float, float])(hue)
    f.c = np.stack(rgb, -1)
    rows = (f.Y % 11 == 10)
    f.mul(np.where(rows, 0.75, 1.0))
    shimmer(f, t, 5, 0.4)
    f.shape(m_scallop(f, 6, 8))
    f.outline()


@design("wings", "storm", True)
def _(f, t):
    feathers(f, C("#5a6276"), C("#1c2028"))
    flash = (t * NF) % 6 in (0, 1)
    pts = [(10, 0), (24, 18), (14, 30), (28, 50), (18, 62), (26, 80)]
    emblem(f, lambda d, s: d.line([(x * s, y * s) for x, y in pts], fill=255, width=int(1.4 * s)),
           C("#fff6a0") if flash else C("#a8b4ff"), glow=1.5 if flash else 0.4, highlight=False)
    f.shape(m_ragged(f, 65, 10))
    f.outline()


@design("wings", "crystal", True)
def _(f, t):
    f.grad(C("#c89aff"), C("#5a2aa0"))
    q = (f.X * 0.7 + f.Y * 0.4) // 6 + (f.X * -0.7 + f.Y * 0.4) // 6
    f.mul(0.8 + 0.25 * ((q % 3) / 2))
    shimmer(f, t, 4, 0.55)
    stars(f, t, 66, 12, glow=1.2)
    f.shape(m_points(f, 4, 18))
    f.trim(C("#f0d8ff"), 1, pattern=False, glow=0.4)
    f.outline(C("#2a0a50"))


# ============================================================ SHIELDS

def s_heater(f):
    return (f.Y < 48) | (((f.X - 23.5) / 24) ** 2 + ((f.Y - 48) / 40) ** 2 < 1)


def s_kite(f):
    top = (f.Y > 2) | (np.abs(f.X - 23.5) < 20)
    return top & (np.abs(f.X - 23.5) < 24 * np.clip(1 - np.clip(f.Y - 26, 0, None) / 64, 0, 1) + 0.5)


def s_round(f):
    return ((f.X - 23.5) / 24) ** 2 + ((f.Y - 43.5) / 26) ** 2 < 1


def s_tower(f):
    return ~(((f.X < 3) | (f.X > 44)) & ((f.Y < 3) | (f.Y > 84)))


def s_spade(f):
    return (((f.X - 23.5) / 24) ** 2 + ((f.Y - 34) / 34) ** 2 < 1) | ((np.abs(f.X - 23.5) < (88 - f.Y) * 0.6) & (f.Y > 40))


def rim(f, col, k=3, rivets=True):
    b = f.band(k)
    shade = 0.85 + 0.3 * np.clip(1 - f.Y / f.h, 0, 1)
    f.c = np.where(b[..., None], col * shade[..., None], f.c)
    inner = f.band(k + 1) & ~b
    f.c = np.where(inner[..., None], f.c * 0.5, f.c)
    if rivets:
        rv = b & ~f.band(1) & ~(f.erode(1) & ~f.erode(2)) & ((f.Y % 8 == 3)) & (np.abs(f.X - 23.5) > 20)
        f.c = np.where(rv[..., None], WHITE * 0.95, f.c)


def boss(f, cx, cy, r, col):
    emblem(f, circle(cx, cy, r), col)
    emblem(f, circle(cx - r * 0.3, cy - r * 0.3, r * 0.35), WHITE * 0.95, shadow=0, highlight=False)


@design("shield", "knight", extra=C("#5a3a20"))
def _(f, t):
    f.grad(C("#d42a36"), C("#7a0c16")).cloth(70, 0.05).light()
    f.shape(s_heater(f))
    cross = (np.abs(f.X - 23.5) < 5) | (np.abs(f.Y - 34) < 5)
    f.c = np.where(cross[..., None], GOLD * (0.9 + 0.2 * (f.Y < 34))[..., None], f.c)
    rim(f, SILVER, 3)
    f.outline()


@design("shield", "buckler", extra=C("#6a4a2a"))
def _(f, t):
    planks = (f.X // 8) % 2
    f.c = np.where(planks[..., None] == 0, C("#a8783e"), C("#8e6432"))
    f.cloth(71, 0.12)
    f.shape(s_round(f))
    rim(f, C("#9aa0aa"), 3)
    boss(f, 23.5, 43.5, 7, C("#c8ccd4"))
    f.outline()


@design("shield", "royal", extra=C("#1a2a5a"))
def _(f, t):
    f.grad(C("#3a62d0"), C("#14286a")).cloth(72, 0.05).light()
    f.shape(s_kite(f))
    fleur = (np.sin(f.X * 1.1) * np.sin(f.Y * 1.1) > 0.75)
    f.mul(np.where(fleur, 1.15, 1.0))
    emblem(f, poly(CROWN, 23.5, 28, 13), GOLD)
    for x in (13, 23.5, 34):
        f.dot(x, 22, C("#e02040"))
    rim(f, GOLD, 3, rivets=False)
    f.outline()


@design("shield", "emerald", True, C("#1a4a2a"))
def _(f, t):
    f.grad(C("#3ad07a"), C("#0e6a34")).cloth(73, 0.04)
    f.shape(s_tower(f))
    emblem(f, poly(GEM, 23.5, 42, 18), C("#9affc0"), glow=0.3)
    f.tint(WHITE, f.layer(poly([(0, -1), (0.75, -0.35), (0, -0.2)], 23.5, 42, 18)) * 0.5)
    shimmer(f, t, 6, 0.55)
    rim(f, C("#e0ffe8"), 2)
    f.outline()


@design("shield", "frost", True, C("#8ab0d0"))
def _(f, t):
    f.grad(C("#f0faff"), C("#8ac0ec")).cloth(74, 0.03)
    f.shape(np.abs(f.X - 23.5) + np.abs(f.Y - 43.5) * 0.55 < 26)
    sf = lambda d, s: [d.line([((23.5 + 16 * math.cos(a)) * s, (43.5 + 16 * math.sin(a)) * s),
                               ((23.5 - 16 * math.cos(a)) * s, (43.5 - 16 * math.sin(a)) * s)], fill=255, width=int(2 * s)) for a in (math.pi / 2, math.pi / 6, 5 * math.pi / 6)]
    emblem(f, sf, WHITE, glow=0.5)
    shimmer(f, t, 5, 0.45)
    stars(f, t, 75, 10, glow=1.0)
    rim(f, WHITE, 2, rivets=False)
    f.outline(C("#4a7aa8"))


@design("shield", "obsidian", True, C("#1a1024"))
def _(f, t):
    f.grad(C("#3a2050"), C("#0e0616"))
    f.mul(0.8 + 0.4 * vnoise(f.h, f.w, 4, 76))
    f.shape(s_heater(f))
    pulse = 0.5 + 0.5 * math.sin(TAU * t)
    rng = np.random.default_rng(77)
    for i in range(6):
        x, y = rng.uniform(10, 38), 12 + i * 11
        glyph = rng.integers(0, 4)
        shapes = [[(-1, -1), (1, -1), (0, 1)], [(-1, 1), (0, -1), (1, 1)], [(-1, 0), (0, -1), (1, 0), (0, 1)], [(-1, -1), (1, 1), (1, -1), (-1, 1)]]
        pts = shapes[glyph]
        emblem(f, lambda d, s, pts=pts, x=x, y=y: d.line([((x + px * 3) * s, (y + py * 3) * s) for px, py in pts] + [((x + pts[0][0] * 3) * s, (y + pts[0][1] * 3) * s)], fill=255, width=int(1.1 * s)),
               C("#c070ff") * (0.5 + 0.5 * ((pulse + i * 0.17) % 1)), glow=(pulse + i * 0.17) % 1, highlight=False)
    rim(f, C("#5a3a7a"), 3)
    f.outline()


@design("shield", "inferno", True, C("#2a1008"))
def _(f, t):
    f.grad(C("#3a0c08"), C("#140302"))
    col, v = flame_field(f, t, 78, 0.8)
    f.tint(col, v)
    f.e += col * (v[..., None] ** 3) * 0.45
    f.shape(s_heater(f))
    emblem(f, poly(FLAME, 23.5, 40, 16), C("#fff0b0"), glow=0.8, shadow=0.3)
    rim(f, C("#3a3a3e"), 3)
    f.outline()


@design("shield", "dragon", extra=C("#2a3a1a"))
def _(f, t):
    q = f.X / 6.0
    r = (f.Y + (np.floor(q) % 2) * 3) / 6.0
    sx, sy = (q % 1) - 0.5, (r % 1)
    scale = np.clip(1 - (sx * sx * 3 + (sy - 0.2) ** 2 * 1.4), 0, 1)
    f.c = C("#2a8a3a") * (0.4 + 0.8 * scale[..., None])
    f.shape(s_heater(f))
    rim(f, C("#d8b860"), 3)
    f.outline()


@design("shield", "tower", extra=C("#4a4a50"))
def _(f, t):
    f.grad(C("#9a9ea8"), C("#5a5e66")).cloth(79, 0.06)
    bands = (np.abs(f.Y - 14) < 2) | (np.abs(f.Y - 44) < 2) | (np.abs(f.Y - 74) < 2)
    f.c = np.where(bands[..., None], C("#c8ccd4"), f.c)
    for y in (14, 44, 74):
        for x in (6, 17, 30, 41):
            f.dot(x, y, WHITE)
    f.shape(s_tower(f))
    rim(f, C("#3a3c42"), 2, rivets=False)
    f.outline()


@design("shield", "creeper", extra=C("#1a3a12"))
def _(f, t):
    rng = np.random.default_rng(80)
    pix = np.kron(rng.choice([0.0, 0.5, 1.0], size=(22, 12)), np.ones((4, 4)))
    f.c = C("#3a8a2a") * (1 - pix[..., None]) + C("#6ccf4a") * pix[..., None] * 0.6 + C("#2a6a1e") * 0.2
    f.shape(s_tower(f))
    creeper_face(f, 12, 26, 4, C("#0c0c0c"))
    rim(f, C("#2a4a20"), 2, rivets=False)
    f.outline()


@design("shield", "netherite", True, C("#2a2428"))
def _(f, t):
    f.grad(C("#4a4246"), C("#221e22"))
    f.mul(0.85 + 0.3 * vnoise(f.h, f.w, 6, 81))
    f.shape(s_heater(f))
    emblem(f, poly(CHEVRON, 23.5, 40, 20), C("#6a6066"))
    emblem(f, poly(CHEVRON, 23.5, 56, 20), C("#6a6066"))
    shimmer(f, t, 4, 0.25)
    rim(f, GOLD, 2, rivets=False)
    f.outline()


@design("shield", "sun", True, C("#6a3a00"))
def _(f, t):
    f.grad(C("#ffd860"), C("#e0701a")).cloth(82, 0.04)
    f.shape(s_round(f))
    ang = np.arctan2(f.Y - 43.5, f.X - 23.5)
    rays = (np.sin(ang * 10 + TAU * t) > 0.2)
    f.tint(C("#fff6c0"), rays * 0.4)
    emblem(f, circle(23.5, 43.5, 10), C("#fff2a0"), glow=0.9)
    rim(f, GOLD, 3)
    f.outline()


@design("shield", "endereye", True, C("#0e1a1a"))
def _(f, t):
    f.grad(C("#14202a"), C("#04080c"))
    f.shape(s_kite(f))
    pulse = 0.55 + 0.45 * math.sin(TAU * t)
    emblem(f, lambda d, s: d.ellipse([8 * s, 30 * s, 40 * s, 52 * s], fill=255), C("#1d8f74"))
    emblem(f, lambda d, s: d.ellipse([12 * s, 32 * s, 36 * s, 50 * s], fill=255), C("#4cffc8") * pulse, glow=pulse, highlight=False)
    emblem(f, lambda d, s: d.ellipse([20 * s, 33 * s, 28 * s, 49 * s], fill=255), C("#03140f"), shadow=0, highlight=False)
    particles(f, t, 83, 10, C("#7affd8"), glow=0.8)
    rim(f, C("#2a3a3a"), 3)
    f.outline()


@design("shield", "ocean", True, C("#06263f"))
def _(f, t):
    f.grad(C("#2fb6e8"), C("#062a5a"))
    for k in range(8):
        y = 6 + k * 11 + 2.5 * np.sin(f.X * 0.3 + TAU * t + k * 1.7)
        f.tint(C("#c8f4ff"), np.clip(1 - np.abs(f.Y - y) / 1.3, 0, 1) * 0.7)
    f.shape(s_heater(f))
    emblem(f, poly(TRIDENT, 23.5, 42, 22), GOLD, glow=0.2)
    rim(f, C("#d8e0e8"), 2)
    f.outline()


@design("shield", "galaxy", True, C("#05040e"))
def _(f, t):
    dx, dy = f.X - 23.5, f.Y - 43.5
    r = np.hypot(dx, dy) + 1e-6
    arm = (np.sin(2 * np.arctan2(dy, dx) + r * 0.3 - TAU * t) + 1) / 2
    f.c = np.zeros_like(f.c) + C("#05040e")
    f.tint(C("#5a3cd0"), arm * np.clip(1 - r / 34, 0, 1) * 0.9)
    f.tint(C("#ff7ad0"), np.clip(arm - 0.6, 0, 1) * np.clip(1 - r / 24, 0, 1) * 1.5)
    f.tint(C("#fff2d0"), np.clip(1 - r / 9, 0, 1) ** 1.5)
    stars(f, t, 84, 34, glow=0.8)
    f.shape(s_round(f))
    rim(f, C("#b8a0ff"), 2, rivets=False)
    f.outline()


@design("shield", "spartan", extra=C("#5a3a10"))
def _(f, t):
    f.grad(C("#e0a040"), C("#8a5214")).light()
    f.mul(0.92 + 0.16 * vnoise(f.h, f.w, 6, 85))
    f.shape(s_round(f))
    emblem(f, poly(LAMBDA, 23.5, 44, 17), C("#8a0e14"))
    rim(f, C("#c08030"), 3, rivets=False)
    f.outline()


@design("shield", "amethyst", True, C("#2a1040"))
def _(f, t):
    f.grad(C("#c89aff"), C("#5a2aa0"))
    q = (f.X * 0.7 + f.Y * 0.4) // 7 + (f.X * -0.7 + f.Y * 0.4) // 7
    f.mul(0.8 + 0.25 * ((q % 3) / 2))
    f.shape(s_spade(f))
    shimmer(f, t, 5, 0.5)
    stars(f, t, 86, 12, glow=1.2)
    rim(f, C("#f0d8ff"), 2, rivets=False)
    f.outline(C("#2a0a50"))


@design("shield", "viking", extra=C("#4a3018"))
def _(f, t):
    seg = (np.floor((np.arctan2(f.Y - 43.5, f.X - 23.5) + math.pi) / (TAU / 8)) % 2)
    f.c = np.where(seg[..., None] == 0, C("#c83a2a"), C("#f0e6d0"))
    planks = (f.X % 8 == 0)
    f.mul(np.where(planks, 0.8, 1.0))
    f.cloth(87, 0.1)
    f.shape(s_round(f))
    rim(f, C("#6a6e76"), 3)
    boss(f, 23.5, 43.5, 6, C("#a8acb4"))
    f.outline()


# ============================================================ run

for kind, name, fn, animated, extra in DESIGNS:
    make(kind, name, fn, animated, extra)
print(len(DESIGNS), "designs")
