"""Vanilla Client cosmetics v6 – three collections (Inferno, Celestial, Void).

Pixel art at 2x vanilla resolution, 12 animation frames (vertical strips, 10 fps in game).

  cape   frame 128x64 : front (2,2) 20x32, back (24,2), sides (0,2)/(22,2) 2 wide, top (2,0), bottom (22,0)
  shield frame 128x128: front (2,2) 24x44, back (28,2), sides (0,2)/(26,2), top (2,0), bottom (26,0), handle (52,0) 32x24
  wings  frame 64x96  : model texture (32x48 units): inner wing front (0,0) 24x32 + back (24,0),
                        wing tip front (0,32) 28x40 + back (28,32)
  wings_<id>_preview.png: both wings spread, for the wardrobe cards
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


def C(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)])


def mix(a, b, t):
    t = np.clip(t, 0, 1)
    if np.ndim(t) == 2:
        t = t[..., None]
    return a * (1 - t) + b * t


def noise(h, w, cells, seed, ox=0.0, oy=0.0, octaves=2):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        cx = cells * 2 ** o
        cs = w / cx
        cy = max(1, int(math.ceil(h / cs)))
        g = rng.random((cy, cx))
        ys = (np.arange(h)[:, None] / cs + oy * cy) % cy
        xs = (np.arange(w)[None, :] / cs + ox * cx) % cx
        x0, y0 = np.floor(xs).astype(int), np.floor(ys).astype(int)
        fx, fy = xs - x0, ys - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        x1, y1 = (x0 + 1) % cx, (y0 + 1) % cy
        a = g[y0, x0] * (1 - fx) + g[y0, x1] * fx
        b = g[y1, x0] * (1 - fx) + g[y1, x1] * fx
        out += amp * (a * (1 - fy) + b * fy)
        tot += amp
        amp *= 0.5
    return out / tot


class Canvas:
    """RGB + alpha + glow layer, all float."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.c = np.zeros((h, w, 3))
        self.a = np.ones((h, w), bool)
        self.e = np.zeros((h, w, 3))
        self.Y, self.X = np.mgrid[0:h, 0:w].astype(float)

    def cov(self, fn, ss=4):
        im = Image.new("L", (self.w * ss, self.h * ss), 0)
        fn(ImageDraw.Draw(im), ss)
        return np.asarray(im.resize((self.w, self.h), Image.LANCZOS), float) / 255

    def paint(self, cov, col, glow=0.0):
        self.c = mix(self.c, col, cov)
        if glow:
            self.e += cov[..., None] * col * glow

    def px(self, x, y, col, glow=0.0):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.c[y, x] = col
            if glow:
                self.e[y, x] += col * glow

    def edge(self, k=1):
        m = self.a.copy()
        for _ in range(k):
            p = np.pad(m, 1, constant_values=False)
            m = m & p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
        return self.a & ~m

    def rgba(self, glow_r=1.2):
        c = np.clip(self.c, 0, 1)
        if self.e.max() > 0:
            em = Image.fromarray((np.clip(self.e, 0, 1) * 255).astype(np.uint8))
            bl = np.asarray(em.filter(ImageFilter.GaussianBlur(glow_r)), float) / 255
            c = 1 - (1 - c) * (1 - np.clip(bl + self.e * 0.5, 0, 1))
        out = np.zeros((self.h, self.w, 4), np.uint8)
        out[..., :3] = (np.clip(c, 0, 1) * 255).astype(np.uint8)
        out[..., 3] = self.a * 255
        return out


def put(img, arr, x, y, mirror=False):
    if mirror:
        arr = arr[:, ::-1]
    im = Image.fromarray(arr)
    img.paste(im, (x, y), im)


