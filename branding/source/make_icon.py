"""Draws the Barely Prospecting icon as 32x32 pixel art and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg and barelyprospecting_logo.png (for the jar).

The picture: a wooden panning tray tilted over a pile of silt, gold flakes glinting in it and a ring of gold sparks above,
on the studio's night tile.
"""
import math
from pathlib import Path

from PIL import Image

N = 32
OUT = Path(__file__).resolve().parent.parent / "icon"

BG = (24, 22, 38)
BG_EDGE = (46, 42, 70)
BG_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
WOOD = (150, 104, 62)
WOOD_HI = (188, 138, 86)
WOOD_LO = (92, 62, 38)
SILT = (126, 104, 82)
SILT_HI = (164, 138, 108)
SILT_LO = (84, 68, 52)
OUTLINE = (30, 22, 40)

px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


# Night tile with stepped corners.
R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R - 0.5), N - R - 0.5)
        cy = min(max(y, R - 0.5), N - R - 0.5)
        if math.hypot(x - cx, y - cy) <= R:
            px[y][x] = BG
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            px[y][x] = BG_TOP if y < N // 2 else BG_EDGE

# A soft gold glow behind the tray: dithered discs.
for y in range(N):
    for x in range(N):
        if px[y][x] is None or x in (0, N - 1) or y in (0, N - 1):
            continue
        d = math.hypot(x - 15.5, y - 14.5)
        if d < 11.5:
            t = max(0.0, 1.0 - d / 11.5)
            if t > 0.55 or (t > 0.25 and (x + y) % 2 == 0):
                px[y][x] = mix(BG, GOLD_LO, 0.5 if t > 0.55 else 0.3)

# The tray: a wide ellipse, rim in wood with light from the top left, the inside holding silt.
cx, cy, rx, ry = 15.5, 19.0, 12.5, 7.0
for y in range(N):
    for x in range(N):
        dx, dy = (x - cx) / rx, (y - cy) / ry
        d = dx * dx + dy * dy
        if d <= 1.0:
            lit = -(dx + dy) * 0.5
            c = WOOD_HI if lit > 0.35 else (WOOD if lit > -0.25 else WOOD_LO)
            if d < 0.62:
                c = SILT_HI if lit > 0.25 else (SILT if lit > -0.2 else SILT_LO)
            px[y][x] = c
# tray shadow below the rim
for x in range(5, 27):
    for y in range(24, 28):
        dx, dy = (x - cx) / rx, (y - cy - 1.5) / ry
        if dx * dx + dy * dy <= 1.0 and px[y][x] in (None, BG, BG_EDGE):
            px[y][x] = WOOD_LO
# outline
for y in range(N):
    for x in range(N):
        if px[y][x] in (WOOD, WOOD_HI, WOOD_LO) and any(
                0 <= x + dx < N and 0 <= y + dy < N and px[y + dy][x + dx] in (None, BG, BG_EDGE, BG_TOP) or
                px[y + dy][x + dx] is not None and px[y + dy][x + dx][0] < 70 and px[y + dy][x + dx] == px[y + dy][x + dx]
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            pass
for y in range(N):
    for x in range(N):
        if px[y][x] is None or px[y][x] in (BG, BG_EDGE, BG_TOP) or px[y][x] in [mix(BG, GOLD_LO, 0.5), mix(BG, GOLD_LO, 0.3)]:
            neighbors = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(0 <= a < N and 0 <= b < N and px[b][a] in (WOOD, WOOD_HI, WOOD_LO) for a, b in neighbors):
                put(x, y, OUTLINE)

# Gold flakes glinting in the silt.
for (fx, fy) in ((11, 18), (17, 20), (20, 17), (14, 22)):
    put(fx, fy, GOLD)
    put(fx + 1, fy, GOLD_HI)
    put(fx, fy + 1, GOLD_LO)

# The progress ring above the tray: a broken ring of gold sparks, filled about two thirds around.
for i in range(14):
    a = -math.pi / 2 + i * (2 * math.pi / 20)
    rx2, ry2 = 9.5, 5.5
    x = round(15.5 + math.cos(a) * rx2)
    y = round(9.0 + math.sin(a) * ry2)
    put(x, y, GOLD_HI if i % 3 == 0 else GOLD)

# Sparkle marks, like the other studio icons.
for (sx, sy) in ((5, 7), (27, 6), (26, 14)):
    put(sx, sy, GOLD_HI)
    put(sx - 1, sy, GOLD)
    put(sx + 1, sy, GOLD)
    put(sx, sy - 1, GOLD)
    put(sx, sy + 1, GOLD)

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png", optimize=True)
img.resize((512, 512), Image.NEAREST).save(OUT / "barelyprospecting_logo.png", optimize=True)

rects = []
for y in range(N):
    for x in range(N):
        c = px[y][x]
        if c is not None:
            rects.append(f'<rect x="{x}" y="{y}" width="1.02" height="1.02" fill="#{c[0]:02x}{c[1]:02x}{c[2]:02x}"/>')
(OUT / "icon.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n",
    encoding="utf-8",
)
print("wrote", OUT)
