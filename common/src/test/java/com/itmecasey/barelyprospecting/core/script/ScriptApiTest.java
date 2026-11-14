package com.itmecasey.barelyprospecting.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyprospecting.core.Dice;
import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.DropTables;
import com.itmecasey.barelyprospecting.core.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScriptApiTest {
    private final ProspectApi api = new ProspectApi();

    @BeforeEach
    @AfterEach
    void reset() {
        api.clearListeners();
        api.resetContent();
        ProspectApi.install(null);
    }

    @Test
    void listenersGetEventsAndAFailingOneDoesNotStopTheRest() {
        List<ScriptEvents.Event> seen = new ArrayList<>();
        api.on("panned", e -> {
            throw new IllegalStateException("script bug");
        });
        api.on("panned", seen::add);
        ScriptEvents.fire("panned", "Steve", "minecraft:overworld", 1, 2, 3, "barelyprospecting:silt", "", 4, "", List.of(new Drop("minecraft:flint", 4)));
        assertEquals(1, seen.size());
        ScriptEvents.Event e = seen.get(0);
        assertEquals("Steve", e.player());
        assertEquals(2, e.y());
        assertEquals(4, e.count());
        assertEquals("minecraft:flint", e.drops().get(0).item());
    }

    @Test
    void anUnknownEventNameIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> api.on("nope", e -> { }));
        assertEquals(8, api.eventNames().size());
    }

    @Test
    void clearingListenersStopsDelivery() {
        List<ScriptEvents.Event> seen = new ArrayList<>();
        api.on("hammered", seen::add);
        api.clearListeners();
        ScriptEvents.fire("hammered", "", "", 0, 0, 0, "a", "b", 1, "", null);
        assertTrue(seen.isEmpty());
    }

    @Test
    void settingADropReplacesTheRowOfTheSameItem() {
        int before = api.drops("silt").size();
        api.setDrop("silt", "minecraft:emerald", 5, 1, 1, 0, 0.0);
        api.setDrop("silt", "minecraft:emerald", 7, 2, 3, 0, 0.0);
        assertEquals(before + 1, api.drops("silt").size());
        assertTrue(api.removeDrop("silt", "minecraft:emerald"));
        assertFalse(api.removeDrop("silt", "minecraft:emerald"));
        assertEquals(before, api.drops("silt").size());
    }

    @Test
    void aClearedTableCanBeFilledAndIsWhatRollsUse() {
        api.clearDrops("grit");
        api.setDrop("grit", "minecraft:diamond", 1, 3, 3, 0, 0.0);
        List<Drop> found = DropTables.roll("grit", 0, 4, Dice.of(new Random(1)));
        assertEquals(1, found.size());
        assertEquals(12, found.get(0).count());
    }

    @Test
    void aScriptCanMakeANewTableForBait() {
        api.setDrop("treats", "minecraft:cookie", 1, 1, 1, 0, 0.0);
        api.setBait("minecraft:sugar", "treats");
        assertEquals("treats", Recipes.lureTable("minecraft:sugar"));
        assertTrue(api.tables().contains("treats"));
    }

    @Test
    void ruleSettersAddReplaceAndRemove() {
        api.setCrush("minecraft:glass", "minecraft:sand");
        assertEquals("minecraft:sand", Recipes.crush("minecraft:glass"));
        api.setCrush("minecraft:glass", "minecraft:gravel");
        assertEquals("minecraft:gravel", Recipes.crush("minecraft:glass"));
        assertTrue(api.removeCrush("minecraft:glass"));
        assertEquals(null, Recipes.crush("minecraft:glass"));

        int soaks = Recipes.soaks().size();
        api.setSoak("minecraft:sand", "water", "minecraft:clay_ball", 1, 20, "");
        api.setSoak("minecraft:sand", "water", "minecraft:clay_ball", 3, 20, "");
        assertEquals(soaks + 1, Recipes.soaks().size());
        assertEquals(3, Recipes.soak("minecraft:sand", Recipes.Fluid.WATER, "minecraft:overworld").count());
        assertTrue(api.removeSoak("minecraft:sand", "water"));
        assertEquals(soaks, Recipes.soaks().size());

        api.setMelt("minecraft:glass", 100);
        api.setFuel("minecraft:cookie", 50);
        assertEquals(100, Recipes.meltUnits("minecraft:glass"));
        assertEquals(50, Recipes.fuelTicks("minecraft:cookie"));
        assertTrue(api.removeMelt("minecraft:glass"));
        assertTrue(api.removeFuel("minecraft:cookie"));
    }

    @Test
    void badArgumentsAreRejectedWithPlainErrors() {
        assertThrows(IllegalArgumentException.class, () -> api.setSoak("a", "oil", "b", 1, 1, ""));
        assertThrows(IllegalArgumentException.class, () -> api.setSoak("a", "water", "b", 0, 1, ""));
        assertThrows(IllegalArgumentException.class, () -> api.setMelt("a", 0));
        assertThrows(IllegalArgumentException.class, () -> api.setFuel("", 5));
        assertThrows(IllegalArgumentException.class, () -> api.setDrop("silt", "minecraft:dirt", 1, 5, 2, 0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> api.setCrush("a", ""));
    }

    @Test
    void resetPutsTheModsOwnContentBack() {
        api.clearDrops("silt");
        api.removeCrush("minecraft:dirt");
        api.setMelt("minecraft:glass", 5);
        api.resetContent();
        assertTrue(api.drops("silt").size() > 5);
        assertEquals("barelyprospecting:silt", Recipes.crush("minecraft:dirt"));
        assertEquals(0, Recipes.meltUnits("minecraft:glass"));
    }

    @Test
    void blockAtReadsThroughTheInstalledReaderAndFallsBackToNone() {
        assertEquals("none", api.blockAt("minecraft:overworld", 0, 0, 0).get("kind"));
        ProspectApi.install((dim, x, y, z) -> Map.of("kind", "basin", "levels", x));
        assertEquals("basin", api.blockAt("minecraft:overworld", 4, 0, 0).get("kind"));
        ProspectApi.install((dim, x, y, z) -> null);
        assertEquals("none", api.blockAt("minecraft:overworld", 4, 0, 0).get("kind"));
    }

    @Test
    void settingsReadTheCurrentValues() {
        Map<String, Object> s = api.settings();
        assertNotNull(s.get("dropMultiplier"));
        assertEquals(4, s.get("panUsesPerBlock"));
    }

    @Test
    void withoutModJsTheBridgeDoesNothingAndDoesNotFail() {
        assertFalse(ModJsBridge.install(api));
    }
}