def box(img, front, fx, fy, side, back_col):
    """Front at (fx,fy), mirrored darker back, edge faces taken from the outer pixels."""
    h, w = front.shape[:2]
    put(img, front, fx, fy)
    back = front.copy()
    back[..., :3] = (np.array(back_col) * 255 * (0.9 + 0.1 * np.random.default_rng(1).random((h, w, 1)))).astype(np.uint8)
    put(img, back, fx + w + side, fy, mirror=True)
    dark = lambda a: np.concatenate([(a[..., :3] * 0.6).astype(np.uint8), a[..., 3:]], -1)
    put(img, dark(np.repeat(front[:, :1], side, 1)), fx - side, fy)
    put(img, dark(np.repeat(front[:, -1:], side, 1)), fx + w, fy)
    put(img, dark(np.repeat(front[:1], side, 0)), fx, fy - side)
    put(img, dark(np.repeat(front[-1:], side, 0)), fx + w, fy - side)


def strip(frames, name):
    w, h = frames[0].size
    out = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        out.paste(f, (0, i * h))
    out.save(OUT / name, optimize=True)


def twinkle(cv, t, seed, n, col, region=None, glow=1.0):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        x, y = rng.uniform(1, cv.w - 1), rng.uniform(1, cv.h - 1 if region is None else region)
        ph = rng.uniform(0, 1)
        b = 0.5 + 0.5 * math.cos(TAU * (t + ph))
        if b < 0.3:
            continue
        cv.px(x, y, mix(cv.c[int(y), int(x)], col, b), glow * b ** 2)
        if b > 0.85:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = int(x) + dx, int(y) + dy
                if 0 <= xx < cv.w and 0 <= yy < cv.h:
                    cv.c[yy, xx] = mix(cv.c[yy, xx], col, 0.45)


def embers(cv, t, seed, n, col, speed=1.0):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        x0, y0, ph = rng.uniform(0, cv.w), rng.uniform(0, cv.h), rng.uniform(0, 1)
        k = (t * speed + ph) % 1
        y = (y0 - k * cv.h) % cv.h
        x = (x0 + math.sin(TAU * (k + ph)) * 1.5) % cv.w
        f = math.sin(math.pi * k)
        cv.px(x, y, col * (0.6 + 0.4 * f), 1.2 * f)


def flames(cv, t, seed, height, hot="#fff2a8", mid="#ff9a1c", low="#d6340c", base=None):
    n = noise(cv.h, cv.w, 3, seed, oy=t, octaves=2)
    yy = cv.Y / (cv.h - 1)
    v = np.clip((yy - (1 - height)) / height + (n - 0.5) * 0.9, 0, 1)
    v = np.floor(v * 5) / 5  # stepped = pixel-art flames
    col = np.where(v[..., None] > 0.75, C(hot), np.where(v[..., None] > 0.45, C(mid), C(low)))
    cv.c = np.where((v > 0.15)[..., None], col, cv.c)
    cv.e += ((v > 0.6)[..., None] * col) * 0.5


def border(cv, col, inset=0, pattern=None):
    m = np.zeros((cv.h, cv.w), bool)
    m[inset, inset:cv.w - inset] = m[cv.h - 1 - inset, inset:cv.w - inset] = True
    m[inset:cv.h - inset, inset] = m[inset:cv.h - inset, cv.w - 1 - inset] = True
    c = np.broadcast_to(col, cv.c.shape)
    if pattern is not None:
        c = np.where(((cv.X + cv.Y) % 3 == 0)[..., None], c * 1.25, c * 0.95)
    cv.c = np.where(m[..., None], c, cv.c)


def sun(cv, cx, cy, r, t, col, ray, rays=8, glow=0.8):
    for i in range(rays):
        a = TAU * (i / rays + t / rays)
        x1, y1 = cx + math.cos(a) * (r + 1), cy + math.sin(a) * (r + 1)
        x2, y2 = cx + math.cos(a) * (r + 3.5), cy + math.sin(a) * (r + 3.5)
        cv.paint(cv.cov(lambda d, s: d.line([(x1 * s, y1 * s), (x2 * s, y2 * s)], fill=255, width=int(1.2 * s))), ray, glow * 0.5)
    cv.paint(cv.cov(lambda d, s: d.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], fill=255)), col, glow)


