"""Pixel-art drawing helpers and every texture of the mod, at 32x32 (original art, drawn with code, nothing from Minecraft).

Shapes are lit from the top left: each filled shape gets a highlight, a body tone and a shadow edge, then a dark outline.
"""
import math
import random
from PIL import Image

N = 32


def clamp(v):
    return max(0, min(255, int(v)))


def tone(color, amount):
    return tuple(clamp(c + amount) for c in color[:3]) + (255,)


def blank():
    return Image.new('RGBA', (N, N), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < N and 0 <= y < N:
        img.putpixel((x, y), color if len(color) == 4 else tuple(color) + (255,))


def ellipse(img, cx, cy, rx, ry, color, light=34, rng=None, grain=6):
    """A lit ellipse: bright upper left, dark lower right, optional grain."""
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = dx * dx + dy * dy
            if d <= 1.0:
                lit = (-dx - dy) * 0.5
                amount = lit * light - d * 10
                if rng:
                    amount += rng.randint(-grain, grain)
                put(img, x, y, tone(color, amount))


def polygon(img, pts, color, light=30, rng=None, grain=5):
    """A filled polygon with the same top-left light, by scanline."""
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    cx, cy = sum(xs) / len(xs), sum(ys) / len(ys)
    w, h = max(1, max(xs) - min(xs)), max(1, max(ys) - min(ys))
    for y in range(N):
        for x in range(N):
            if inside(pts, x + 0.5, y + 0.5):
                lit = (-(x + 0.5 - cx) / w - (y + 0.5 - cy) / h)
                amount = lit * light
                if rng:
                    amount += rng.randint(-grain, grain)
                put(img, x, y, tone(color, amount))


def inside(pts, px, py):
    ok = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi + 1e-9) + xi:
            ok = not ok
        j = i
    return ok


def line(img, x0, y0, x1, y1, color, width=1.0, rng=None, grain=0):
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
    for s in range(steps + 1):
        t = s / steps
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        r = width / 2.0
        for yy in range(int(y - r - 1), int(y + r + 2)):
            for xx in range(int(x - r - 1), int(x + r + 2)):
                if (xx + 0.5 - x) ** 2 + (yy + 0.5 - y) ** 2 <= r * r + 0.2:
                    shade = (-(xx + 0.5 - x) - (yy + 0.5 - y)) * 7
                    if rng:
                        shade += rng.randint(-grain, grain)
                    put(img, xx, yy, tone(color, shade))


def outline(img, color=(26, 20, 22)):
    """A one pixel dark outline around everything that is drawn, a little darker at the bottom right."""
    src = img.copy()
    for y in range(N):
        for x in range(N):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < N and 0 <= ny < N and src.getpixel((nx, ny))[3] > 0:
                        put(img, x, y, tone(color, 0 if (dx + dy) < 0 else -8))
                        break
    return img


def speckle(img, rng, color, count, size=1):
    for _ in range(count):
        x, y = rng.randint(0, N - 1), rng.randint(0, N - 1)
        if img.getpixel((x, y))[3] > 0:
            for dx in range(size):
                for dy in range(size):
                    if x + dx < N and y + dy < N and img.getpixel((x + dx, y + dy))[3] > 0:
                        put(img, x + dx, y + dy, tone(color, rng.randint(-10, 10)))


