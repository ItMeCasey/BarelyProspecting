# Scripting with ModJS

With ModJS installed, scripts get a `BarelyProspecting` object. ModJS is not required; without it the mod loads and plays exactly the same.

The global is available in every script type. Changes to drop tables and rules apply at once to the game. Every setter replaces an existing entry of the same key, so a script that runs twice does not double anything. A bad argument is a plain script error in the ModJS log; a listener that throws is logged as one line and skipped, and never reaches the game.

## Example

```js
// A server script. Clear listeners first so a script reload replaces the old ones.
BarelyProspecting.clearListeners()

// Make silt a little richer and let the hammer break glass.
BarelyProspecting.setDrop('silt', 'minecraft:emerald', 1, 1, 1, 0, 0.0)
BarelyProspecting.setCrush('minecraft:glass', 'minecraft:sand')

// A lure that eats sugar and answers with its own table.
BarelyProspecting.setDrop('sweet', 'minecraft:cookie', 10, 1, 3, 0, 0.0)
BarelyProspecting.setDrop('sweet', 'minecraft:honey_bottle', 2, 1, 1, 0, 0.0)
BarelyProspecting.setBait('minecraft:sugar', 'sweet')

BarelyProspecting.on('panned', e => {
  if (e.count() > 3) console.log(e.player() + ' panned ' + e.count() + ' items at ' + e.x() + ' ' + e.y() + ' ' + e.z())
})
BarelyProspecting.on('soakFinished', e => {
  const basin = BarelyProspecting.blockAt(e.dimension(), e.x(), e.y(), e.z())
  console.log('soak done: ' + basin.get('result') + ' x' + basin.get('resultCount'))
})
```

## Events

`on(name, fn)` takes one of these names. The function gets an event with the methods below; text values are empty and numbers are zero when they do not apply.

| Event | When | Fields set |
|---|---|---|
| `panned` | A panning ring closes on a block. | `player`, position, `subject` (the material block), `count` (items in total), `drops` |
| `hammered` | A hammer blow changes a block. | `player`, position, `subject` (block before), `result` (block after) |
| `soakStarted` | An item goes into a basin. | `player`, position, `subject` (item), `result` (item it will become), `count`, `detail` (water or lava) |
| `soakFinished` | A soak is done and the result waits. | position, `subject`, `result`, `count` |
| `crucibleFed` | Rubble or fuel goes into a crucible. | `player`, position, `subject` (item), `count` (lava units or burn ticks), `detail` (melt or fuel) |
| `crucibleQuenched` | A bucket of water quenches the lava. | `player`, position, `result` (stone or obsidian), `count` |
| `lureFed` | A lure eats a piece of bait. | `player`, position, `subject` (item), `count` (meals so far), `detail` (table) |
| `lureHatched` | A lure answers. | position, `detail` (table), `count`, `drops` |

Event methods: `name()`, `player()`, `dimension()`, `x()`, `y()`, `z()`, `subject()`, `result()`, `count()`, `detail()`, `drops()` (each drop has `item()` and `count()`).

## Reading state

| Call | Returns |
|---|---|
| `blockAt(dimension, x, y, z)` | A map with `kind`: `basin`, `crucible`, `lure`, `pannable` or `none`. Basin: `fluid`, `levels`, `soaking`, `progress`, `result`, `resultCount`. Crucible: `fireSeconds`, `queued`, `lava`. Lure: `table`, `meals`, `secondsLeft`. Pannable: `material`, `wear`. Read values with `get('levels')`. It never loads chunks. |
| `settings()` | The config values right now: `dropMultiplier`, `fastMode`, `panUsesPerBlock`, `panTicks`, `rainFillsBasins`, `buildStarterIsland`. Read only. |
| `tables()`, `drops(table)` | The drop table names, and the rows of one (`item()`, `weight()`, `min()`, `max()`, `minTier()`, `boostPerTier()`). |
| `eventNames()` | The event names above. |

## Changing content

| Call | Does |
|---|---|
| `setDrop(table, item, weight, min, max, minTier, boostPerTier)` | Adds a row, or replaces the row of the same item. An empty item is the "nothing found" row. A new table name creates a table. |
| `removeDrop(table, item)`, `clearDrops(table)` | Removes one row, or empties a table. |
| `setCrush(from, to)`, `removeCrush(from)` | What a hammer blow turns a block into. |
| `setSoak(input, fluid, output, count, ticks, dimension)`, `removeSoak(input, fluid)` | A basin recipe. The fluid is `water` or `lava`; the dimension is an id such as `minecraft:the_nether`, or empty for everywhere. |
| `setMelt(item, units)`, `removeMelt(item)` | Lava units one item gives in a crucible (a clay bucket is 1000). |
| `setFuel(item, ticks)`, `removeFuel(item)` | Burn ticks one item gives a crucible fire. |
| `setBait(item, table)`, `removeBait(item)` | Which drop table a lure uses for a bait item. |
| `resetContent()` | Puts every table and rule back to the mod's own. |

Tables are `silt`, `grit`, `fine_grit`, `nether_silt`, `end_silt` (panning) and `farm`, `night` (lure). The recipe viewer pages show the tables as they were when the viewer loaded, so make content changes in a startup script. Crafting recipes are ordinary data pack recipes and are changed the way ModJS changes any recipe.