FLAME = [(0, -1), (0.3, -0.45), (0.6, -0.2), (0.62, 0.3), (0.4, 0.75), (0, 1), (-0.4, 0.75), (-0.62, 0.3), (-0.45, -0.1), (-0.25, 0.1), (-0.12, -0.4)]


def flame_icon(cv, cx, cy, s, t, outer="#ff7a1a", inner="#fff0a0"):
    wob = 1 + 0.08 * math.sin(TAU * t * 2)
    poly = lambda k: (lambda d, ss: d.polygon([((cx + x * s * k) * ss, (cy + y * s * k * wob) * ss) for x, y in FLAME], fill=255))
    cv.paint(cv.cov(poly(1.0)), C(outer), 0.6)
    cv.paint(cv.cov(lambda d, ss: d.polygon([((cx + x * s * 0.5) * ss, (cy + s * 0.25 + y * s * 0.55 * wob) * ss) for x, y in FLAME], fill=255)), C(inner), 0.8)


# ======================================================================== CAPES (20x32)

def cape_frame(cv, lining):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    box(img, cv.rgba(), 2, 2, 2, lining)
    return img


def cape_inferno(t):
    cv = Canvas(20, 32)
    n = noise(32, 20, 3, 1, octaves=2)
    cv.c = mix(C("#2a0a08"), C("#4a120c"), n)
    flames(cv, t, 2, 0.42)
    embers(cv, t, 3, 6, C("#ffb040"))
    flame_icon(cv, 10, 11, 5, t)
    border(cv, C("#d9a43a"), 0, pattern=True)
    return cape_frame(cv, C("#2a0a08"))


def cape_celestial(t):
    cv = Canvas(20, 32)
    cv.c = mix(C("#fffaf0"), C("#e8dcc4"), cv.Y / 31)
    cv.c *= (1 + 0.05 * np.sin(cv.X * 1.2))[..., None]  # folds
    sun(cv, 10, 12, 3.2, t, C("#fff2b0"), C("#f2c440"))
    # little wings beside the sun
    for sx in (-1, 1):
        cv.paint(cv.cov(lambda d, s, sx=sx: d.polygon([((10 + sx * 5) * s, 11 * s), ((10 + sx * 9) * s, 8 * s), ((10 + sx * 8) * s, 13 * s), ((10 + sx * 5) * s, 14 * s)], fill=255)), C("#f6d77a"))
    twinkle(cv, t, 4, 10, C("#ffd75a"))
    for y in (24, 27):
        cv.c[y, 2:18] = mix(cv.c[y, 2:18], C("#e2b23e"), 0.8)
    border(cv, C("#e2b23e"), 0, pattern=True)
    border(cv, C("#f7e3a2"), 1)
    return cape_frame(cv, C("#e8dcc4"))


def cape_void(t):
    cv = Canvas(20, 32)
    dx, dy = cv.X - 10, cv.Y - 15
    r = np.hypot(dx, dy) + 1e-6
    arm = (np.sin(2 * np.arctan2(dy, dx) + r * 0.45 - TAU * t) + 1) / 2
    cv.c = np.zeros_like(cv.c) + C("#07040e")
    cv.c = mix(cv.c, C("#3c1d78"), arm * np.clip(1 - r / 16, 0, 1))
    cv.c = mix(cv.c, C("#c04cff"), np.clip(arm - 0.65, 0, 1) * 2 * np.clip(1 - r / 12, 0, 1))
    cv.paint(np.clip(1 - r / 3.2, 0, 1), C("#ffe6ff"), 0.7)
    twinkle(cv, t, 5, 16, C("#ffffff"))
    pulse = 0.5 + 0.5 * math.sin(TAU * t)
    trim = mix(C("#5a1fa0"), C("#c070ff"), pulse)
    border(cv, trim, 0)
    cv.e += cv.edge(1)[..., None] * trim * 0.4 * pulse
    return cape_frame(cv, C("#0c0616"))


# ======================================================================== SHIELDS (round, 36x36 plate)
SD = 36


