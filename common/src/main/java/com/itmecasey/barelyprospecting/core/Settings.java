package com.itmecasey.barelyprospecting.core;

/**
 * The mod's options as the rest of the code sees them. Each loader layer reads its own configuration file with the loader's
 * standard config API and installs a {@link Source} here, so the game logic never depends on a loader.
 */
public final class Settings {
    /** Where the values come from. Every method is called on demand, so edits to the config file apply at once. */
    public interface Source {
        /** Multiplies the number of items every drop table hands out (1.0 is the default balance). */
        double dropMultiplier();

        /** Modpack authors: every manual wait (panning, soaking, melting, hatching) becomes four times shorter. */
        boolean fastMode();

        /** How many times one block can be panned before it is used up. */
        int panUsesPerBlock();

        /** Ticks one full panning ring takes with the wooden tray (higher tiers are faster). */
        int panTicks();

        /** Whether rain slowly fills basins that are open to the sky. */
        boolean rainFillsBasins();

        /** Whether the void-island world preset builds its starter island, tree and chest. */
        boolean buildStarterIsland();
    }

    /** The defaults, used until a loader installs its config and in unit tests. */
    public static final Source DEFAULTS = new Source() {
        @Override
        public double dropMultiplier() {
            return 1.0;
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
    };

    private static Source source = DEFAULTS;

    private Settings() {
    }

    /** Called once by the loader layer. */
    public static void install(Source installed) {
        source = installed == null ? DEFAULTS : installed;
    }

    public static double dropMultiplier() {
        return Math.max(0.0, source.dropMultiplier());
    }

    public static boolean fastMode() {
        return source.fastMode();
    }

    public static int panUsesPerBlock() {
        return Math.max(1, source.panUsesPerBlock());
    }

    /** Wait lengths shrink to a quarter in fast mode. */
    public static int scaleWait(int ticks) {
        return source.fastMode() ? Math.max(2, ticks / 4) : ticks;
    }

    public static int panTicksBase() {
        return Math.max(4, source.panTicks());
    }

    public static boolean rainFillsBasins() {
        return source.rainFillsBasins();
    }

    public static boolean buildStarterIsland() {
        return source.buildStarterIsland();
    }
}
