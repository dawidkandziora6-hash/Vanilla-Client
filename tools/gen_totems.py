"""Built-in totem textures (16x16, own pixel art in the style of an item)."""
import sys
from pathlib import Path

from PIL import Image

OUT = Path(sys.argv[1])

SHAPE = [
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

DESIGNS = {
    # head, head-light, eyes, mouth, body, body-light, gem, wing, wing-light
    "king": ((150, 105, 20), (250, 205, 70), (40, 20, 10), (120, 70, 10), (165, 115, 25), (245, 200, 80), (60, 200, 90), (190, 140, 40), (255, 225, 120)),
    "diamond": ((30, 120, 140), (120, 235, 235), (10, 40, 60), (20, 90, 110), (35, 135, 150), (130, 240, 240), (255, 255, 255), (40, 150, 170), (190, 255, 255)),
    "emerald": ((20, 110, 50), (80, 220, 120), (10, 40, 20), (15, 80, 40), (25, 125, 60), (90, 230, 130), (255, 230, 90), (30, 140, 70), (150, 255, 170)),
    "ender": ((25, 15, 40), (90, 60, 140), (110, 255, 200), (60, 30, 90), (30, 20, 50), (120, 80, 180), (200, 100, 255), (45, 25, 70), (160, 110, 230)),
    "blood": ((110, 10, 15), (220, 40, 50), (255, 230, 120), (70, 5, 10), (120, 12, 18), (230, 60, 60), (255, 200, 60), (90, 10, 15), (200, 50, 60)),
    "frost": ((120, 170, 210), (235, 250, 255), (30, 60, 110), (90, 140, 190), (130, 180, 220), (240, 252, 255), (90, 180, 255), (150, 200, 235), (255, 255, 255)),
}

for name, (H, h, E, M, B, b, G, W, w) in DESIGNS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pal = {"H": H, "h": h, "E": E, "M": M, "B": B, "b": b, "G": G, "W": W, "w": w}
    for y, row in enumerate(SHAPE):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch] + (255,))
    img.save(OUT / f"totem_{name}.png")
print("ok")
