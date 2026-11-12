package com.itmecasey.barelyprospecting.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every drop table of the mod: what panning each material can turn up, and what a hand-fed lure hatches. Names, rows and numbers
 * are this mod's own. A finer material counts as one tray level higher than it is held at (see {@link #tierShift(String)}).
 */
public final class DropTables {
    public static final String MOD = "barelyprospecting";

    /** Pannable source blocks (item and block path) in the order of the progression. */
    public static final String SILT = "silt";
    public static final String GRIT = "grit";
    public static final String FINE_GRIT = "fine_grit";
    public static final String NETHER_SILT = "nether_silt";
    public static final String END_SILT = "end_silt";

    /** Lure tables. */
    public static final String LURE_FARM = "farm";
    public static final String LURE_NIGHT = "night";

    private static final Map<String, List<DropEntry>> TABLES = new LinkedHashMap<>();

    static {
        TABLES.put(SILT, List.of(
                none(22),
                mine("pebble", 40, 1, 2, 0, 0.0),
                mine("twig", 22, 1, 2, 0, 0.0),
                vanilla("flint", 7, 1, 1, 0, 0.2),
                mine("coal_dust", 8, 1, 2, 0, 0.1),
                vanilla("wheat_seeds", 6, 1, 1, 0, 0.0),
                vanilla("beetroot_seeds", 3, 1, 1, 0, 0.0),
                vanilla("melon_seeds", 2, 1, 1, 0, 0.0),
                vanilla("pumpkin_seeds", 2, 1, 1, 0, 0.0),
                vanilla("oak_sapling", 3, 1, 1, 0, 0.0),
                vanilla("birch_sapling", 2, 1, 1, 0, 0.0),
                vanilla("spruce_sapling", 2, 1, 1, 0, 0.0),
                vanilla("jungle_sapling", 1, 1, 1, 0, 0.0),
                vanilla("acacia_sapling", 1, 1, 1, 0, 0.0),
                vanilla("dark_oak_sapling", 1, 1, 1, 0, 0.0),
                vanilla("sugar_cane", 3, 1, 1, 0, 0.0),
                vanilla("sweet_berries", 3, 1, 2, 0, 0.0),
                vanilla("cactus", 1, 1, 1, 0, 0.0),
                vanilla("bone_meal", 4, 1, 2, 0, 0.0),
                vanilla("apple", 1, 1, 1, 0, 0.0)));

        TABLES.put(GRIT, List.of(
                none(20),
                mine("pebble", 22, 1, 2, 0, 0.0),
                vanilla("flint", 10, 1, 1, 0, 0.0),
                mine("coal_dust", 14, 1, 3, 0, 0.1),
                mine("copper_flake", 8, 1, 2, 0, 0.3),
                mine("iron_flake", 6, 1, 2, 1, 0.3),
                vanilla("redstone", 5, 1, 2, 1, 0.3),
                mine("gold_flake", 4, 1, 1, 2, 0.4),
                vanilla("lapis_lazuli", 4, 1, 2, 2, 0.3),
                mine("diamond_shard", 2, 1, 1, 3, 0.6),
                vanilla("sand", 10, 1, 1, 0, 0.0)));

        TABLES.put(FINE_GRIT, List.of(
                none(16),
                mine("copper_flake", 10, 1, 3, 0, 0.3),
                mine("iron_flake", 9, 1, 2, 0, 0.3),
                mine("gold_flake", 6, 1, 2, 1, 0.4),
                vanilla("redstone", 8, 1, 3, 0, 0.3),
                vanilla("lapis_lazuli", 6, 1, 2, 1, 0.3),
                mine("diamond_shard", 5, 1, 2, 2, 0.6),
                mine("coal_dust", 10, 1, 2, 0, 0.0),
                vanilla("glowstone_dust", 2, 1, 1, 2, 0.0),
                vanilla("amethyst_shard", 2, 1, 1, 2, 0.0)));

        TABLES.put(NETHER_SILT, List.of(
                none(15),
                vanilla("quartz", 20, 1, 3, 0, 0.1),
                vanilla("glowstone_dust", 12, 1, 3, 0, 0.1),
                vanilla("nether_wart", 8, 1, 2, 0, 0.0),
                mine("gold_flake", 10, 1, 3, 0, 0.2),
                vanilla("magma_cream", 5, 1, 1, 1, 0.2),
                vanilla("blaze_powder", 6, 1, 2, 2, 0.2),
                vanilla("blaze_rod", 2, 1, 1, 3, 0.2),
                vanilla("ghast_tear", 2, 1, 1, 3, 0.2),
                mine("diamond_shard", 4, 1, 2, 2, 0.4),
                vanilla("netherite_scrap", 1, 1, 1, 4, 0.0)));

        TABLES.put(END_SILT, List.of(
                none(15),
                vanilla("chorus_fruit", 14, 1, 2, 0, 0.0),
                vanilla("amethyst_shard", 10, 1, 3, 0, 0.0),
                vanilla("ender_pearl", 5, 1, 1, 0, 0.2),
                vanilla("emerald", 4, 1, 1, 1, 0.3),
                vanilla("diamond", 3, 1, 1, 2, 0.5),
                vanilla("end_stone", 10, 1, 2, 0, 0.0),
                vanilla("netherite_scrap", 2, 1, 1, 3, 0.2)));

        TABLES.put(LURE_FARM, List.of(
                none(10),
                vanilla("feather", 30, 1, 2, 0, 0.0),
                vanilla("egg", 20, 1, 2, 0, 0.0),
                vanilla("chicken", 16, 1, 1, 0, 0.0),
                vanilla("rotten_flesh", 18, 1, 2, 0, 0.0),
                vanilla("bone", 10, 1, 1, 0, 0.0),
                vanilla("string", 8, 1, 1, 0, 0.0),
                vanilla("leather", 6, 1, 1, 0, 0.0)));

        TABLES.put(LURE_NIGHT, List.of(
                none(10),
                vanilla("bone", 24, 1, 2, 0, 0.0),
                vanilla("string", 22, 1, 2, 0, 0.0),
                vanilla("gunpowder", 20, 1, 2, 0, 0.0),
                vanilla("spider_eye", 12, 1, 1, 0, 0.0),
                vanilla("rotten_flesh", 14, 1, 2, 0, 0.0),
                vanilla("slime_ball", 6, 1, 1, 0, 0.0),
                vanilla("ender_pearl", 4, 1, 1, 0, 0.0)));
    }

