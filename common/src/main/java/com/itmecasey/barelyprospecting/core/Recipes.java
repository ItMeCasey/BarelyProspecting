package com.itmecasey.barelyprospecting.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The fixed rules of the hands-on steps, as data: what the hammer crushes into, what a basin soaks, what a crucible melts and
 * burns, what a lure accepts as bait. Ids are item or block ids such as {@code minecraft:dirt}.
 */
public final class Recipes {
    private static final String MOD = DropTables.MOD + ":";

    /** What a hammer blow turns a block into. */
    public record Crush(String from, String to) {
    }

    /** What a basin does with one item: soaks it in a fluid for a while and hands back the result. */
    public record Soak(String input, Fluid fluid, String output, int count, int ticks, String dimension) {
        /** True when the soak works in the given dimension id (an empty requirement means everywhere). */
        public boolean worksIn(String dimensionId) {
            return dimension.isEmpty() || dimension.equals(dimensionId);
        }
    }

    /** The two fluids a basin and a crucible deal with. */
    public enum Fluid {
        NONE, WATER, LAVA
    }

    private static final List<Crush> CRUSH_DEFAULT = List.of(
            // Dirt becomes silt, the first thing a tray can work.
            new Crush("minecraft:dirt", MOD + "silt"),
            new Crush("minecraft:grass_block", MOD + "silt"),
            new Crush("minecraft:coarse_dirt", MOD + "silt"),
            new Crush("minecraft:podzol", MOD + "silt"),
            new Crush("minecraft:rooted_dirt", MOD + "silt"),
            new Crush("minecraft:mud", MOD + "silt"),
            // Stone becomes rubble; rubble, grit and fine grit are refined by hand, one step at a time.
            new Crush("minecraft:stone", MOD + "rubble"),
            new Crush("minecraft:cobblestone", MOD + "rubble"),
            new Crush("minecraft:andesite", MOD + "rubble"),
            new Crush("minecraft:diorite", MOD + "rubble"),
            new Crush("minecraft:granite", MOD + "rubble"),
            new Crush("minecraft:deepslate", MOD + "rubble"),
            new Crush("minecraft:cobbled_deepslate", MOD + "rubble"),
            new Crush("minecraft:tuff", MOD + "rubble"),
            new Crush("minecraft:sandstone", MOD + "rubble"),
            new Crush("minecraft:gravel", MOD + "grit"),
            new Crush(MOD + "rubble", MOD + "grit"),
            new Crush(MOD + "grit", MOD + "fine_grit"),
            new Crush(MOD + "fine_grit", "minecraft:sand"),
            // The other dimensions' stone becomes their own silt.
            new Crush("minecraft:netherrack", MOD + "nether_silt"),
            new Crush("minecraft:end_stone", MOD + "end_silt"));

    private static final List<Soak> SOAK_DEFAULT = List.of(
            new Soak(MOD + "silt", Fluid.WATER, "minecraft:clay_ball", 2, 200, ""),
            new Soak(MOD + "fine_grit", Fluid.LAVA, MOD + "nether_silt", 1, 400, "minecraft:the_nether"),
            new Soak(MOD + "nether_silt", Fluid.WATER, MOD + "end_silt", 1, 400, "minecraft:the_end"));

    /** Units of lava a block or item turns into in a crucible. A full clay bucket is {@link #BUCKET}. */
    private static final Map<String, Integer> MELT_DEFAULT = Map.of(
            MOD + "rubble", 250,
            MOD + "grit", 100,
            MOD + "fine_grit", 50,
            "minecraft:cobblestone", 250,
            "minecraft:netherrack", 250);

    /** Burn time in ticks of what you feed a crucible's fire. */
    private static final Map<String, Integer> FUEL_DEFAULT = Map.of(
            MOD + "twig", 60,
            "minecraft:stick", 100,
            MOD + "coal_dust", 400,
            "minecraft:charcoal", 1600,
            "minecraft:coal", 1600,
            "minecraft:blaze_powder", 1200,
            "minecraft:dried_kelp_block", 4000);

    private static final Map<String, String> BAIT_DEFAULT = Map.of(
            "minecraft:wheat_seeds", DropTables.LURE_FARM,
            "minecraft:beetroot_seeds", DropTables.LURE_FARM,
            "minecraft:melon_seeds", DropTables.LURE_FARM,
            "minecraft:pumpkin_seeds", DropTables.LURE_FARM,
            "minecraft:rotten_flesh", DropTables.LURE_NIGHT,
            "minecraft:bone", DropTables.LURE_NIGHT,
            "minecraft:spider_eye", DropTables.LURE_NIGHT);

