package com.itmecasey.barelyprospecting.core.script;

import com.itmecasey.barelyprospecting.core.DropEntry;
import com.itmecasey.barelyprospecting.core.DropTables;
import com.itmecasey.barelyprospecting.core.Recipes;
import com.itmecasey.barelyprospecting.core.Settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * The one scripting surface. The ModJS bridge hands an instance to scripts as {@code BarelyProspecting}; Java code can use it
 * directly. Everything takes plain strings and numbers (item and block ids such as {@code minecraft:flint}).
 *
 * <p>Content changes (drop tables and rules) apply at once to the game. Every setter replaces an existing entry of the same key, so
 * a script that runs twice does not double anything. A bad argument throws a plain error that ModJS logs as a script error.
 */
public final class ProspectApi {
    /** Reads the block at a position; installed by the game code once a server runs. A null result means nothing of this mod there. */
    public interface StateReader {
        Map<String, Object> read(String dimension, int x, int y, int z);
    }

    private static volatile StateReader reader;

    public static void install(StateReader r) {
        reader = r;
    }

    // ---- events

    /** Listens to an event: panned, hammered, soakStarted, soakFinished, crucibleFed, crucibleQuenched, lureFed, lureHatched. */
    public void on(String event, Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on(event, listener);
    }

    /** Drops every listener. Call it first in a script that registers listeners, so a script reload replaces the old ones. */
    public void clearListeners() {
        ScriptEvents.clear();
    }

    public List<String> eventNames() {
        return List.copyOf(new TreeSet<>(ScriptEvents.NAMES));
    }

    // ---- read access

    /**
     * What this mod has at a position: a map with {@code kind} (basin, crucible, lure, pannable or none) and the values of that
     * block. Basin: fluid, levels, soaking, progress, result, resultCount. Crucible: fireSeconds, queued, lava. Lure: table, meals,
     * secondsLeft. Pannable: material, wear. Use it from a server script or an event; it never loads chunks.
     */
    public Map<String, Object> blockAt(String dimension, int x, int y, int z) {
        StateReader r = reader;
        Map<String, Object> out = r == null ? null : r.read(dimension, x, y, z);
        if (out == null) {
            out = new LinkedHashMap<>();
            out.put("kind", "none");
        }
        return out;
    }

    /** The options as the config file sets them right now (read only). */
    public Map<String, Object> settings() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dropMultiplier", Settings.dropMultiplier());
        out.put("fastMode", Settings.fastMode());
        out.put("panUsesPerBlock", Settings.panUsesPerBlock());
        out.put("panTicks", Settings.panTicksBase());
        out.put("rainFillsBasins", Settings.rainFillsBasins());
        out.put("buildStarterIsland", Settings.buildStarterIsland());
        return out;
    }

    // ---- drop tables

    /** Names of every drop table: silt, grit, fine_grit, nether_silt, end_silt, farm, night (and any a script created). */
    public List<String> tables() {
        return DropTables.names();
    }

    /** The rows of a table; each has item(), weight(), min(), max(), minTier() and boostPerTier(). */
    public List<DropEntry> drops(String table) {
        return DropTables.table(table);
    }

    /** Adds or replaces the row of {@code item} in a table. An empty item is the "nothing found" row. */
    public void setDrop(String table, String item, int weight, int min, int max, int minTier, double boostPerTier) {
        requireName(table, "table");
        DropTables.setRow(table, new DropEntry(item == null ? "" : item, weight, min, max, minTier, boostPerTier));
    }

    public boolean removeDrop(String table, String item) {
        return DropTables.removeRow(table, item);
    }

    /** Empties a table so a script can fill it with its own rows. */
    public void clearDrops(String table) {
        requireName(table, "table");
        DropTables.clearTable(table);
    }

    // ---- hammer, basin, crucible, lure rules

    /** A hammer blow turns block {@code from} into block {@code to}. */
    public void setCrush(String from, String to) {
        requireName(from, "from");
        requireName(to, "to");
        Recipes.setCrush(from, to);
    }

    public boolean removeCrush(String from) {
        return Recipes.removeCrush(from);
    }

    /**
     * A basin soaks {@code input} in {@code fluid} (water or lava) for {@code ticks} and hands back {@code count} of
     * {@code output}. {@code dimension} limits it to one dimension id such as minecraft:the_nether; an empty text works everywhere.
     */
    public void setSoak(String input, String fluid, String output, int count, int ticks, String dimension) {
        requireName(input, "input");
        requireName(output, "output");
        if (count < 1 || ticks < 1) {
            throw new IllegalArgumentException("BarelyProspecting.setSoak: count and ticks must be at least 1");
        }
        Recipes.setSoak(new Recipes.Soak(input, fluid(fluid), output, count, ticks, dimension == null ? "" : dimension));
    }

    public boolean removeSoak(String input, String fluid) {
        return Recipes.removeSoak(input, fluid(fluid));
    }

    /** A crucible turns one {@code item} into {@code units} of lava (a clay bucket is 1000). */
    public void setMelt(String item, int units) {
        requireName(item, "item");
        if (units < 1) {
            throw new IllegalArgumentException("BarelyProspecting.setMelt: units must be at least 1");
        }
        Recipes.setMelt(item, units);
    }

    public boolean removeMelt(String item) {
        return Recipes.removeMelt(item);
    }

    /** A crucible's fire burns {@code ticks} for one {@code item}. */
    public void setFuel(String item, int ticks) {
        requireName(item, "item");
        if (ticks < 1) {
            throw new IllegalArgumentException("BarelyProspecting.setFuel: ticks must be at least 1");
        }
        Recipes.setFuel(item, ticks);
    }

    public boolean removeFuel(String item) {
        return Recipes.removeFuel(item);
    }

    /** A lure eats {@code item} as bait for the named drop table (farm, night or one made with setDrop). */
    public void setBait(String item, String table) {
        requireName(item, "item");
        requireName(table, "table");
        Recipes.setBait(item, table);
    }

    public boolean removeBait(String item) {
        return Recipes.removeBait(item);
    }

    /** Puts every drop table and rule back to the mod's own. */
    public void resetContent() {
        DropTables.resetDefaults();
        Recipes.resetDefaults();
    }

    private static void requireName(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BarelyProspecting: " + what + " must not be empty");
        }
    }

    private static Recipes.Fluid fluid(String name) {
        if ("water".equalsIgnoreCase(name)) {
            return Recipes.Fluid.WATER;
        }
        if ("lava".equalsIgnoreCase(name)) {
            return Recipes.Fluid.LAVA;
        }
        throw new IllegalArgumentException("BarelyProspecting: fluid must be water or lava, not " + name);
    }
}