    private DropTables() {
    }

    private static DropEntry none(int weight) {
        return new DropEntry("", weight, 0, 0, 0, 0.0);
    }

    private static DropEntry mine(String path, int weight, int min, int max, int minTier, double boost) {
        return new DropEntry(MOD + ":" + path, weight, min, max, minTier, boost);
    }

    private static DropEntry vanilla(String path, int weight, int min, int max, int minTier, double boost) {
        return new DropEntry("minecraft:" + path, weight, min, max, minTier, boost);
    }

    /** The rows of a table, empty when the name is unknown. */
    public static List<DropEntry> table(String name) {
        return TABLES.getOrDefault(name, List.of());
    }

    public static Iterable<String> names() {
        return TABLES.keySet();
    }

    /** True for the five materials a tray can pan. */
    public static boolean isPannable(String path) {
        return SILT.equals(path) || GRIT.equals(path) || FINE_GRIT.equals(path) || NETHER_SILT.equals(path) || END_SILT.equals(path);
    }

    /** Finer material is worth more: fine grit pans one level above the tray, End silt two. */
    public static int tierShift(String source) {
        return switch (source) {
            case FINE_GRIT -> 1;
            case END_SILT -> 1;
            default -> 0;
        };
    }

    /** The lowest tray level that is allowed to pan this material at all (the gate between the phases). */
    public static int minimumTier(String source) {
        return switch (source) {
            case NETHER_SILT -> 1;
            case END_SILT -> 2;
            default -> 0;
        };
    }

    /**
     * One pan or hatch: rolls the table {@code rolls} times with a tray of the given level and returns what was found. The count of
     * every found row is scaled by the configured drop multiplier (a fraction is settled by chance).
     */
    public static List<Drop> roll(String tableName, int tierLevel, int rolls, Dice dice) {
        List<DropEntry> rows = table(tableName);
        List<Drop> out = new ArrayList<>();
        int level = tierLevel + tierShift(tableName);
        double multiplier = Settings.dropMultiplier();
        for (int r = 0; r < rolls; r++) {
            DropEntry row = pick(rows, level, dice);
            if (row == null || row.isNothing()) {
                continue;
            }
            int count = row.min() + (row.max() > row.min() ? dice.nextInt(row.max() - row.min() + 1) : 0);
            double scaled = count * multiplier;
            int whole = (int) Math.floor(scaled);
            if (dice.nextDouble() < scaled - whole) {
                whole++;
            }
            if (whole > 0) {
                merge(out, row.item(), whole);
            }
        }
        return out;
    }

    private static void merge(List<Drop> out, String item, int count) {
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).item().equals(item)) {
                out.set(i, new Drop(item, out.get(i).count() + count));
                return;
            }
        }
        out.add(new Drop(item, count));
    }

    private static DropEntry pick(List<DropEntry> rows, int level, Dice dice) {
        double total = 0.0;
        for (DropEntry row : rows) {
            total += row.weightFor(level);
        }
        if (total <= 0.0) {
            return null;
        }
        double roll = dice.nextDouble() * total;
        for (DropEntry row : rows) {
            double w = row.weightFor(level);
            if (w <= 0.0) {
                continue;
            }
            if (roll < w) {
                return row;
            }
            roll -= w;
        }
        return rows.get(rows.size() - 1);
    }

    /** The chance (0..1) that one roll with the given tray level finds this item. Used by the viewer integrations. */
    public static double chance(String tableName, int tierLevel, String item) {
        List<DropEntry> rows = table(tableName);
        int level = tierLevel + tierShift(tableName);
        double total = 0.0;
        double mine = 0.0;
        for (DropEntry row : rows) {
            double w = row.weightFor(level);
            total += w;
            if (row.item().equals(item)) {
                mine += w;
            }
        }
        return total <= 0.0 ? 0.0 : mine / total;
    }
}
