package com.itmecasey.barelyprospecting.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pages the recipe viewers (BEI, JEI, REI, EMI) show for the hands-on steps: panning, hammering, soaking, melting and the lure.
 * Built from the same rules the game uses, so a page can never disagree with the game. Minecraft-free; each viewer plugin turns the
 * item ids into stacks.
 */
public final class ViewerData {
    /** The five kinds of page, each one recipe category in a viewer. The path is the category id and the language key suffix. */
    public enum Kind {
        PAN("panning"), HAMMER("hammering"), SOAK("soaking"), MELT("melting"), LURE("lure");

        private final String path;

        Kind(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    /**
     * One thing a page hands out.
     *
     * @param item    item id
     * @param min     fewest items
     * @param max     most items
     * @param chances chance per roll for each tray level (panning: five values), or one value (the lure); empty when it always happens
     */
    public record Out(String item, int min, int max, float[] chances) {
    }

    /**
     * One page.
     *
     * @param kind       the category
     * @param id         unique id within the category, used for viewer recipe ids
     * @param inputs     each entry is one input slot with the item ids that may sit in it
     * @param outputs    what comes out
     * @param catalysts  items that do the work (trays, hammer, basin, crucible, lure)
     * @param noteKey    language key of the line under the slots, or empty
     * @param noteArgs   arguments of that line
     */
    public record Page(Kind kind, String id, List<List<String>> inputs, List<Out> outputs, List<String> catalysts, String noteKey,
                       List<Object> noteArgs) {
    }

    private static final String MOD = DropTables.MOD + ":";

    private ViewerData() {
    }

    /** Every page of one kind. */
    public static List<Page> pages(Kind kind) {
        return switch (kind) {
            case PAN -> pan();
            case HAMMER -> hammer();
            case SOAK -> soak();
            case MELT -> melt();
            case LURE -> lure();
        };
    }

    public static List<Page> all() {
        List<Page> all = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            all.addAll(pages(kind));
        }
        return all;
    }

    private static List<Out> tableOutputs(String table, int levels) {
        List<Out> outs = new ArrayList<>();
        for (DropEntry row : DropTables.table(table)) {
            if (row.isNothing() || row.weight() <= 0) {
                continue;
            }
            float[] chances = new float[levels];
            boolean any = false;
            for (int t = 0; t < levels; t++) {
                chances[t] = (float) DropTables.chance(table, t, row.item());
                any |= chances[t] > 0;
            }
            if (any) {
                outs.add(new Out(row.item(), row.min(), row.max(), chances));
            }
        }
        return outs;
    }

    private static List<Page> pan() {
        List<Page> pages = new ArrayList<>();
        List<String> trays = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            trays.add(MOD + tier.itemPath());
        }
        for (String material : new String[] {DropTables.SILT, DropTables.GRIT, DropTables.FINE_GRIT, DropTables.NETHER_SILT, DropTables.END_SILT}) {
            pages.add(new Page(Kind.PAN, material, List.of(List.of(MOD + material)), tableOutputs(material, Tier.values().length), trays,
                    "viewer.barelyprospecting.pan_note", List.of(DropTables.minimumTier(material))));
        }
        return pages;
    }

    private static List<Page> hammer() {
        Map<String, List<String>> byResult = new LinkedHashMap<>();
        for (Recipes.Crush crush : Recipes.crushes()) {
            byResult.computeIfAbsent(crush.to(), k -> new ArrayList<>()).add(crush.from());
        }
        List<Page> pages = new ArrayList<>();
        byResult.forEach((to, from) -> pages.add(new Page(Kind.HAMMER, to.substring(to.indexOf(':') + 1), List.of(from),
                List.of(new Out(to, 1, 1, new float[0])), List.of(MOD + "pick_hammer"), "", List.of())));
        return pages;
    }

    private static List<Page> soak() {
        List<Page> pages = new ArrayList<>();
        int i = 0;
        for (Recipes.Soak soak : Recipes.soaks()) {
            String dimension = soak.dimension().isEmpty() ? "" : soak.dimension().substring(soak.dimension().indexOf(':') + 1);
            pages.add(new Page(Kind.SOAK, "soak_" + i++, List.of(List.of(soak.input())),
                    List.of(new Out(soak.output(), soak.count(), soak.count(), new float[0])), List.of(MOD + "basin"),
                    "viewer.barelyprospecting.soak_note_" + soak.fluid().name().toLowerCase(),
                    List.of(soak.ticks() / 20, dimension)));
        }
        return pages;
    }

    private static List<Page> melt() {
        Map<Integer, List<String>> byUnits = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : Recipes.melts().entrySet()) {
            byUnits.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        List<Page> pages = new ArrayList<>();
        byUnits.entrySet().stream().sorted(Map.Entry.<Integer, List<String>>comparingByKey().reversed()).forEach(e -> {
            List<String> inputs = new ArrayList<>(e.getValue());
            inputs.sort(String::compareTo);
            pages.add(new Page(Kind.MELT, "melt_" + e.getKey(), List.of(inputs),
                    List.of(new Out(MOD + "clay_bucket_lava", 1, 1, new float[0])), List.of(MOD + "crucible"),
                    "viewer.barelyprospecting.melt_note", List.of(e.getKey(), Recipes.BUCKET)));
        });
        pages.add(new Page(Kind.MELT, "quench_obsidian", List.of(List.of(MOD + "clay_bucket_water")),
                List.of(new Out("minecraft:obsidian", 1, 1, new float[0])), List.of(MOD + "crucible"),
                "viewer.barelyprospecting.quench_obsidian", List.of(Recipes.OBSIDIAN_UNITS)));
        pages.add(new Page(Kind.MELT, "quench_stone", List.of(List.of(MOD + "clay_bucket_water")),
                List.of(new Out("minecraft:stone", 1, 1, new float[0])), List.of(MOD + "crucible"),
                "viewer.barelyprospecting.quench_stone", List.of(Recipes.STONE_UNITS)));
        return pages;
    }

    private static List<Page> lure() {
        Map<String, List<String>> baits = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : Recipes.baits().entrySet()) {
            baits.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        List<Page> pages = new ArrayList<>();
        baits.forEach((table, items) -> {
            List<String> sorted = new ArrayList<>(items);
            sorted.sort(String::compareTo);
            pages.add(new Page(Kind.LURE, table, List.of(sorted), tableOutputs(table, 1), List.of(MOD + "lure"),
                    "viewer.barelyprospecting.lure_note", List.of(LureState.MEALS, Settings.scaleWait(LureState.HATCH_TICKS) / 20)));
        });
        return pages;
    }
}
