"""Writes the wiki's drop-tables page from common/.../DropTables.java, so the page cannot drift from the code.

python scripts/wiki_tables.py
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = (ROOT / 'common/src/main/java/com/itmecasey/barelyprospecting/core/DropTables.java').read_text(encoding='utf-8')
OUT = Path(r"G:\Other computers\My Computer\Projects\ItMeCasey\website\d-minimal\wiki\barelyprospecting\pages\drop-tables.md")
TIERS = ['wooden', 'copper', 'iron', 'gold', 'diamond']
TITLES = {'SILT': ('Silt', 0, 'Every tray'), 'GRIT': ('Grit', 0, 'Every tray'), 'FINE_GRIT': ('Fine grit', 1, 'Counts one tier higher'),
          'NETHER_SILT': ('Nether silt', 0, 'Copper tray or better'), 'END_SILT': ('End silt', 1, 'Iron tray or better, counts one tier higher'),
          'LURE_FARM': ('Lure, farm table (seeds)', 0, 'Lure'), 'LURE_NIGHT': ('Lure, night table (rotten flesh, bone, spider eye)', 0, 'Lure')}


def pretty(item):
    return item.split(':')[1].replace('_', ' ').capitalize()


def weight(row, level):
    _, w, mn, mx, tier, boost = row
    return 0.0 if level < tier else w * (1 + boost * (level - tier))


parts = ['---', 'title: Drop tables', 'summary: What every material and the lure can turn up, by tray tier.', '---',
         'These tables are generated from the mod\'s own rules. The percentages are the chance that one roll finds the item; a pan rolls once (wooden, copper), twice (iron, gold) or three times (diamond). "Nothing" rows are what makes a pan come up empty. See [[panning]] for how tiers work.', '']
for const, (title, shift, note) in TITLES.items():
    m = re.search(r'TABLES\.put\(' + const + r', List\.of\((.*?)\)\);\n', SRC, re.S)
    rows = []
    for line in m.group(1).split('\n'):
        line = line.strip().rstrip(',')
        if line.startswith('none('):
            rows.append(('', int(re.search(r'\d+', line).group()), 0, 0, 0, 0.0))
            continue
        mm = re.match(r'(mine|vanilla)\("(\w+)", (\d+), (\d+), (\d+), (\d+), ([\d.]+)\)', line.rstrip(')') + ')')
        if mm:
            ns = 'barelyprospecting' if mm.group(1) == 'mine' else 'minecraft'
            rows.append((f'{ns}:{mm.group(2)}', int(mm.group(3)), int(mm.group(4)), int(mm.group(5)), int(mm.group(6)), float(mm.group(7))))
    parts.append(f'## {title}')
    parts.append('')
    parts.append(f'{note}.')
    parts.append('')
    lure = const.startswith('LURE')
    if lure:
        parts.append('| Item | Count | Chance per roll (three rolls per hatch) |')
        parts.append('|---|---|---|')
        total = sum(weight(r, 0) for r in rows)
        for r in rows:
            name = 'Nothing' if not r[0] else pretty(r[0])
            parts.append(f'| {name} | {r[2]}{"-" + str(r[3]) if r[3] != r[2] else ""} | {weight(r, 0) / total * 100:.1f}% |')
    else:
        parts.append('| Item | Count | ' + ' | '.join(t.capitalize() for t in TIERS) + ' |')
        parts.append('|---|---|' + '---|' * len(TIERS))
        for r in rows:
            cells = []
            for t in range(5):
                lvl = t + shift
                total = sum(weight(x, lvl) for x in rows)
                w = weight(r, lvl)
                cells.append('-' if w == 0 else f'{w / total * 100:.1f}%')
            name = 'Nothing' if not r[0] else pretty(r[0])
            count = '' if not r[0] else f'{r[2]}-{r[3]}' if r[3] != r[2] else str(r[2])
            parts.append(f'| {name} | {count} | ' + ' | '.join(cells) + ' |')
    parts.append('')
OUT.write_text('\n'.join(parts) + '\n', encoding='utf-8', newline='\n')
print('wrote', OUT)
