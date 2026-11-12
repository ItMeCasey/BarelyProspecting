"""Pixel-art drawing helpers and every texture of the mod, at 16x16 like the game's own (original art, drawn with code, nothing from Minecraft).

The style follows vanilla: a few flat colors per material (no gradients, no smooth noise), light from the top left with a highlight,
a body tone and a shadow tone placed by hand or by hard thresholds, a one pixel outline in a darker shade of the material for items,
and blocks that are a small palette scattered in clusters.
"""
import math
import random
from PIL import Image

N = 16


def rgb(c):
    return tuple(c[:3]) + (255,)


def darker(c, f):
    return tuple(int(v * f) for v in c[:3]) + (255,)


def blank():
    return Image.new('RGBA', (N, N), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < N and 0 <= y < N:
        img.putpixel((x, y), rgb(color))


def sprite(rows, palette):
    """An item from text rows; '.' is empty, every other character looks up the palette."""
    img = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in palette:
                put(img, x, y, palette[ch])
    return img


def outline(img, factor=0.42):
    """One pixel outline around the drawing, in a dark shade of the pixel it touches (vanilla items do the same)."""
    src = img.copy()
    for y in range(N):
        for x in range(N):
            if src.getpixel((x, y))[3] == 0:
                best = None
                for dx, dy in ((-1, 0), (0, -1), (1, 0), (0, 1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < N and 0 <= ny < N:
                        p = src.getpixel((nx, ny))
                        if p[3] > 0:
                            best = p
                            if dx + dy < 0:
                                break
                if best is not None:
                    put(img, x, y, darker(best, factor))
    return img


def band(lit, pal, hi_at=0.35, lo_at=-0.35):
    """Three flat tones from a light value (positive is toward the top left)."""
    if lit > hi_at:
        return pal[0]
    if lit < lo_at:
        return pal[2]
    return pal[1]


def blob(img, cx, cy, rx, ry, pal, hi_at=0.35, lo_at=-0.3):
    """A rounded shape in three flat tones, lit from the top left."""
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            if dx * dx + dy * dy <= 1.0:
                put(img, x, y, band((-dx - dy) * 0.5, pal, hi_at, lo_at))


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


def poly(img, pts, pal, hi_at=0.3, lo_at=-0.3, axis=(-1, -1)):
    """A polygon in three flat tones, banded across the given direction."""
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    ext = max(max(xs) - min(xs), max(ys) - min(ys)) / 2 or 1
    for y in range(N):
        for x in range(N):
            if inside(pts, x + 0.5, y + 0.5):
                lit = ((x + 0.5 - cx) * axis[0] + (y + 0.5 - cy) * axis[1]) / (ext * 1.4)
                put(img, x, y, band(lit, pal, hi_at, lo_at))


def steps(img, p0, p1, color_a, color_b=None):
    """A one pixel wide line, optionally alternating two colors."""
    (x0, y0), (x1, y1) = p0, p1
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        t = i / n if n else 0
        x, y = round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t)
        put(img, x, y, color_b if (color_b and i % 2) else color_a)


# ---------------------------------------------------------------- block textures
def scatter(palette, weights, seed, cluster=0.35):
    """A block texture from a small palette: each pixel takes a color by weight, or copies its left or upper neighbor so the colors form small clumps."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (N, N))
    grid = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            if rng.random() < cluster:
                opts = []
                if x > 0:
                    opts.append(grid[y][x - 1])
                if y > 0:
                    opts.append(grid[y - 1][x])
                if opts:
                    grid[y][x] = rng.choice(opts)
                    continue
            grid[y][x] = rng.choices(palette, weights)[0]
    # wrap the edges so the texture tiles: the first row and column copy the last
    for i in range(N):
        if rng.random() < 0.5:
            grid[i][0] = grid[i][N - 1]
        if rng.random() < 0.5:
            grid[0][i] = grid[N - 1][i]
    for y in range(N):
        for x in range(N):
            put(img, x, y, grid[y][x])
    return img


DENTS = [(4, 3), (11, 5), (7, 9), (3, 12), (12, 11), (9, 2), (5, 7), (13, 14), (1, 6), (10, 8)]


def wear_dents(img, count, dark, rim):
    """Worn pans: a few shallow dents, each a dark pixel pair with a lit pixel on its lower right, more of them at every stage."""
    for i in range(count):
        x, y = DENTS[i]
        put(img, x, y, dark)
        put(img, x + 1, y, dark)
        put(img, x, y + 1, dark)
        put(img, x + 1, y + 1, rim)
    return img


def pannable(pal, speck, seed, weights=(2, 4, 3, 1)):
    """The four wear stages of one pannable block: the same base, 0, 3, 6 and 9 dents."""
    out = []
    for wear in range(4):
        img = scatter(pal, list(weights), seed)
        rng = random.Random(seed + 1)
        for _ in range(7):
            put(img, rng.randint(0, N - 1), rng.randint(0, N - 1), speck[rng.randint(0, len(speck) - 1)])
        wear_dents(img, (0, 3, 6, 9)[wear], darker(pal[3], 0.62), tuple(min(255, v + 34) for v in pal[0]))
        out.append(img)
    return out


def rubble():
    """Cobble-like chunks: a few Voronoi cells in three greys, dark mortar between, a lit top left edge on each."""
    rng = random.Random(33)
    seeds = [(rng.uniform(0, N), rng.uniform(0, N)) for _ in range(9)]
    greys = [(124, 124, 130), (108, 108, 116), (140, 140, 146)]
    tones = [rng.choice(greys) for _ in seeds]
    mortar = (64, 62, 70)
    cell = [[0] * N for _ in range(N)]
    gap = [[False] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            ds = []
            for i, (sx, sy) in enumerate(seeds):
                best = 1e9
                for ox in (-N, 0, N):
                    for oy in (-N, 0, N):
                        best = min(best, math.hypot(x + 0.5 - sx - ox, y + 0.5 - sy - oy))
                ds.append((best, i))
            ds.sort()
            cell[y][x] = ds[0][1]
            gap[y][x] = ds[1][0] - ds[0][0] < 1.1
    img = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            if gap[y][x]:
                put(img, x, y, mortar)
                continue
            c = tones[cell[y][x]]
            up, left = cell[(y - 1) % N][x], cell[y][(x - 1) % N]
            if gap[(y - 1) % N][x] or gap[y][(x - 1) % N] or up != cell[y][x] or left != cell[y][x]:
                c = tuple(min(255, v + 22) for v in c)
            put(img, x, y, c)
    return img


def planks(pal, seed):
    """Four horizontal boards, each four pixels high: a seam on the bottom row, a lit top row, grain streaks and a joint."""
    hi, mid, lo, seam = pal
    rng = random.Random(seed)
    img = Image.new('RGBA', (N, N))
    for b in range(4):
        joint = rng.randint(2, 13)
        for y in range(4):
            for x in range(N):
                if y == 3:
                    c = seam
                elif y == 0:
                    c = hi if rng.random() < 0.7 else mid
                elif y == 1:
                    c = mid if rng.random() < 0.8 else hi
                else:
                    c = mid if rng.random() < 0.55 else lo
                put(img, x, b * 4 + y, c)
        for _ in range(3):
            sx, sy = rng.randint(0, N - 4), b * 4 + rng.randint(0, 2)
            for k in range(rng.randint(2, 4)):
                put(img, sx + k, sy, lo)
        for y in range(3):
            put(img, joint, b * 4 + y, seam)
    return img


def water():
    rng = random.Random(8)
    pal = [(60, 116, 210), (50, 100, 196), (42, 86, 178)]
    img = scatter(pal, [3, 4, 2], 8, 0.5)
    for _ in range(5):
        x, y = rng.randint(0, N - 4), rng.randint(0, N - 1)
        for k in range(rng.randint(2, 3)):
            put(img, x + k, y, (110, 168, 242))
    return img


def lava(seed):
    pal = [(252, 178, 48), (240, 132, 24), (218, 92, 16), (176, 56, 10)]
    img = scatter(pal, [2, 4, 3, 1], seed, 0.55)
    rng = random.Random(seed)
    for _ in range(4):
        put(img, rng.randint(0, N - 1), rng.randint(0, N - 1), (255, 224, 110))
    return img


def fire():
    """A flame texture: bright yellow near the bottom, orange and red higher up, set by dithering."""
    rng = random.Random(14)
    img = Image.new('RGBA', (N, N))
    cols = [(255, 232, 120), (250, 190, 44), (236, 128, 22), (190, 66, 12)]
    for y in range(N):
        for x in range(N):
            k = (y / (N - 1)) * 3.0 + rng.uniform(-0.9, 0.9)
            idx = 3 - max(0, min(3, int(round(k))))
            put(img, x, y, cols[idx])
    return img


def clay_side(pal, seed):
    """A pot wall: flat clay with soft speckle, a dark band near the top and the bottom with a lit line above it."""
    hi, mid, lo, band_c = pal
    rng = random.Random(seed)
    img = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            r = rng.random()
            put(img, x, y, hi if r < 0.12 else (lo if r < 0.3 else mid))
    for y, c in ((3, band_c), (4, lo), (11, band_c), (12, lo)):
        for x in range(N):
            put(img, x, y, c)
    for y in (2, 10):
        for x in range(N):
            put(img, x, y, hi)
    return img


def soot():
    pal = [(60, 52, 50), (48, 41, 40), (36, 30, 30)]
    return scatter(pal, [3, 4, 3], 12, 0.4)


def lure_top(meals):
    """The lure from above: a woven rim of twigs, a dark hollow, and a pile of seeds that grows with every meal."""
    rng = random.Random(40)
    img = Image.new('RGBA', (N, N))
    rim_a, rim_b, rim_lo = (150, 108, 62), (122, 86, 48), (88, 60, 34)
    for y in range(N):
        for x in range(N):
            put(img, x, y, rim_a if (x + y) % 2 == 0 else rim_b)
            if rng.random() < 0.12:
                put(img, x, y, rim_lo)
    for y in range(3, 13):
        for x in range(3, 13):
            dx, dy = x - 7.5, y - 7.5
            if dx * dx + dy * dy <= 22:
                put(img, x, y, (54, 38, 24) if (dx + dy) < 1 else (40, 28, 18))
    spots = [(6, 8), (8, 9), (9, 7), (7, 6), (6, 5), (9, 10), (5, 9), (10, 8), (7, 10)]
    gold, pale = (236, 212, 124), (200, 172, 88)
    for i in range(meals * 3):
        x, y = spots[i]
        put(img, x, y, gold)
        put(img, x + 1, y, pale)
    return img


# ---------------------------------------------------------------- item sprites
STONE = [(176, 176, 184), (140, 140, 150), (104, 104, 116)]
DIRT_OUT = 0.42


def twig():
    img = blank()
    hi, mid, lo = (176, 130, 76), (136, 96, 54), (100, 68, 38)
    steps(img, (2, 14), (11, 5), mid, hi)
    steps(img, (5, 11), (5, 8), mid, lo)
    steps(img, (8, 8), (11, 8), mid, lo)
    leaf_hi, leaf, leaf_lo = (136, 204, 84), (84, 152, 52), (54, 106, 38)
    for x, y, c in ((4, 7, leaf), (5, 7, leaf_hi), (4, 6, leaf_lo), (5, 6, leaf),
                    (12, 8, leaf), (12, 7, leaf_hi), (13, 8, leaf_lo), (12, 9, leaf_lo),
                    (12, 4, leaf), (13, 3, leaf_hi), (12, 3, leaf_hi), (13, 4, leaf_lo), (14, 3, leaf), (13, 2, leaf_lo)):
        put(img, x, y, c)
    return outline(img)


def pebble():
    img = blank()
    blob(img, 5.8, 10.2, 4.2, 3.4, STONE)
    blob(img, 11.2, 8.0, 3.2, 2.8, [(160, 160, 170), (126, 126, 138), (94, 94, 108)])
    blob(img, 10.6, 12.2, 2.9, 2.2, [(188, 188, 194), (150, 150, 158), (112, 112, 122)])
    put(img, 4, 8, (214, 214, 220))
    put(img, 10, 6, (200, 200, 208))
    return outline(img)


def dust(pal):
    code = {'h': pal[0], 'm': pal[1], 'l': pal[2]}
    rows = ['................',
            '................',
            '................',
            '................',
            '.......hh.......',
            '.....hhhmm......',
            '....hhmmmmmm....',
            '...hhmmmlmmmm...',
            '..hhmmmmmmmmml..',
            '..hmmlmmmmmlmml.',
            '...lmmmmlmmmmll.',
            '....llllllllll..',
            '................',
            '................',
            '................',
            '................']
    img = sprite(rows, code)
    for x, y in ((6, 6), (9, 8), (5, 9), (11, 9)):
        put(img, x, y, (132, 132, 146))
    return outline(img)


def flakes(pal):
    hi, mid, lo = pal
    img = blank()
    shapes = [
        (3, 9, ['hh.', 'hmm', '.ml']),
        (8, 4, ['.hhh', 'hmmm', 'mmml', '.ll.']),
        (9, 10, ['.hh.', 'hmml', '.mll']),
        (4, 4, ['hm', 'ml']),
    ]
    code = {'h': hi, 'm': mid, 'l': lo}
    for ox, oy, rows in shapes:
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch in code:
                    put(img, ox + dx, oy + dy, code[ch])
    for x, y in ((9, 5), (4, 10), (10, 11)):
        put(img, x, y, (255, 255, 240))
    return outline(img)


def shard():
    img = blank()
    poly(img, [(2.5, 14), (4, 8.5), (10.5, 1.5), (14.5, 4.5), (8.5, 12.5)], [(120, 232, 240), (64, 200, 222), (34, 140, 172)], 0.25, -0.2)
    for x, y in ((6, 8), (7, 7), (8, 6), (9, 5)):
        put(img, x, y, (206, 255, 255))
    put(img, 11, 3, (230, 255, 255))
    for x, y in ((12, 5), (11, 6), (10, 7)):
        put(img, x, y, (34, 140, 172))
    return outline(img)


def hammer():
    img = blank()
    wood_hi, wood, wood_lo = (176, 130, 76), (136, 96, 54), (98, 66, 36)
    steps(img, (2, 14), (10, 6), wood, wood_lo)
    steps(img, (3, 13), (11, 5), wood_hi, wood)
    # head: a blunt face on the lower right, a pointed pick on the upper left
    poly(img, [(5.5, 2.5), (8, 1.5), (14, 7.5), (13.5, 9.5), (11, 9)], [(196, 196, 206), (150, 150, 162), (104, 104, 118)], 0.2, -0.25, axis=(-1, 1))
    for x, y in ((6, 3), (7, 3)):
        put(img, x, y, (226, 226, 234))
    put(img, 12, 8, (82, 82, 96))
    put(img, 13, 8, (82, 82, 96))
    return outline(img)


def tray(rim, inner, seed):
    """A shallow pan seen from above and a little from the front: lit rim, dark bowl with grit, a shadow lip underneath."""
    hi, mid, lo = rim
    img = blank()
    blob(img, 8, 10, 7, 4.2, [lo, lo, lo], 2, -2)
    blob(img, 8, 8.7, 7, 4.2, rim, 0.3, -0.25)
    blob(img, 8.2, 8.9, 4.9, 2.6, [inner[1], inner[1], inner[2]], 2, -0.2)
    rng = random.Random(seed)
    for _ in range(6):
        x, y = rng.randint(5, 11), rng.randint(7, 10)
        if inside([(3, 9), (8, 5), (13, 9), (8, 12)], x + 0.5, y + 0.5):
            put(img, x, y, inner[0] if rng.random() < 0.5 else inner[2])
    return outline(img)


def bucket(body, fill=None):
    hi, mid, lo = body
    img = blank()
    for y in range(4, 14):
        left = 3 + (y - 4) * 0.2
        right = 12.99 - (y - 4) * 0.2
        for x in range(16):
            if left <= x + 0.5 <= right + 0.5:
                t = (x + 0.5 - left) / max(0.1, right - left)
                c = hi if t < 0.22 else (lo if t > 0.78 else mid)
                if y in (8, 12):
                    c = lo if t > 0.22 else mid
                put(img, x, y, c)
    for x in range(3, 13):
        put(img, x, 4, hi if x < 8 else mid)
    inner_dark = (62, 42, 36)
    top = (inner_dark, darker(inner_dark, 0.75))
    for x in range(4, 12):
        put(img, x, 5, fill[0] if fill else top[0])
    for x in range(5, 11):
        put(img, x, 6, fill[1] if fill else top[1])
    outline(img)
    wire = (92, 72, 58)
    for x, y in ((4, 3), (4, 2), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 2), (11, 3)):
        put(img, x, y, wire)
    return img