def shield_frame(cv, wood):
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    box(img, cv.rgba(), 2, 2, 2, wood)
    handle = np.zeros((24, 32, 4), np.uint8)
    handle[..., :3] = (np.array(wood) * 0.75 * 255).astype(np.uint8)
    handle[..., 3] = 255
    put(img, handle, 0, 40)
    return img


def round_canvas():
    cv = Canvas(SD, SD)
    c = (SD - 1) / 2
    cv.r = np.hypot(cv.X - c, cv.Y - c)
    cv.a = cv.r < SD / 2 - 0.2
    return cv


def rim(cv, col, k=3, rivets=True, glow=0.0):
    e = cv.edge(k)
    light = (1.2 - 0.45 * (cv.X + cv.Y) / (2 * SD))[..., None]  # lit from the top left
    cv.c = np.where(e[..., None], col * light, cv.c)
    inner = cv.edge(k + 1) & ~e
    cv.c = np.where(inner[..., None], cv.c * 0.5, cv.c)
    cv.c = np.where(cv.edge(1)[..., None], cv.c * 0.6, cv.c)
    if glow:
        cv.e += e[..., None] * col * glow
    if rivets:
        c = (SD - 1) / 2
        for i in range(8):
            a = TAU * i / 8 + TAU / 16
            cv.px(c + math.cos(a) * (SD / 2 - 2.2), c + math.sin(a) * (SD / 2 - 2.2), C("#f4f4f4"))


def shield_inferno(t):
    cv = round_canvas()
    cv.c = mix(C("#6a160e"), C("#2a0806"), cv.Y / SD)
    cv.c *= (1 + 0.15 * (noise(SD, SD, 4, 7) - 0.5))[..., None]
    flames(cv, t, 8, 0.45)
    flame_icon(cv, 17.5, 15, 8, t)
    embers(cv, t, 9, 6, C("#ffb040"))
    cv.e *= (cv.r < SD / 2 - 5)[..., None]  # no glow over the metal rim
    rim(cv, C("#5c5c66"))
    return shield_frame(cv, C("#3a2a22"))


def shield_celestial(t):
    cv = round_canvas()
    cv.c = mix(C("#fffdf6"), C("#e6dbc2"), cv.r / (SD / 2))
    for k in range(2):
        ring = np.abs(cv.r - (11 + k * 3)) < 0.6
        cv.c = np.where(ring[..., None], mix(cv.c, C("#e2b23e"), 0.6), cv.c)
    sun(cv, 17.5, 17.5, 5, t, C("#ffdf6a"), C("#e8a820"), rays=12, glow=0.4)
    twinkle(cv, t, 10, 8, C("#ffd75a"))
    rim(cv, C("#e8b83e"))
    return shield_frame(cv, C("#cfc2a6"))


def shield_void(t):
    cv = round_canvas()
    cv.c = mix(C("#1e1230"), C("#06030c"), cv.r / (SD / 2))
    cv.c *= (0.85 + 0.3 * noise(SD, SD, 5, 11))[..., None]
    pulse = 0.55 + 0.45 * math.sin(TAU * t)
    cv.paint(cv.cov(lambda d, s: d.ellipse([6 * s, 10 * s, 30 * s, 26 * s], fill=255)), C("#1d8f74"))
    cv.paint(cv.cov(lambda d, s: d.ellipse([8.5 * s, 11.5 * s, 27.5 * s, 24.5 * s], fill=255)), mix(C("#2a9f84"), C("#6cffd0"), pulse), 0.9 * pulse)
    cv.paint(cv.cov(lambda d, s: d.ellipse([15.5 * s, 12 * s, 20 * s, 24 * s], fill=255)), C("#04140f"))
    twinkle(cv, t, 12, 10, C("#d8a0ff"))
    rim(cv, mix(C("#3a2050"), C("#a050f0"), pulse), rivets=False, glow=0.3 * pulse)
    return shield_frame(cv, C("#1a1024"))


