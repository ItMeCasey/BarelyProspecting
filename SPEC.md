# Barely Prospecting: design spec

**Goal:** a skyblock mod that gives the "feel" of Ex Deorum (scarcity, then a hands-on chain where you actively work blocks: sieve, combine, break, sieve again) with completely original mechanics, names, textures and drop tables. Do not reuse Ex Deorum / Ex Nihilo names or mechanics (no sieve, crook, mesh, compressed blocks, witch water, silk worms), recipes, textures or drop tables.

**Targets:** Forge and NeoForge on Minecraft 26.3 first (Forge 26.3 is the reference build), then 26.2, 26.1.2, 1.21.1, 1.20.1 (ten builds, same layout as BarelyGravestone: per-version, per-loader Gradle modules plus shared `common/`). MIT license. Mod id `barelyprospecting`.

## Principles (must hold)
- Every step is a hands-on action on a block (hold-use, swing, right-click, feed). No machine runs on its own in this mod.
- Every output feeds another manual step: refine the same material repeatedly (silt, then grit, then finer grit).
- Progress is visible: blocks change as you work them (progress ring while holding use on a block).
- Skyblock-first: the first 10 minutes work with one tree and a small island; no step needs something you cannot yet make. Every vanilla resource has a renewable source. Multiplayer-friendly (shared basins/crucibles, no per-player state).

## Start
A world preset (a ModJS-style "world type" in the world creation screen) is part of the mod: void world with a small dirt-and-stone island, one tree and a chest of starter items. Ship it on every version that supports presets; where it cannot exist, record it as n/a.

## Progression phases
| Phase | Manual loop | Unlocks |
|---|---|---|
| 0 Scrounge | Break leaves/dirt by hand for twigs and pebbles. Combine twigs into a **pick-hammer** and pebbles into a **panning tray**. | First tools, first "silt" |
| 1 Silt | Hold-use the tray on silt (progress ring): flint, seeds, saplings, coal dust. Crush rubble with the hammer into grit; pan grit again for finer drops. | Wood and plants, flint, coal |
| 2 Water and stone | Soak silt in a **basin** for clay. Fire clay into a bucket and crucible. Smelt rubble in the crucible for lava, stone, obsidian. | Water, lava, stone, obsidian |
| 3 Metals | **Pan tiers** (wood, copper, iron, gold, diamond) pull finer material from grit; each tier is a hand-crafted upgrade that changes the drop roll. | Copper, iron, gold, redstone, lapis |
| 4 Life | Mob drops come from a **hand-fed lure** block you feed and wait on (manual feeding, not a spawner). | String, bone, gunpowder, ender pearls |
| 5 Beyond | Obsidian builds a portal frame. Nether and End silt are new materials; panning them gives the rest. | Nether, End, diamonds, netherite |

Pan tiers are the progression gates. Names, textures, drop tables and numbers are original; balance is a first pass that Casey plays.

## Config
- Teams/shared use: basins and crucibles are shared blocks. Drop-table multiplier and a "creative-friendly" fast mode for modpack authors.

## Compatibility (optional dependencies, per the playbook)
Item and recipe viewers (BEI, REI, JEI, EMI): show panning, crushing and soaking recipes and drop tables. In-world info (SMWTI or Jade): show progress and what a block can yield. Check each version's availability with the curseforge-lookup skill and record gaps. Skyblock Resources (our generators mod) is the late game; do not depend on it.
