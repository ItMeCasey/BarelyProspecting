package com.itmecasey.barelyprospecting.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoreTest {
    private static Dice dice(long seed) {
        return Dice.of(new Random(seed));
    }

    @Test
    void tiersAreOrderedAndFaster() {
        assertEquals(5, Tier.values().length);
        for (int i = 1; i < Tier.values().length; i++) {
            assertTrue(Tier.values()[i].panTicks() < Tier.values()[i - 1].panTicks());
            assertTrue(Tier.values()[i].rolls() >= Tier.values()[i - 1].rolls());
        }
        assertEquals("wooden_panning_tray", Tier.WOOD.itemPath());
        assertEquals(Tier.IRON, Tier.byLevel(2));
        assertEquals(Tier.DIAMOND, Tier.byLevel(99));
    }

    @Test
    void ringPointsGrowWithProgress() {
        assertEquals(0, Ring.litPoints(0, 40, 16));
        assertEquals(8, Ring.litPoints(20, 40, 16));
        assertEquals(16, Ring.litPoints(40, 40, 16));
        assertEquals(16, Ring.litPoints(99, 40, 16));
        assertEquals(0.0, Ring.x(0, 16, 1.0), 1e-9);
        assertEquals(-1.0, Ring.z(0, 16, 1.0), 1e-9);
        assertEquals(1.0, Ring.x(4, 16, 1.0), 1e-9);
    }

    @Test
    void everyTableRowIsValid() {
        for (String name : DropTables.names()) {
            List<DropEntry> rows = DropTables.table(name);
            assertFalse(rows.isEmpty(), name);
            assertTrue(rows.stream().anyMatch(r -> r.weightFor(0) > 0), name + " has something for tier 0");
        }
    }

    @Test
    void tiersGateTheMaterials() {
        // Wood finds no iron, diamond shards or gold in grit; copper finds iron; iron finds gold; gold finds diamond shards.
        assertEquals(0.0, DropTables.chance(DropTables.GRIT, 0, "barelyprospecting:iron_flake"), 1e-12);
        assertTrue(DropTables.chance(DropTables.GRIT, 1, "barelyprospecting:iron_flake") > 0);
        assertEquals(0.0, DropTables.chance(DropTables.GRIT, 1, "barelyprospecting:gold_flake"), 1e-12);
        assertTrue(DropTables.chance(DropTables.GRIT, 2, "barelyprospecting:gold_flake") > 0);
        assertEquals(0.0, DropTables.chance(DropTables.GRIT, 2, "barelyprospecting:diamond_shard"), 1e-12);
        assertTrue(DropTables.chance(DropTables.GRIT, 3, "barelyprospecting:diamond_shard") > 0);
        // Wood can find copper at once (the first metal), and nothing in silt is gated.
        assertTrue(DropTables.chance(DropTables.GRIT, 0, "barelyprospecting:copper_flake") > 0);
        assertTrue(DropTables.chance(DropTables.SILT, 0, "barelyprospecting:pebble") > 0);
        // Netherite scrap needs the diamond tray.
        assertEquals(0.0, DropTables.chance(DropTables.NETHER_SILT, 3, "minecraft:netherite_scrap"), 1e-12);
        assertTrue(DropTables.chance(DropTables.NETHER_SILT, 4, "minecraft:netherite_scrap") > 0);
    }

    @Test
    void finerMaterialHelpsAndHigherTierBoosts() {
        double fine = DropTables.chance(DropTables.FINE_GRIT, 0, "barelyprospecting:gold_flake");
        assertTrue(fine > 0, "fine grit counts as one level up, so wood finds gold in it");
        // A better tray raises the weight of the rows it boosts (gold flakes in grit: tier 3 beats tier 2).
        assertTrue(DropTables.chance(DropTables.GRIT, 3, "barelyprospecting:gold_flake")
                > DropTables.chance(DropTables.GRIT, 2, "barelyprospecting:gold_flake"));
    }

    @Test
    void rollsFindOnlyAllowedItems() {
        Dice d = dice(1);
        Set<String> found = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            for (Drop drop : DropTables.roll(DropTables.GRIT, 0, 1, d)) {
                found.add(drop.item());
                assertTrue(drop.count() >= 1);
            }
        }
        assertTrue(found.contains("barelyprospecting:pebble"));
        assertFalse(found.contains("barelyprospecting:diamond_shard"));
        assertFalse(found.contains("barelyprospecting:iron_flake"));
    }

    @Test
    void multiplierScalesCounts() {
        Settings.install(new Settings.Source() {
            @Override
            public double dropMultiplier() {
                return 3.0;
            }

            @Override
            public boolean fastMode() {
                return false;
            }

            @Override
            public int panUsesPerBlock() {
                return 4;
            }

            @Override
            public int panTicks() {
                return 40;
            }

            @Override
            public boolean rainFillsBasins() {
                return true;
            }

            @Override
            public boolean buildStarterIsland() {
                return true;
            }
        });
        try {
            Dice d = dice(2);
            int most = 0;
            for (int i = 0; i < 500; i++) {
                for (Drop drop : DropTables.roll(DropTables.SILT, 0, 1, d)) {
                    most = Math.max(most, drop.count());
                }
            }
            assertTrue(most >= 6, "pebbles come in twos and the multiplier is 3");
        } finally {
            Settings.install(null);
        }
    }

    @Test
    void crushChainRefinesStepByStep() {
        assertEquals("barelyprospecting:silt", Recipes.crush("minecraft:dirt"));
        assertEquals("barelyprospecting:rubble", Recipes.crush("minecraft:cobblestone"));
        assertEquals("barelyprospecting:grit", Recipes.crush("barelyprospecting:rubble"));
        assertEquals("barelyprospecting:fine_grit", Recipes.crush("barelyprospecting:grit"));
        assertEquals("minecraft:sand", Recipes.crush("barelyprospecting:fine_grit"));
        assertNull(Recipes.crush("minecraft:sand"));
        assertEquals("barelyprospecting:nether_silt", Recipes.crush("minecraft:netherrack"));
        assertEquals("barelyprospecting:end_silt", Recipes.crush("minecraft:end_stone"));
        // Every crush leads somewhere that exists in the chain or in vanilla, and never loops.
        for (Recipes.Crush c : Recipes.crushes()) {
            String at = c.to();
            for (int hops = 0; hops < 10 && at != null; hops++) {
                at = Recipes.crush(at);
            }
            assertNull(at, "crush chain from " + c.from() + " ends");
        }
    }

    @Test
    void basinSoaksOnlyWhatFits() {
        BasinState basin = new BasinState();
        assertNull(basin.start("barelyprospecting:silt", "minecraft:overworld"), "no water yet");
        assertEquals(BasinState.CAPACITY, basin.fill(Recipes.Fluid.WATER, 99));
        assertEquals(0, basin.fill(Recipes.Fluid.LAVA, 1), "one fluid at a time");
        assertNull(basin.start("minecraft:dirt", "minecraft:overworld"), "dirt is not soaked");
        assertNotNull(basin.start("barelyprospecting:silt", "minecraft:overworld"));
        assertEquals(BasinState.CAPACITY - 1, basin.levels());
        assertNull(basin.start("barelyprospecting:silt", "minecraft:overworld"), "one item at a time");
        boolean done = false;
        for (int i = 0; i < 500 && !done; i++) {
            done = basin.tick("minecraft:overworld");
        }
        assertTrue(done);
        assertTrue(basin.hasResult());
        Drop result = basin.takeResult();
        assertEquals("minecraft:clay_ball", result.item());
        assertEquals(2, result.count());
        assertFalse(basin.hasResult());
    }

    @Test
    void netherAndEndSoaksNeedTheirDimension() {
        BasinState basin = new BasinState();
        basin.fill(Recipes.Fluid.LAVA, 4);
        assertNull(basin.start("barelyprospecting:fine_grit", "minecraft:overworld"));
        assertNotNull(basin.start("barelyprospecting:fine_grit", "minecraft:the_nether"));
        BasinState end = new BasinState();
        end.fill(Recipes.Fluid.WATER, 4);
        assertNull(end.start("barelyprospecting:nether_silt", "minecraft:the_nether"));
        assertNotNull(end.start("barelyprospecting:nether_silt", "minecraft:the_end"));
    }

    @Test
    void basinSurvivesSaveAndLoad() {
        BasinState a = new BasinState();
        a.fill(Recipes.Fluid.WATER, 3);
        a.start("barelyprospecting:silt", "minecraft:overworld");
        BasinState b = new BasinState();
        b.load(a.fluidOrdinal(), a.levels(), a.soakingItem(), a.ticksLeft(), a.totalTicks(), a.resultItem(), a.resultCount());
        assertEquals(a.levels(), b.levels());
        assertEquals(a.ticksLeft(), b.ticksLeft());
        assertTrue(b.isSoaking());
    }

    @Test
    void crucibleMeltsOnlyWhileBurning() {
        CrucibleState c = new CrucibleState();
        assertEquals(250, c.feed("barelyprospecting:rubble"));
        assertEquals(0, c.feed("minecraft:diamond"));
        for (int i = 0; i < 400; i++) {
            c.tick();
        }
        assertEquals(0, c.lava(), "no fire, no lava");
        assertEquals(400, c.addFuel(Recipes.fuelTicks("barelyprospecting:coal_dust")));
        for (int i = 0; i < 400; i++) {
            c.tick();
        }
        assertEquals(250, c.lava());
        assertEquals(0, c.queued());
        assertFalse(c.isBurning() && c.fuel() > 400);
    }

    @Test
    void crucibleQuenchesIntoStoneAndObsidian() {
        CrucibleState c = new CrucibleState();
        c.load(0, 0, 250);
        Drop stone = c.quench();
        assertEquals("minecraft:stone", stone.item());
        assertEquals(1, stone.count());
        assertNull(c.quench());
        c.load(0, 0, 1000);
        assertEquals("minecraft:obsidian", c.quench().item());
        c.load(0, 0, 1000);
        assertTrue(c.takeBucket());
        assertEquals(0, c.lava());
        assertFalse(c.takeBucket());
    }

    @Test
    void crucibleHasACapacity() {
        CrucibleState c = new CrucibleState();
        int taken = 0;
        while (c.feed("barelyprospecting:rubble") > 0) {
            taken++;
        }
        assertEquals(CrucibleState.CAPACITY / 250, taken);
        assertEquals(CrucibleState.MAX_FUEL, c.addFuel(10 * CrucibleState.MAX_FUEL));
        assertEquals(CrucibleState.MAX_FUEL, c.fuel());
        assertEquals(0, c.addFuel(5));
    }

    @Test
    void fuelRulesCoverWoodAndCoal() {
        assertTrue(Recipes.fuelTicks("minecraft:charcoal") > Recipes.fuelTicks("minecraft:stick"));
        assertEquals(300, Recipes.fuelTicks("minecraft:oak_planks"));
        assertEquals(0, Recipes.fuelTicks("minecraft:dirt"));
    }

    @Test
    void lureNeedsThreeMealsOfOneKindThenHatches() {
        LureState lure = new LureState();
        assertFalse(lure.feed("minecraft:diamond"));
        assertTrue(lure.feed("minecraft:wheat_seeds"));
        assertFalse(lure.feed("minecraft:bone"), "mixed bait is refused");
        assertTrue(lure.feed("minecraft:beetroot_seeds"));
        assertTrue(lure.feed("minecraft:melon_seeds"));
        assertFalse(lure.feed("minecraft:wheat_seeds"), "hatching");
        assertTrue(lure.isHatching());
        Dice d = dice(7);
        List<Drop> out = null;
        for (int i = 0; i < 1000 && out == null; i++) {
            out = lure.tick(d);
        }
        assertNotNull(out);
        assertEquals(0, lure.meals());
        assertTrue(lure.feed("minecraft:bone"), "empty again, and the other table now works");
    }

    @Test
    void lureTablesCoverTheMobDrops() {
        Set<String> farm = new HashSet<>();
        Set<String> night = new HashSet<>();
        for (DropEntry e : DropTables.table(DropTables.LURE_FARM)) {
            farm.add(e.item());
        }
        for (DropEntry e : DropTables.table(DropTables.LURE_NIGHT)) {
            night.add(e.item());
        }
        assertTrue(farm.contains("minecraft:rotten_flesh") && farm.contains("minecraft:bone"), "the farm lure feeds the night lure");
        assertTrue(night.contains("minecraft:string") && night.contains("minecraft:gunpowder") && night.contains("minecraft:ender_pearl"));
    }

    @Test
    void waitsShrinkInFastMode() {
        assertEquals(400, Settings.scaleWait(400));
        Settings.install(new Settings.Source() {
            @Override
            public double dropMultiplier() {
                return 1.0;
            }

            @Override
            public boolean fastMode() {
                return true;
            }

            @Override
            public int panUsesPerBlock() {
                return 4;
            }

            @Override
            public int panTicks() {
                return 40;
            }

            @Override
            public boolean rainFillsBasins() {
                return true;
            }

            @Override
            public boolean buildStarterIsland() {
                return true;
            }
        });
        try {
            assertEquals(100, Settings.scaleWait(400));
            assertEquals(2, Settings.scaleWait(4));
        } finally {
            Settings.install(null);
        }
    }
}