# ======================================================================== WINGS
# Canvas in model space: 26 x 20 units (x 0..26 from the back outwards, y -8..12), at 2x = 52 x 40 px.
# Root (shoulder) at the left edge, the hinge to the wing tip at x = 24 px.

W, H = 52, 40
ARM = [(0, 17), (12, 13), (24, 10), (38, 5), (51, 1)]  # top bone, root -> tip


def scallop_poly(points, depth):
    """Bottom edge between finger tips, curving inwards."""
    out = []
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        for k in range(6):
            f = k / 6
            x = x1 + (x2 - x1) * f
            y = y1 + (y2 - y1) * f - depth * math.sin(math.pi * f)
            out.append((x, y))
    out.append(points[-1])
    return out


def membrane_wing(t, base, dark, bone, vein, seed, sparkles=None):
    cv = Canvas(W, H)
    tips = [(2, 30), (13, 38), (25, 39), (37, 33), (47, 22), (51, 1)]
    wrist = (24, 10)
    outline = ARM + list(reversed(scallop_poly(tips, 5)))[:-1] + [(0, 30)]
    shape = cv.cov(lambda d, s: d.polygon([(x * s, y * s) for x, y in [(0, 17)] + ARM[1:] + list(reversed(scallop_poly(tips, 5)))], fill=255))
    cv.a = shape > 0.5
    # membrane shading: darker near the bones, lighter in the middle of each panel
    n = noise(H, W, 3, seed, octaves=2)
    cv.c = mix(base, dark, 0.35 + 0.4 * (cv.Y / H) + 0.25 * (n - 0.5))
    # glowing veins (animated)
    v = noise(H, W, 5, seed + 1, ox=t * 0.5, octaves=1)
    glow = np.clip(1 - np.abs(v - 0.5) * 30, 0, 1) * (0.6 + 0.4 * math.sin(TAU * t))
    cv.c = mix(cv.c, vein, glow * 0.9)
    cv.e += glow[..., None] * vein * 0.6
    # bones
    bones = cv.cov(lambda d, s: [d.line([(x * s, y * s) for x, y in ARM], fill=255, width=int(2.4 * s))]
                   + [d.line([(wrist[0] * s, wrist[1] * s), (x * s, y * s)], fill=255, width=int(1.3 * s)) for x, y in tips[:-1]]
                   + [d.line([(0, 18 * s), (tips[0][0] * s, tips[0][1] * s)], fill=255, width=int(1.3 * s))])
    cv.c = mix(cv.c, bone, np.clip(bones * 1.3, 0, 1))
    # claw at the wrist
    cv.paint(cv.cov(lambda d, s: d.polygon([(23 * s, 9 * s), (26 * s, 5 * s), (26 * s, 10 * s)], fill=255)), bone * 1.4)
    if sparkles is not None:
        twinkle(cv, t, seed + 2, 14, sparkles, glow=1.0)
    e = cv.edge(1)
    cv.c = np.where(e[..., None], cv.c * 0.45, cv.c)
    return cv


