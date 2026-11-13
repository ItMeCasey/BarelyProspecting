# Barely Prospecting: plan and API

The design is in `SPEC.md`. This note is the build plan and the shape of the code, so a port knows what to copy.

## Layout

- `common/src/main/java/.../core`: Minecraft-free rules, unit tested (`CoreTest`). Drop tables, hammer rules, soak recipes, the
  three state machines (`BasinState`, `CrucibleState`, `LureState`), tiers, the progress ring math, settings.
- `version-<mc>/src/main/java`: the game code per Minecraft version (blocks, block entities, items, events), shared by Forge and
  NeoForge of that version. `version-<mc>/src/main/resources`: item definitions (26.x) and the data pack (recipes, loot tables,
  tags, world preset).
- `<loader>-<mc>`: build files, the entry point, the config class, the dev hooks (`src/dev`, only with `-PdevTools`).
- `scripts/make_assets.py`: draws every texture and writes all models, block states, the language file and every version's
  data pack. Run it, never edit the generated JSON by hand.

## The loop (every step is a hand on a block)

| Phase | Step | Code |
|---|---|---|
| 0 | Break leaves or dirt bare-handed: twig, pebble | `HandDrops` (loader forwards the break event) |
| 0 | Craft pick-hammer, wooden panning tray, turf (twigs and pebbles to dirt) | recipes |
| 0/1 | Hammer: dirt to silt, stone to rubble, rubble to grit, grit to fine grit, fine grit to sand; crafting 2x2 rubble gives cobblestone and 2x2 grit gives gravel | `HammerItem`, `Recipes.crush` |
| 1 | Hold the tray on silt, grit or fine grit; a ring of sparks closes; the table is rolled | `TrayItem`, `Pan`, `DropTables` |
| 2 | Basin: filled by clay bucket or rain, one item soaks, an empty hand takes the result (silt to clay) | `BasinBlock`, `BasinState` |
| 2 | Clay to unfired bucket and crucible, fired on a campfire or in a furnace | recipes (campfire cooking) |
| 2 | Crucible: feed fuel and rubble by hand, take lava with a clay bucket, quench with water for stone and obsidian | `CrucibleBlock`, `CrucibleState` |
| 3 | Five trays (wood, copper, iron, gold, diamond); each is the last one plus four flakes of what it found | recipes, `Tier`, row gates in `DropTables` |
| 4 | Lure: three meals of one bait, then a wait; seeds call the farm table, flesh and bone the night table | `LureBlock`, `LureState` |
| 5 | Obsidian frame (vanilla portal); fine grit soaked in lava in the Nether gives Nether silt; Nether silt soaked in the End gives End silt; hammer on netherrack and end stone does the same | `Recipes.soak`, `Recipes.crush` |
| start | Void-island world preset: flat world with one air layer, an island, one oak and a chest built on first start | `StarterIsland`, `world_preset/void_island.json` |

## Rules that carry the balance

- A pannable block wears four stages (visible in its texture) and is used up on the fourth pan (`panUsesPerBlock` config, 1 to 4).
- A tray tier gates rows (`DropEntry.minTier`) and adds weight above it (`boostPerTier`). Fine grit counts as one tier higher.
  Nether silt needs at least the copper tray, End silt at least the iron tray (`DropTables.minimumTier`).
- Every vanilla resource has a renewable source: dirt from turf, water from rain, a basin and two clay-bucket sources, lava and
  stone from rubble, metals from flakes, mob drops from the lure.
- Basins, crucibles and lures keep all state in the block entity, so everyone who uses one shares it. Nothing is per player.
- Config (`barelyprospecting-common.toml`): `dropMultiplier`, `fastMode` (all waits a quarter as long), `panUsesPerBlock`,
  `panTicks`, `rainFillsBasins`, `buildStarterIsland`.

## API notes

- `core.Settings.install(Source)`: the loader installs its config.
- `core.DropTables.roll(table, tierLevel, rolls, Dice)`: returns `List<Drop>`; `chance(...)` serves the recipe viewers.
- `core.Recipes`: `crushes()`, `soaks()`, `melts()`, `fuels()`, `baits()` for the viewer integrations.
- `Pan.tick(...)` is called from `TrayItem.onUseTick`; `Pan.debugFinish` is the test hook.

## Compatibility plan

Recipe viewers (BEI, REI, JEI, EMI) get categories for panning, hammering, soaking, melting and the lure; SMWTI and Jade show
basin, crucible and lure state and what a pannable block can yield. Each is its own package that loads only when the other mod is
present. Which builds exist per version is tracked in `PROJECT_STATUS.md` and `docs/known-gaps.md`.
