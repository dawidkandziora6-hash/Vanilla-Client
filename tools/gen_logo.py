"""Generates the 'VANILLA CLIENT' logo (pixel font, 3D edge) and its white mask (for the shine)."""
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(sys.argv[1])
OUT.mkdir(parents=True, exist_ok=True)

FONT = {
    "V": ["X...X", "X...X", "X...X", "X...X", ".X.X.", ".X.X.", "..X.."],
    "A": [".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"],
    "N": ["X...X", "XX..X", "X.X.X", "X.X.X", "X..XX", "X...X", "X...X"],
    "I": ["XXX", ".X.", ".X.", ".X.", ".X.", ".X.", "XXX"],
    "L": ["X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"],
    "C": [".XXXX", "X....", "X....", "X....", "X....", "X....", ".XXXX"],
    "E": ["XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX"],
    "T": ["XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."],
    " ": ["...", "...", "...", "...", "...", "...", "..."],
}
P = 2          # texels per font pixel
DEPTH = 2      # 3D extrusion in texels
words = [("VANILLA", ((255, 247, 214), (229, 190, 104))), (" ", None), ("CLIENT", ((150, 222, 96), (66, 140, 44)))]

# measure
cells = []
for word, cols in words:
    for ch in word:
        cells.append((ch, cols))
width = sum((len(FONT[c][0]) + 1) * P for c, _ in cells) + 2 + DEPTH
height = 7 * P + 2 + DEPTH
img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
mask = Image.new("RGBA", (width, height), (0, 0, 0, 0))
rnd = random.Random(7)

# face pixels
face = {}
x0 = 1
for ch, cols in cells:
    g = FONT[ch]
    for gy, row in enumerate(g):
        for gx, c in enumerate(row):
            if c != "X":
                continue
            for dy in range(P):
                for dx in range(P):
                    face[(x0 + gx * P + dx, 1 + gy * P + dy)] = cols
    x0 += (len(g[0]) + 1) * P


def mixc(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# extrusion (dark sides), drawn first
for (x, y), cols in face.items():
    for d in range(1, DEPTH + 1):
        if (x + d, y + d) not in face:
            base = mixc(cols[1], (0, 0, 0), 0.55)
            img.putpixel((x + d, y + d), base + (255,))
# outline
for (x, y) in list(face.keys()):
    for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
        if (nx, ny) not in face and img.getpixel((nx, ny))[3] == 0:
            img.putpixel((nx, ny), (30, 24, 18, 255))
# face with vertical gradient + subtle noise + top highlight
for (x, y), cols in face.items():
    t = (y - 1) / (7 * P - 1)
    c = mixc(cols[0], cols[1], t)
    n = rnd.uniform(-0.05, 0.05)
    c = tuple(max(0, min(255, int(v * (1 + n)))) for v in c)
    if (x, y - 1) not in face:
        c = mixc(c, (255, 255, 255), 0.35)
    img.putpixel((x, y), c + (255,))
for y in range(height):
    for x in range(width):
        if img.getpixel((x, y))[3] > 0:
            mask.putpixel((x, y), (255, 255, 255, 255))
img.save(OUT / "logo.png")
mask.save(OUT / "logo_mask.png")
print(width, height)
