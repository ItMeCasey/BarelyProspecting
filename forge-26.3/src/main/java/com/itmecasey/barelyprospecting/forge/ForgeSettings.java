package com.itmecasey.barelyprospecting.forge;

import com.itmecasey.barelyprospecting.core.Settings;
import net.minecraftforge.common.ForgeConfigSpec;

/** The configuration file, {@code barelyprospecting-common.toml} in the config folder, read with Forge's config API. */
final class ForgeSettings implements Settings.Source {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue DROP_MULTIPLIER = BUILDER
            .comment("Multiplies how many items every drop table hands out: panning, the lure and everything else.",
                    "1.0 is the intended balance. A fraction is settled by chance, so 1.5 gives an extra item half of the time.")
            .defineInRange("dropMultiplier", 1.0, 0.0, 100.0);

    private static final ForgeConfigSpec.BooleanValue FAST_MODE = BUILDER
            .comment("Creative-friendly fast mode for modpack authors: every wait (panning, soaking, melting, the lure) is four times shorter.")
            .define("fastMode", false);

    private static final ForgeConfigSpec.IntValue PAN_USES = BUILDER
            .comment("How many times one block of silt, grit or fine grit can be panned before it is used up (1 to 4).")
            .defineInRange("panUsesPerBlock", 4, 1, 4);

    private static final ForgeConfigSpec.IntValue PAN_TICKS = BUILDER
            .comment("Ticks the progress ring takes with the wooden tray (20 ticks are one second). Better trays are faster.")
            .defineInRange("panTicks", 40, 8, 400);

    private static final ForgeConfigSpec.BooleanValue RAIN_FILLS_BASINS = BUILDER
            .comment("Rain slowly fills basins that are open to the sky.")
            .define("rainFillsBasins", true);

    private static final ForgeConfigSpec.BooleanValue BUILD_STARTER_ISLAND = BUILDER
            .comment("In a world made with the Void Island preset, build the starter island, tree and chest the first time it starts.")
            .define("buildStarterIsland", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @Override
    public double dropMultiplier() {
        return SPEC.isLoaded() ? DROP_MULTIPLIER.get() : 1.0;
    }

    @Override
    public boolean fastMode() {
        return SPEC.isLoaded() && FAST_MODE.get();
    }

    @Override
    public int panUsesPerBlock() {
        return SPEC.isLoaded() ? PAN_USES.get() : 4;
    }

    @Override
    public int panTicks() {
        return SPEC.isLoaded() ? PAN_TICKS.get() : 40;
    }

    @Override
    public boolean rainFillsBasins() {
        return !SPEC.isLoaded() || RAIN_FILLS_BASINS.get();
    }

    @Override
    public boolean buildStarterIsland() {
        return !SPEC.isLoaded() || BUILD_STARTER_ISLAND.get();
    }
}
