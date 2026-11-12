"""Draws the mod's textures (original pixel art, nothing from Minecraft or other mods) and writes the models, block states, language
file, recipes, loot tables, tags and the world preset for every version.

Run from the project folder:  python scripts/make_assets.py
Needs Pillow. Writes into common/src/main/resources (textures, models, block states, language) and into
version-<mc>/src/main/resources (item definitions for 26.x, and the data pack: recipes, loot tables, tags, world preset).
"""
import json
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
NS = 'barelyprospecting'
COMMON = os.path.join(ROOT, 'common', 'src', 'main', 'resources', 'assets', NS)
VERSIONS = ['26.3', '26.2', '26.1.2', '1.21.1', '1.20.1']


# ---------------------------------------------------------------- helpers
def save(img, *parts):
    path = os.path.join(COMMON, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def write_json(data, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def common_json(data, *parts):
    write_json(data, os.path.join(COMMON, *parts))


def shade(color, amount):
    return tuple(max(0, min(255, int(c + amount))) for c in color[:3]) + (255,)


def noisy(base, spread, seed, speck=None, speck_every=0):
    """A 16x16 block texture: the base color with random brightness noise and optional specks."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            color = shade(base, rng.randint(-spread, spread))
            if speck and speck_every and rng.randint(0, speck_every) == 0:
                color = shade(speck, rng.randint(-10, 10))
            img.putpixel((x, y), color)
    return img


def pits(img, count, seed, dark=40):
    """Worn spots: darker pits that grow with every pan."""
    rng = random.Random(seed)
    for _ in range(count):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            r, g, b, a = img.getpixel((x + dx, y + dy))
            img.putpixel((x + dx, y + dy), shade((r, g, b), -dark))
    return img


def sprite(rows, palette):
    """An item sprite from text rows; '.' is transparent and every other character looks up the palette."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in palette:
                img.putpixel((x, y), palette[ch] + (255,) if len(palette[ch]) == 3 else palette[ch])
    return img


OUT = (28, 22, 20)

# ---------------------------------------------------------------- block textures
SILT = (126, 104, 82)
GRIT = (140, 138, 132)
FINE = (196, 188, 164)
NETHER = (96, 46, 52)
END = (206, 206, 150)
RUBBLE = (118, 116, 122)


def pannable_textures():
    for i, (name, base, spread, speck, every) in enumerate([
            ('silt', SILT, 12, (90, 74, 58), 6),
            ('grit', GRIT, 18, (96, 94, 92), 5),
            ('fine_grit', FINE, 9, (222, 214, 190), 4),
            ('nether_silt', NETHER, 14, (214, 110, 40), 9),
            ('end_silt', END, 8, (120, 76, 168), 8)]):
        for wear in range(4):
            img = noisy(base, spread, 100 + i * 10, speck, every)
            pits(img, wear * 4, 200 + i, 38)
            save(img, 'textures', 'block', f'{name}_{wear}.png')


def rubble_texture():
    rng = random.Random(33)
    img = noisy(RUBBLE, 14, 33)
    # angular chips: a few lighter and darker blocks
    for _ in range(9):
        x, y, w, h = rng.randint(0, 12), rng.randint(0, 12), rng.randint(2, 4), rng.randint(2, 3)
        tone = rng.choice([-26, 22, 12, -14])
        for yy in range(y, min(16, y + h)):
            for xx in range(x, min(16, x + w)):
                r, g, b, a = img.getpixel((xx, yy))
                img.putpixel((xx, yy), shade((r, g, b), tone))
    save(img, 'textures', 'block', 'rubble.png')


def planks(base, seed):
    img = Image.new('RGBA', (16, 16))
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            n = rng.randint(-8, 8)
            if y % 4 == 3:
                n -= 26
            img.putpixel((x, y), shade(base, n))
    return img


def basin_textures():
    save(planks((140, 98, 58), 5), 'textures', 'block', 'basin_wood.png')
    inner = planks((96, 66, 40), 6)
    save(inner, 'textures', 'block', 'basin_inner.png')
    water = noisy((58, 110, 196), 10, 8, (130, 180, 240), 9)
    save(water, 'textures', 'block', 'basin_water.png')
    lava = noisy((230, 110, 24), 22, 9, (255, 214, 80), 6)
    save(lava, 'textures', 'block', 'basin_lava.png')


def crucible_textures():
    clay = (178, 98, 62)
    side = noisy(clay, 8, 11)
    for y in (4, 5, 11):
        for x in range(16):
            r, g, b, a = side.getpixel((x, y))
            side.putpixel((x, y), shade((r, g, b), -34))
    save(side, 'textures', 'block', 'crucible_side.png')
    save(noisy((52, 44, 42), 7, 12), 'textures', 'block', 'crucible_inner.png')
    save(noisy((236, 120, 30), 24, 13, (255, 220, 90), 5), 'textures', 'block', 'crucible_lava.png')
    fire = noisy((238, 160, 40), 20, 14, (255, 240, 130), 3)
    save(fire, 'textures', 'block', 'crucible_fire.png')
    soft = noisy((200, 150, 116), 8, 15)
    for y in (4, 5, 11):
        for x in range(16):
            r, g, b, a = soft.getpixel((x, y))
            soft.putpixel((x, y), shade((r, g, b), -22))
    save(soft, 'textures', 'block', 'unfired_crucible.png')


def lure_textures():
    nest = (122, 92, 52)
    seeds = [(226, 204, 120), (210, 184, 96), (236, 220, 150)]
    for meals in range(4):
        img = noisy(nest, 14, 20)
        # a ring of twigs around a hollow
        for y in range(16):
            for x in range(16):
                d = (x - 7.5) ** 2 + (y - 7.5) ** 2
                if d < 22:
                    img.putpixel((x, y), shade((70, 52, 30), 0))
        rng = random.Random(40 + meals)
        for _ in range(meals * 5):
            x, y = rng.randint(4, 11), rng.randint(4, 11)
            img.putpixel((x, y), rng.choice(seeds) + (255,))
        save(img, 'textures', 'block', f'lure_{meals}.png')
    save(noisy(nest, 14, 21), 'textures', 'block', 'lure_side.png')


# ---------------------------------------------------------------- item textures
def twig():
    return sprite([
        '................',
        '..............g.',
        '.............gG.',
        '............b...',
        '...........b.G..',
        '..........b.....',
        '.....g...b......',
        '....gG..b.......',
        '.....b.b........',
        '......bb........',
        '.....bb.........',
        '....b.b.........',
        '...b............',
        '..d.............',
        '................',
        '................'],
        {'b': (122, 88, 52), 'd': (84, 58, 34), 'g': (96, 150, 60), 'G': (70, 116, 44)})


def pebble():
    return sprite([
        '................',
        '................',
        '................',
        '................',
        '.....ssss.......',
        '....sSSSSs......',
        '....sSSSSsss....',
        '.....ssssSSSs...',
        '..sss...sSSSs...',
        '.sSSSs...sss....',
        '.sSSSSs.........',
        '..sssss.........',
        '................',
        '................',
        '................',
        '................'],
        {'s': (96, 96, 102), 'S': (150, 150, 156)})


def dust(base, light, dark):
    return sprite([
        '................',
        '................',
        '................',
        '................',
        '................',
        '......dd........',
        '.....dllld......',
        '....dllbllld....',
        '...dllbbbblld...',
        '..dlbbbbbbblld..',
        '..dbbbbbbbbbbd..',
        '...dddddddddd...',
        '................',
        '................',
        '................',
        '................'],
        {'b': base, 'l': light, 'd': dark})


def chips(base, light, dark):
    return sprite([
        '................',
        '................',
        '................',
        '.......ll.......',
        '......lbbd......',
        '.....lbbbbd.....',
        '......dbbd..ll..',
        '.............d..',
        '..lb............',
        '.lbbd.......ll..',
        '..dd.......lbbd.',
        '............dd..',
        '................',
        '................',
        '................',
        '................'],
        {'b': base, 'l': light, 'd': dark})


def shard():
    return sprite([
        '................',
        '................',
        '.........l......',
        '........lcc.....',
        '.......lccC.....',
        '......lcccCd....',
        '.....lcccCd.....',
        '....lcccCd......',
        '...lccCCd.......',
        '..lcCCd.........',
        '..lCd...........',
        '..d.............',
        '................',
        '................',
        '................',
        '................'],
        {'c': (92, 226, 232), 'C': (44, 168, 190), 'l': (210, 255, 255), 'd': (20, 100, 124)})


def hammer():
    return sprite([
        '................',
        '................',
        '...hhhhhhh......',
        '..hHHHHHHHh.....',
        '..hHSSSSSHhd....',
        '..hHHHHHHHhwd...',
        '...hhhhhhh.wd...',
        '............wd..',
        '.............wd.',
        '..............wd',
        '...............w',
        '................',
        '................',
        '................',
        '................',
        '................'],
        {'h': (70, 70, 78), 'H': (130, 130, 140), 'S': (170, 170, 180), 'w': (130, 92, 54), 'd': (88, 62, 36)})


def tray(rim, rim_dark, inner):
    return sprite([
        '................',
        '................',
        '................',
        '................',
        '....rrrrrrrr....',
        '..rrRRRRRRRRrr..',
        '.rRRiiiiiiiiRRr.',
        '.rRiiiiiiiiiiRr.',
        '.rRiiiiiiiiiiRr.',
        '.rRiiiiiiiiiiRr.',
        '..rRRiiiiiiRRr..',
        '...rrRRRRRRrr...',
        '....dddddddd....',
        '................',
        '................',
        '................'],
        {'r': rim_dark, 'R': rim, 'i': inner, 'd': (40, 34, 32)})


def bucket(body, body_dark, fill=None, fill_dark=None):
    rows = [
        '................',
        '................',
        '....h......h....',
        '...h.hhhhhh.h...',
        '...h.fFFFFf.h...',
        '....bfFFFFfb....',
        '....bbbbbbbb....',
        '....bBbbbbBb....',
        '....bBbbbbBb....',
        '.....bBbbBb.....',
        '.....bBbbBb.....',
        '.....dddddd.....',
        '................',
        '................',
        '................',
        '................']
    palette = {'b': body, 'B': body_dark, 'd': tuple(max(0, c - 70) for c in body_dark), 'h': (90, 84, 78)}
    if fill:
        palette['f'] = fill_dark
        palette['F'] = fill
    else:
        palette['f'] = (40, 32, 30)
        palette['F'] = (58, 48, 44)
    return sprite(rows, palette)


def item_textures():
    def put(name, img):
        save(img, 'textures', 'item', name + '.png')

    put('twig', twig())
    put('pebble', pebble())
    put('coal_dust', dust((46, 46, 52), (88, 88, 98), (22, 22, 26)))
    put('copper_flake', chips((214, 126, 82), (248, 178, 130), (140, 74, 44)))
    put('iron_flake', chips((206, 204, 200), (240, 240, 236), (120, 120, 126)))
    put('gold_flake', chips((246, 196, 56), (255, 238, 140), (176, 124, 24)))
    put('diamond_shard', shard())
    put('pick_hammer', hammer())
    put('wooden_panning_tray', tray((160, 112, 66), (96, 66, 38), (112, 92, 70)))
    put('copper_panning_tray', tray((220, 128, 84), (140, 74, 44), (118, 96, 80)))
    put('iron_panning_tray', tray((214, 214, 214), (116, 116, 122), (120, 112, 104)))
    put('gold_panning_tray', tray((248, 204, 66), (176, 124, 24), (122, 102, 78)))
    put('diamond_panning_tray', tray((98, 232, 236), (36, 142, 164), (116, 108, 100)))
    put('unfired_clay_bucket', bucket((196, 160, 130), (150, 114, 90)))
    put('clay_bucket', bucket((190, 98, 64), (136, 64, 40)))
    put('clay_bucket_water', bucket((190, 98, 64), (136, 64, 40), (74, 128, 214), (48, 92, 172)))
    put('clay_bucket_lava', bucket((190, 98, 64), (136, 64, 40), (250, 140, 30), (210, 80, 20)))


# ---------------------------------------------------------------- models
def cube_all(texture):
    return {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{texture}'}}


def box(frm, to, tex, faces='all'):
    face = {'texture': f'#{tex}'}
    return {'from': frm, 'to': to, 'faces': {d: dict(face) for d in ('down', 'up', 'north', 'south', 'west', 'east')}}


def basin_models():
    walls = [
        box([0, 0, 0], [16, 2, 16], 'wood'),
        box([0, 2, 0], [16, 9, 2], 'wood'),
        box([0, 2, 14], [16, 9, 16], 'wood'),
        box([0, 2, 2], [2, 9, 14], 'wood'),
        box([14, 2, 2], [16, 9, 14], 'wood'),
    ]
    inner_floor = box([2, 2, 2], [14, 2.01, 14], 'inner')
    base = {'textures': {'wood': f'{NS}:block/basin_wood', 'inner': f'{NS}:block/basin_inner', 'particle': f'{NS}:block/basin_wood'},
            'elements': walls + [inner_floor]}
    common_json(base, 'models', 'block', 'basin.json')
    for kind in ('water', 'lava'):
        for level in range(1, 5):
            top = [3, 5, 7, 8][level - 1]
            part = {'textures': {'fluid': f'{NS}:block/basin_{kind}', 'particle': f'{NS}:block/basin_{kind}'},
                    'elements': [{'from': [2, 2, 2], 'to': [14, top, 14], 'faces': {'up': {'texture': '#fluid'}}}]}
            common_json(part, 'models', 'block', f'basin_{kind}_{level}.json')
    state = {'multipart': [{'apply': {'model': f'{NS}:block/basin'}}]}
    for kind in ('water', 'lava'):
        for level in range(1, 5):
            state['multipart'].append({'when': {kind: str(level)}, 'apply': {'model': f'{NS}:block/basin_{kind}_{level}'}})
    common_json(state, 'blockstates', 'basin.json')
    common_json({'parent': f'{NS}:block/basin'}, 'models', 'item', 'basin.json')


def pot_elements(tex):
    return [
        box([2, 0, 2], [14, 2, 14], tex),
        box([2, 2, 2], [14, 13, 4], tex),
        box([2, 2, 12], [14, 13, 14], tex),
        box([2, 2, 4], [4, 13, 12], tex),
        box([12, 2, 4], [14, 13, 12], tex),
        box([4, 2, 4], [12, 2.01, 12], 'inner'),
    ]


def crucible_models():
    base = {'textures': {'side': f'{NS}:block/crucible_side', 'inner': f'{NS}:block/crucible_inner', 'particle': f'{NS}:block/crucible_side'},
            'elements': pot_elements('side')}
    common_json(base, 'models', 'block', 'crucible.json')
    for level in range(1, 4):
        top = [5, 8, 11][level - 1]
        common_json({'textures': {'lava': f'{NS}:block/crucible_lava', 'particle': f'{NS}:block/crucible_lava'},
                     'elements': [{'from': [4, 2, 4], 'to': [12, top, 12], 'faces': {'up': {'texture': '#lava'}}}]},
                    'models', 'block', f'crucible_lava_{level}.json')
    common_json({'textures': {'fire': f'{NS}:block/crucible_fire', 'particle': f'{NS}:block/crucible_fire'},
                 'elements': [{'from': [5, 2, 5], 'to': [11, 3.5, 11], 'faces': {d: {'texture': '#fire'} for d in ('up', 'north', 'south', 'west', 'east')}}]},
                'models', 'block', 'crucible_fire.json')
    state = {'multipart': [{'apply': {'model': f'{NS}:block/crucible'}},
                           {'when': {'lit': 'true', 'lava': '0'}, 'apply': {'model': f'{NS}:block/crucible_fire'}}]}
    for level in range(1, 4):
        state['multipart'].append({'when': {'lava': str(level)}, 'apply': {'model': f'{NS}:block/crucible_lava_{level}'}})
    common_json(state, 'blockstates', 'crucible.json')
    common_json({'parent': f'{NS}:block/crucible'}, 'models', 'item', 'crucible.json')
    # the unfired pot: same shape, soft clay
    unfired = {'textures': {'side': f'{NS}:block/unfired_crucible', 'inner': f'{NS}:block/crucible_inner', 'particle': f'{NS}:block/unfired_crucible'},
               'elements': pot_elements('side')}
    common_json(unfired, 'models', 'block', 'unfired_crucible.json')
    common_json({'variants': {'': {'model': f'{NS}:block/unfired_crucible'}}}, 'blockstates', 'unfired_crucible.json')
    common_json({'parent': f'{NS}:block/unfired_crucible'}, 'models', 'item', 'unfired_crucible.json')


def lure_models():
    state = {'variants': {}}
    for meals in range(4):
        model = {'textures': {'top': f'{NS}:block/lure_{meals}', 'side': f'{NS}:block/lure_side', 'particle': f'{NS}:block/lure_side'},
                 'elements': [{'from': [2, 0, 2], 'to': [14, 7, 14],
                               'faces': {'up': {'texture': '#top'}, 'down': {'texture': '#side'}, 'north': {'texture': '#side'},
                                         'south': {'texture': '#side'}, 'west': {'texture': '#side'}, 'east': {'texture': '#side'}}}]}
        common_json(model, 'models', 'block', f'lure_{meals}.json')
        state['variants'][f'meals={meals}'] = {'model': f'{NS}:block/lure_{meals}'}
    common_json(state, 'blockstates', 'lure.json')
    common_json({'parent': f'{NS}:block/lure_0'}, 'models', 'item', 'lure.json')


def simple_block_models():
    for name in ('silt', 'grit', 'fine_grit', 'nether_silt', 'end_silt'):
        state = {'variants': {}}
        for wear in range(4):
            common_json(cube_all(f'{name}_{wear}'), 'models', 'block', f'{name}_{wear}.json')
            state['variants'][f'wear={wear}'] = {'model': f'{NS}:block/{name}_{wear}'}
        common_json(state, 'blockstates', f'{name}.json')
        common_json({'parent': f'{NS}:block/{name}_0'}, 'models', 'item', f'{name}.json')
    common_json(cube_all('rubble'), 'models', 'block', 'rubble.json')
    common_json({'variants': {'': {'model': f'{NS}:block/rubble'}}}, 'blockstates', 'rubble.json')
    common_json({'parent': f'{NS}:block/rubble'}, 'models', 'item', 'rubble.json')


FLAT_ITEMS = ['twig', 'pebble', 'coal_dust', 'copper_flake', 'iron_flake', 'gold_flake', 'diamond_shard', 'pick_hammer',
              'wooden_panning_tray', 'copper_panning_tray', 'iron_panning_tray', 'gold_panning_tray', 'diamond_panning_tray',
              'unfired_clay_bucket', 'clay_bucket', 'clay_bucket_water', 'clay_bucket_lava']
BLOCK_ITEMS = ['silt', 'rubble', 'grit', 'fine_grit', 'nether_silt', 'end_silt', 'basin', 'unfired_crucible', 'crucible', 'lure']
BLOCKS = BLOCK_ITEMS


def item_models():
    for name in FLAT_ITEMS:
        parent = 'minecraft:item/handheld' if name in ('pick_hammer',) else 'minecraft:item/generated'
        common_json({'parent': parent, 'textures': {'layer0': f'{NS}:item/{name}'}}, 'models', 'item', f'{name}.json')


# ---------------------------------------------------------------- language
LANG = {
    'itemGroup.barelyprospecting': 'Barely Prospecting',
    'item.barelyprospecting.twig': 'Twig',
    'item.barelyprospecting.pebble': 'Pebble',
    'item.barelyprospecting.coal_dust': 'Coal Dust',
    'item.barelyprospecting.copper_flake': 'Copper Flake',
    'item.barelyprospecting.iron_flake': 'Iron Flake',
    'item.barelyprospecting.gold_flake': 'Gold Flake',
    'item.barelyprospecting.diamond_shard': 'Diamond Shard',
    'item.barelyprospecting.pick_hammer': 'Pick-Hammer',
    'item.barelyprospecting.wooden_panning_tray': 'Wooden Panning Tray',
    'item.barelyprospecting.copper_panning_tray': 'Copper Panning Tray',
    'item.barelyprospecting.iron_panning_tray': 'Iron Panning Tray',
    'item.barelyprospecting.gold_panning_tray': 'Gold Panning Tray',
    'item.barelyprospecting.diamond_panning_tray': 'Diamond Panning Tray',
    'item.barelyprospecting.unfired_clay_bucket': 'Unfired Clay Bucket',
    'item.barelyprospecting.clay_bucket': 'Clay Bucket',
    'item.barelyprospecting.clay_bucket_water': 'Clay Bucket of Water',
    'item.barelyprospecting.clay_bucket_lava': 'Clay Bucket of Lava',
    'block.barelyprospecting.silt': 'Silt',
    'block.barelyprospecting.rubble': 'Rubble',
    'block.barelyprospecting.grit': 'Grit',
    'block.barelyprospecting.fine_grit': 'Fine Grit',
    'block.barelyprospecting.nether_silt': 'Nether Silt',
    'block.barelyprospecting.end_silt': 'End Silt',
    'block.barelyprospecting.basin': 'Soaking Basin',
    'block.barelyprospecting.unfired_crucible': 'Unfired Crucible',
    'block.barelyprospecting.crucible': 'Crucible',
    'block.barelyprospecting.lure': 'Lure',
    'generator.barelyprospecting.void_island': 'Void Island',
    'fluid.barelyprospecting.none': 'empty',
    'fluid.barelyprospecting.water': 'water',
    'fluid.barelyprospecting.lava': 'lava',
    'message.barelyprospecting.tray_too_weak': 'Your tray is too weak to work this. Upgrade it.',
    'message.barelyprospecting.pan_nothing': 'Nothing but dust this time.',
    'message.barelyprospecting.basin_dry': 'The basin is dry. Fill it with water, or lava for the right recipes.',
    'message.barelyprospecting.basin_busy': 'The basin is busy. Take the result or wait.',
    'message.barelyprospecting.basin_wrong': 'That does not soak in this fluid or in this dimension.',
    'message.barelyprospecting.basin_needs_full': 'The basin must be full to fill a bucket.',
    'message.barelyprospecting.basin_full': 'The basin cannot take more, or holds another fluid.',
    'message.barelyprospecting.basin_soaking': 'Soaking: %s%%',
    'message.barelyprospecting.basin_status': 'Basin: %s of %s levels of %s',
    'message.barelyprospecting.crucible_full': 'The crucible cannot take more.',
    'message.barelyprospecting.crucible_fire_full': 'The fire is already as big as it gets.',
    'message.barelyprospecting.crucible_needs_lava': 'Not enough lava for a bucket yet (%s of %s).',
    'message.barelyprospecting.crucible_nothing_to_quench': 'There is no molten lava to quench.',
    'message.barelyprospecting.crucible_status': 'Fire: %s s, rubble waiting: %s, lava: %s',
    'message.barelyprospecting.lure_hatching': 'The lure is busy. Wait for it.',
    'message.barelyprospecting.lure_other_bait': 'The lure only takes one kind of bait at a time.',
    'message.barelyprospecting.lure_waiting': 'Something is stirring: %s s',
    'message.barelyprospecting.lure_status': 'Lure: %s of %s meals',
}


def write_lang():
    common_json(LANG, 'lang', 'en_us.json')


# ---------------------------------------------------------------- data pack (per version)
class Fmt:
    def __init__(self, version):
        self.version = version
        self.new = version.startswith('26.')
        self.mid = version == '1.21.1'
        self.old = version == '1.20.1'
        self.recipes = 'recipes' if self.old else 'recipe'
        self.loot = 'loot_tables' if self.old else 'loot_table'
        self.tag_item = 'items' if self.old else 'item'
        self.tag_block = 'blocks' if self.old else 'block'

    def ing(self, value):
        """An ingredient: '#tag' or 'ns:item'."""
        if self.new:
            return value
        if value.startswith('#'):
            return {'tag': value[1:]}
        return {'item': value}

    def result(self, item, count=1):
        if self.old:
            out = {'item': item}
        else:
            out = {'id': item}
        if count != 1:
            out['count'] = count
        return out


def m(name):
    return f'{NS}:{name}'


def v(name):
    return f'minecraft:{name}'


def recipes(f):
    out = {}
    tray_names = ['wooden', 'copper', 'iron', 'gold', 'diamond']

    def shaped(name, pattern, key, result, count=1, category='misc'):
        out[name] = {'type': 'minecraft:crafting_shaped', 'category': category, 'pattern': pattern,
                     'key': {k: f.ing(val) for k, val in key.items()}, 'result': f.result(result, count)}

    def shapeless(name, ingredients, result, count=1, category='misc'):
        out[name] = {'type': 'minecraft:crafting_shapeless', 'category': category, 'ingredients': [f.ing(i) for i in ingredients],
                     'result': f.result(result, count)}

    def cooking(name, kind, ingredient, result, time, xp=0.1):
        out[name] = {'type': f'minecraft:{kind}', 'category': 'misc', 'cookingtime': time, 'experience': xp,
                     'ingredient': f.ing(ingredient), 'result': result if f.old else f.result(result)}

    shaped('pick_hammer', [' PP', ' TP', 'T  '], {'P': m('pebble'), 'T': m('twig')}, m('pick_hammer'), category='equipment')
    shaped('wooden_panning_tray', ['T T', 'PPP'], {'P': m('pebble'), 'T': m('twig')}, m('wooden_panning_tray'), category='equipment')
    shaped('turf', ['TP', 'PT'], {'P': m('pebble'), 'T': m('twig')}, v('dirt'))
    shaped('basin', ['P P', 'PPP'], {'P': '#minecraft:planks'}, m('basin'))
    shaped('unfired_clay_bucket', ['C C', 'C C', ' C '], {'C': v('clay_ball')}, m('unfired_clay_bucket'))
    shaped('unfired_crucible', ['C C', 'C C', 'CCC'], {'C': v('clay_ball')}, m('unfired_crucible'))
    cooking('clay_bucket_campfire', 'campfire_cooking', m('unfired_clay_bucket'), m('clay_bucket'), 600)
    cooking('clay_bucket_smelting', 'smelting', m('unfired_clay_bucket'), m('clay_bucket'), 200)
    cooking('crucible_campfire', 'campfire_cooking', m('unfired_crucible'), m('crucible'), 600)
    cooking('crucible_smelting', 'smelting', m('unfired_crucible'), m('crucible'), 200)
    # Pan tiers: each tray is the one before it plus four flakes of the metal the previous tier found.
    for prev, nxt, flake in (('wooden', 'copper', 'copper_flake'), ('copper', 'iron', 'iron_flake'),
                             ('iron', 'gold', 'gold_flake'), ('gold', 'diamond', 'diamond_shard')):
        shapeless(f'{nxt}_panning_tray', [m(f'{prev}_panning_tray')] + [m(flake)] * 4, m(f'{nxt}_panning_tray'), category='equipment')
    shapeless('raw_copper', [m('copper_flake')] * 4, v('raw_copper'))
    shapeless('raw_iron', [m('iron_flake')] * 4, v('raw_iron'))
    shapeless('raw_gold', [m('gold_flake')] * 4, v('raw_gold'))
    shapeless('diamond_from_shards', [m('diamond_shard')] * 4, v('diamond'))
    shaped('lure', ['TFT', 'FSF', 'TFT'], {'T': m('twig'), 'F': m('copper_flake'), 'S': v('wheat_seeds')}, m('lure'))
    shapeless('end_portal_frame', [v('ender_eye')] + [v('obsidian')] * 4, v('end_portal_frame'))
    return out


def loot_table(name):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
                                                  'entries': [{'type': 'minecraft:item', 'name': m(name)}],
                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def world_preset():
    return {
        'dimensions': {
            'minecraft:overworld': {
                'type': 'minecraft:overworld',
                'generator': {'type': 'minecraft:flat', 'settings': {
                    'biome': 'minecraft:plains', 'features': False, 'lakes': False,
                    'layers': [{'block': 'minecraft:air', 'height': 1}], 'structure_overrides': []}}},
            'minecraft:the_nether': {
                'type': 'minecraft:the_nether',
                'generator': {'type': 'minecraft:noise', 'settings': 'minecraft:nether',
                              'biome_source': {'type': 'minecraft:multi_noise', 'preset': 'minecraft:nether'}}},
            'minecraft:the_end': {
                'type': 'minecraft:the_end',
                'generator': {'type': 'minecraft:noise', 'settings': 'minecraft:end',
                              'biome_source': {'type': 'minecraft:the_end'}}}}}


def write_version(version):
    f = Fmt(version)
    res = os.path.join(ROOT, f'version-{version}', 'src', 'main', 'resources')
    data = os.path.join(res, 'data')
    for name, body in recipes(f).items():
        write_json(body, os.path.join(data, NS, f.recipes, f'{name}.json'))
    for name in BLOCKS:
        write_json(loot_table(name), os.path.join(data, NS, f.loot, 'blocks', f'{name}.json'))
    write_json({'values': [m('coal_dust')]}, os.path.join(data, 'minecraft', 'tags', f.tag_item, 'coals.json'))
    write_json({'values': [m('silt'), m('grit'), m('fine_grit'), m('nether_silt'), m('end_silt')]},
               os.path.join(data, 'minecraft', 'tags', f.tag_block, 'mineable', 'shovel.json'))
    write_json({'values': [m('rubble'), m('unfired_crucible'), m('crucible')]},
               os.path.join(data, 'minecraft', 'tags', f.tag_block, 'mineable', 'pickaxe.json'))
    write_json({'values': [m('basin'), m('lure')]},
               os.path.join(data, 'minecraft', 'tags', f.tag_block, 'mineable', 'axe.json'))
    write_json(world_preset(), os.path.join(data, NS, 'worldgen', 'world_preset', 'void_island.json'))
    write_json({'values': [m('void_island')]}, os.path.join(data, 'minecraft', 'tags', 'worldgen', 'world_preset', 'normal.json'))
    if f.new:
        for name in FLAT_ITEMS + BLOCK_ITEMS:
            write_json({'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}},
                       os.path.join(res, 'assets', NS, 'items', f'{name}.json'))


def main():
    pannable_textures()
    rubble_texture()
    basin_textures()
    crucible_textures()
    lure_textures()
    item_textures()
    simple_block_models()
    basin_models()
    crucible_models()
    lure_models()
    item_models()
    write_lang()
    for version in VERSIONS:
        write_version(version)
    print('assets written')


if __name__ == '__main__':
    main()