# ---------------------------------------------------------------- block textures
def blotchy(base, spread, seed, cell=4, fine=7):
    """Cloudy noise: coarse blotches plus fine grain, so large surfaces do not look like static."""
    rng = random.Random(seed)
    coarse = [[rng.randint(-spread, spread) for _ in range(N // cell + 2)] for _ in range(N // cell + 2)]
    img = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            gx, gy = x / cell, y / cell
            x0, y0 = int(gx), int(gy)
            fx, fy = gx - x0, gy - y0
            v = (coarse[y0][x0] * (1 - fx) * (1 - fy) + coarse[y0][x0 + 1] * fx * (1 - fy)
                 + coarse[y0 + 1][x0] * (1 - fx) * fy + coarse[y0 + 1][x0 + 1] * fx * fy)
            put(img, x, y, tone(base, v + rng.randint(-fine, fine)))
    return img


def pit_wear(img, stage, seed):
    """Worn pans: shallow dark dents with a lit rim, more of them at every stage."""
    rng = random.Random(seed)
    for _ in range(stage * 5):
        cx, cy, r = rng.randint(3, 28), rng.randint(3, 28), rng.choice([1.6, 2.2, 2.8])
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r and 0 <= x < N and 0 <= y < N:
                    lit = ((x + 0.5 - cx) + (y + 0.5 - cy)) / r
                    p = img.getpixel((x, y))
                    put(img, x, y, tone(p, -34 + lit * 10))
                elif d <= r + 1.0 and 0 <= x < N and 0 <= y < N:
                    p = img.getpixel((x, y))
                    put(img, x, y, tone(p, 12))
    return img


def pannable(name, base, spread, speck, seed):
    out = []
    for wear in range(4):
        img = blotchy(base, spread, seed)
        rng = random.Random(seed + 1)
        for _ in range(55):
            x, y = rng.randint(0, N - 1), rng.randint(0, N - 1)
            put(img, x, y, tone(speck, rng.randint(-14, 14)))
            if rng.random() < 0.4:
                put(img, min(N - 1, x + 1), y, tone(speck, -10))
        pit_wear(img, wear, seed + 7)
        out.append(img)
    return out


def rubble():
    rng = random.Random(33)
    img = blotchy((92, 92, 98), 10, 33, cell=8, fine=5)
    # angular chips of stone with lit top edges and dark cracks between them
    for _ in range(14):
        cx, cy = rng.randint(2, 29), rng.randint(2, 29)
        r = rng.randint(3, 6)
        pts = [(cx + math.cos(a) * r * rng.uniform(0.7, 1.2), cy + math.sin(a) * r * rng.uniform(0.7, 1.2))
               for a in [i * math.pi / 3 + rng.uniform(-0.3, 0.3) for i in range(6)]]
        base = rng.choice([(128, 126, 132), (112, 110, 118), (146, 144, 148), (100, 98, 106)])
        layer = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        polygon(layer, pts, base, light=44, rng=rng, grain=5)
        edge = layer.copy()
        outline(edge, (52, 50, 56))
        img.alpha_composite(edge)
    return img


def planks(base, seed, dark_seam=30):
    rng = random.Random(seed)
    img = Image.new('RGBA', (N, N))
    for y in range(N):
        plank = y // 8
        prng = random.Random(seed + plank)
        shift = prng.randint(-10, 10)
        for x in range(N):
            grain = math.sin((x + plank * 7) * 0.9 + math.sin(y * 0.5) * 1.5) * 6
            v = shift + grain + rng.randint(-4, 4)
            if y % 8 == 7:
                v -= dark_seam
            if y % 8 == 0:
                v += 10
            if (x + plank * 13) % 29 == 0 and y % 8 in (3, 4):
                v -= 24  # a nail
            put(img, x, y, tone(base, v))
    return img


def clay_pot_side(base, seed, bands=True):
    img = blotchy(base, 8, seed, cell=6, fine=5)
    if bands:
        for yy in (6, 7, 22, 23):
            for x in range(N):
                p = img.getpixel((x, yy))
                put(img, x, yy, tone(p, -30 if yy % 2 else -16))
        for yy in (8, 24):
            for x in range(N):
                p = img.getpixel((x, yy))
                put(img, x, yy, tone(p, 14))
    return img


def lava(seed):
    rng = random.Random(seed)
    img = blotchy((222, 96, 20), 38, seed, cell=5, fine=10)
    for _ in range(26):
        x, y = rng.randint(0, N - 1), rng.randint(0, N - 1)
        for dx in range(rng.randint(1, 3)):
            put(img, min(N - 1, x + dx), y, (255, 214 - rng.randint(0, 40), 70, 255))
    for _ in range(30):
        x, y = rng.randint(0, N - 1), rng.randint(0, N - 1)
        put(img, x, y, (120, 36, 10, 255))
    return img


def water(seed):
    rng = random.Random(seed)
    img = blotchy((52, 108, 200), 16, seed, cell=6, fine=6)
    for _ in range(10):
        x, y = rng.randint(0, N - 8), rng.randint(0, N - 1)
        for dx in range(rng.randint(3, 7)):
            put(img, x + dx, y, (150, 200, 250, 255))
    return img


def lure_top(meals, seed):
    rng = random.Random(seed)
    img = blotchy((120, 90, 50), 14, seed, cell=4, fine=8)
    # twigs woven around a hollow
    for i in range(16):
        a = i * math.pi / 8
        x0, y0 = 15.5 + math.cos(a) * 14, 15.5 + math.sin(a) * 14
        x1, y1 = 15.5 + math.cos(a + 0.9) * 9, 15.5 + math.sin(a + 0.9) * 9
        line(img, x0, y0, x1, y1, (96 + rng.randint(-14, 14), 70, 40), 2.0, rng, 4)
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if d < 7.2:
                put(img, x, y, tone((58, 42, 26), -d * 2 + rng.randint(-5, 5)))
    seeds = [(232, 208, 120), (214, 186, 98), (240, 224, 150)]
    for k in range(meals * 6):
        ang = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(0, 5.2)
        x, y = int(16 + math.cos(ang) * r), int(16 + math.sin(ang) * r)
        c = rng.choice(seeds)
        put(img, x, y, c + (255,))
        put(img, x + 1, y, tone(c, -30))
        put(img, x, y - 1, tone(c, 18))
    return img


# ---------------------------------------------------------------- item sprites
def twig():
    img = blank()
    rng = random.Random(3)
    bark = (126, 90, 54)
    line(img, 4, 28, 27, 5, bark, 3.0, rng, 5)
    line(img, 11, 21, 7, 13, bark, 2.0, rng, 4)
    line(img, 17, 15, 24, 18, bark, 2.0, rng, 4)
    line(img, 22, 10, 22, 4, bark, 1.6, rng, 4)
    for lx, ly in ((7, 12), (25, 19), (22, 3), (28, 5)):
        polygon(img, [(lx - 2, ly + 1), (lx, ly - 3), (lx + 3, ly), (lx, ly + 3)], (84, 150, 58), 40, rng, 5)
    return outline(img)


def pebble():
    img = blank()
    rng = random.Random(4)
    ellipse(img, 11, 20, 8, 6, (132, 132, 140), 44, rng, 7)
    ellipse(img, 22, 17, 6, 5, (110, 108, 118), 40, rng, 7)
    ellipse(img, 20, 25, 5, 4, (150, 148, 154), 40, rng, 7)
    return outline(img)


def dust(base):
    img = blank()
    rng = random.Random(5)
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5 - 16) / 13.0, (y + 0.5 - 24) / 12.0
            h = 1.0 - dx * dx
            if h > 0 and dy < 0 and (y + 0.5) > 24 - 14 * h * h and (y + 0.5) < 27 + (1 - abs(dx)) * 2:
                lit = (-dx - dy * 0.6) * 22
                put(img, x, y, tone(base, lit + rng.randint(-9, 9)))
    speckle(img, rng, tone(base, 44), 20)
    return outline(img)


def flakes(base):
    img = blank()
    rng = random.Random(base[0])
    for cx, cy, r in ((9, 21, 5), (21, 13, 6), (22, 24, 4), (11, 9, 3)):
        pts = [(cx + math.cos(a) * r * rng.uniform(0.6, 1.1), cy + math.sin(a) * r * rng.uniform(0.6, 1.1))
               for a in [i * math.pi / 2.5 + rng.uniform(-0.4, 0.4) for i in range(5)]]
        polygon(img, pts, base, 56, rng, 6)
    for _ in range(7):
        x, y = rng.randint(4, 27), rng.randint(4, 27)
        if img.getpixel((x, y))[3] > 0:
            put(img, x, y, (255, 255, 255, 255))
    return outline(img)


def shard():
    img = blank()
    rng = random.Random(8)
    polygon(img, [(8, 28), (11, 17), (21, 3), (26, 9), (17, 22)], (74, 206, 224), 60, rng, 4)
    polygon(img, [(11, 17), (21, 3), (19, 15)], (170, 246, 252), 30, rng, 3)
    polygon(img, [(17, 22), (26, 9), (22, 21)], (30, 128, 160), 20, rng, 3)
    line(img, 21, 5, 12, 20, (230, 255, 255), 1.0)
    for x, y in ((24, 6), (26, 4), (5, 14)):
        put(img, x, y, (255, 255, 255, 255))
        put(img, x + 1, y, (190, 250, 255, 255))
        put(img, x, y + 1, (190, 250, 255, 255))
    return outline(img)


def hammer():
    img = blank()
    rng = random.Random(9)
    line(img, 8, 27, 24, 11, (136, 96, 56), 3.2, rng, 5)
    polygon(img, [(3, 8), (19, 3), (25, 13), (9, 18)], (118, 118, 128), 52, rng, 6)
    polygon(img, [(3, 8), (19, 3), (20, 7), (5, 12)], (190, 190, 200), 20, rng, 3)
    line(img, 12, 16, 16, 20, (82, 60, 36), 1.5)
    line(img, 16, 20, 14, 22, (82, 60, 36), 1.5)
    return outline(img)


def tray(rim, rim_dark, inner):
    img = blank()
    rng = random.Random(rim[0] + rim[1])
    ellipse(img, 16, 18, 14, 9, rim_dark, 20, rng, 4)
    ellipse(img, 16, 16.5, 14, 8.5, rim, 40, rng, 5)
    ellipse(img, 16, 17.5, 10.5, 5.8, tone(inner, -34), 0, rng, 3)
    ellipse(img, 16, 18.2, 9.6, 5.0, inner, 18, rng, 7)
    speckle(img, rng, tone(inner, 30), 14)
    speckle(img, rng, tone(inner, -30), 14)
    line(img, 6, 13, 12, 10, tone(rim, 60), 1.0)
    return outline(img)


def bucket(body, body_dark, fill=None):
    img = blank()
    rng = random.Random(body[0])
    polygon(img, [(6, 9), (26, 9), (23, 28), (9, 28)], body, 44, rng, 5)
    for yy, amt in ((13, -24), (22, -24)):
        for x in range(4, 28):
            p = img.getpixel((x, yy))
            if p[3] > 0:
                put(img, x, yy, tone(p, amt))
    ellipse(img, 16, 9, 10, 3, tone(body, 18), 14, rng, 3)
    if fill:
        ellipse(img, 16, 9.3, 8.4, 2.1, fill, 22, rng, 8)
    else:
        ellipse(img, 16, 9.3, 8.4, 2.1, (46, 36, 32), 8, rng, 3)
    line(img, 7, 8, 5, 3, (96, 90, 84), 1.4)
    line(img, 5, 3, 27, 3, (96, 90, 84), 1.4)
    line(img, 27, 3, 25, 8, (96, 90, 84), 1.4)
    return outline(img)