    private static final List<Crush> CRUSH = new CopyOnWriteArrayList<>(CRUSH_DEFAULT);
    private static final List<Soak> SOAK = new CopyOnWriteArrayList<>(SOAK_DEFAULT);
    private static final Map<String, Integer> MELT = new ConcurrentHashMap<>(MELT_DEFAULT);
    private static final Map<String, Integer> FUEL = new ConcurrentHashMap<>(FUEL_DEFAULT);
    private static final Map<String, String> BAIT = new ConcurrentHashMap<>(BAIT_DEFAULT);

    /** Lava units in one bucket, and in one block of obsidian. */
    public static final int BUCKET = 1000;
    public static final int OBSIDIAN_UNITS = 1000;
    public static final int STONE_UNITS = 250;

    private Recipes() {
    }

    /** Sets what a hammer blow turns {@code from} into; replaces an existing rule for the same block. */
    public static synchronized void setCrush(String from, String to) {
        CRUSH.removeIf(c -> c.from().equals(from));
        CRUSH.add(new Crush(from, to));
    }

    public static synchronized boolean removeCrush(String from) {
        return CRUSH.removeIf(c -> c.from().equals(from));
    }

    /** Adds a soak; replaces the soak of the same input, fluid and dimension. */
    public static synchronized void setSoak(Soak soak) {
        SOAK.removeIf(s -> s.input().equals(soak.input()) && s.fluid() == soak.fluid() && s.dimension().equals(soak.dimension()));
        SOAK.add(soak);
    }

    /** Removes every soak of this input and fluid (any dimension). */
    public static synchronized boolean removeSoak(String input, Fluid fluid) {
        return SOAK.removeIf(s -> s.input().equals(input) && s.fluid() == fluid);
    }

    public static void setMelt(String item, int units) {
        MELT.put(item, units);
    }

    public static boolean removeMelt(String item) {
        return MELT.remove(item) != null;
    }

    public static void setFuel(String item, int ticks) {
        FUEL.put(item, ticks);
    }

    public static boolean removeFuel(String item) {
        return FUEL.remove(item) != null;
    }

    public static void setBait(String item, String table) {
        BAIT.put(item, table);
    }

    public static boolean removeBait(String item) {
        return BAIT.remove(item) != null;
    }

    /** Back to the mod's own rules. */
    public static synchronized void resetDefaults() {
        CRUSH.clear();
        CRUSH.addAll(CRUSH_DEFAULT);
        SOAK.clear();
        SOAK.addAll(SOAK_DEFAULT);
        MELT.clear();
        MELT.putAll(MELT_DEFAULT);
        FUEL.clear();
        FUEL.putAll(FUEL_DEFAULT);
        BAIT.clear();
        BAIT.putAll(BAIT_DEFAULT);
    }

    public static List<Crush> crushes() {
        return CRUSH;
    }

    /** The block a hammer blow turns {@code blockId} into, or null when the hammer does nothing to it. */
    public static String crush(String blockId) {
        for (Crush c : CRUSH) {
            if (c.from().equals(blockId)) {
                return c.to();
            }
        }
        return null;
    }

    public static List<Soak> soaks() {
        return SOAK;
    }

    /** The soak recipe for an item in the given fluid and dimension, or null. */
    public static Soak soak(String itemId, Fluid fluid, String dimensionId) {
        for (Soak s : SOAK) {
            if (s.input().equals(itemId) && s.fluid() == fluid && s.worksIn(dimensionId)) {
                return s;
            }
        }
        return null;
    }

    public static Map<String, Integer> melts() {
        return MELT;
    }

    /** Lava units this item adds to a crucible, 0 when it cannot be melted. */
    public static int meltUnits(String itemId) {
        return MELT.getOrDefault(itemId, 0);
    }

    public static Map<String, Integer> fuels() {
        return FUEL;
    }

    /** Burn ticks this item adds, 0 when it does not burn. Logs and planks (any wood) count as 300. */
    public static int fuelTicks(String itemId) {
        Integer ticks = FUEL.get(itemId);
        if (ticks != null) {
            return ticks;
        }
        if (itemId.startsWith("minecraft:") && (itemId.endsWith("_log") || itemId.endsWith("_planks") || itemId.endsWith("_wood"))) {
            return 300;
        }
        return 0;
    }

    public static Map<String, String> baits() {
        return BAIT;
    }

    /** The lure table this bait feeds, or null when a lure does not eat it. */
    public static String lureTable(String itemId) {
        return BAIT.get(itemId);
    }
}