def feather_wing(t, light, shade, tip, seed):
    """Layered feathers: long primaries at the tip, secondaries below the arm, small coverts on top."""
    SS = 6
    im = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    to8 = lambda c: tuple(int(v * 255) for v in np.clip(c, 0, 1))

    def feather(x, y, length, width, angle, col, edge):
        ca, sa = math.cos(angle), math.sin(angle)
        pts = []
        for k in range(16):
            f = k / 15
            w_ = width * (0.55 + 0.45 * math.sin(math.pi * min(1.0, f * 0.95 + 0.05)))
            if f > 0.75:
                w_ *= max(0.0, 1 - (f - 0.75) / 0.25) ** 0.6
            pts.append((f, w_))
        left = [(x + ca * f * length - sa * w_, y + sa * f * length + ca * w_) for f, w_ in pts]
        right = [(x + ca * f * length + sa * w_, y + sa * f * length - ca * w_) for f, w_ in reversed(pts)]
        poly = [(px * SS, py * SS) for px, py in left + right]
        d.polygon(poly, fill=to8(col) + (255,), outline=to8(edge) + (255,), width=SS)
        d.line([(x * SS, y * SS), ((x + ca * length * 0.8) * SS, (y + sa * length * 0.8) * SS)], fill=to8(col * 0.86) + (255,), width=max(1, SS // 2))

    # primaries (outer, long, fanning down)
    for i in range(6):
        f = i / 5
        feather(26 + f * 16, 9 - f * 5, 24 - f * 7, 2.6, math.radians(88 - f * 30), mix(light, tip, 0.35 + f * 0.2), shade * 0.8)
    # secondaries (under the arm)
    for i in range(7):
        f = i / 6
        feather(2 + f * 24, 16 - f * 5, 19 + f * 4, 2.6, math.radians(93 - f * 8), mix(light, tip, 0.15), shade * 0.85)
    # coverts (short, on top)
    for i in range(10):
        f = i / 9
        feather(1 + f * 46, 16 - f * 13, 8 - f * 2, 2.2, math.radians(98 - f * 30), light, shade)
    # arm
    d.line([(x * SS, y * SS) for x, y in ARM], fill=to8(light) + (255,), width=int(2.4 * SS))
    small = np.asarray(im.resize((W, H), Image.LANCZOS), float) / 255
    cv = Canvas(W, H)
    cv.a = small[..., 3] > 0.45
    cv.c = np.where(small[..., 3:4] > 0, small[..., :3] / np.maximum(small[..., 3:4], 1e-3), light)
    return cv


def wing_inferno(t):
    cv = membrane_wing(t, C("#a8180c"), C("#2a0604"), C("#1a0806"), C("#ffa21c"), 21)
    embers(cv, t, 22, 6, C("#ffc050"))
    return cv


def wing_celestial(t):
    cv = feather_wing(t, C("#fffdf6"), C("#d8d2c4"), C("#f2c440"), 31)
    # golden light sweeping along the feathers
    pos = (t * 1.4 - 0.2) * (W + H)
    d = np.abs(cv.X + cv.Y * 0.6 - pos)
    s = np.clip(1 - d / 5, 0, 1) * cv.a
    cv.c = mix(cv.c, C("#fff4c0"), s * 0.7)
    cv.e += s[..., None] * C("#ffe08a") * 0.4
    twinkle(cv, t, 32, 8, C("#fff2a0"))
    cv.a &= cv.a  # keep
    return cv


def wing_void(t):
    return membrane_wing(t, C("#2a1446"), C("#06030c"), C("#120a1e"), C("#c04cff"), 41, sparkles=C("#ffffff"))


def wings_frame(cv):
    """Splits the canvas into the inner wing (24 px) and the tip (28 px) model faces."""
    arr = cv.rgba()
    img = Image.new("RGBA", (64, 96), (0, 0, 0, 0))
    inner = arr[8:40, 0:24]
    tipa = arr[0:40, 24:52]
    put(img, inner, 0, 0)
    put(img, inner, 24, 0, mirror=True)
    put(img, tipa, 0, 32)
    put(img, tipa, 28, 32, mirror=True)
    return img, arr


def wings(name, fn):
    frames, preview = [], None
    for i in range(NF):
        img, arr = wings_frame(fn(i / NF))
        frames.append(img)
        if i == 0:
            preview = arr
    strip(frames, f"wings_{name}.png")
    p = Image.new("RGBA", (W * 2 + 4, H), (0, 0, 0, 0))
    put(p, preview, W + 4, 0)
    put(p, preview, 0, 0, mirror=True)
    p.save(OUT / f"wings_{name}_preview.png")


def anim(fn, name):
    strip([fn(i / NF) for i in range(NF)], name)


for coll in ("inferno", "celestial", "void"):
    anim(globals()[f"cape_{coll}"], f"cape_{coll}.png")
    anim(globals()[f"shield_{coll}"], f"shield_{coll}.png")
    wings(coll, globals()[f"wing_{coll}"])
print("ok")
