package com.itmecasey.barelyprospecting.core;

/**
 * The five panning trays. A higher tier pans faster, rolls more often and unlocks rarer rows in the drop tables. Each tray is a
 * hand-crafted upgrade of the one before it, using material the previous tier found.
 */
public enum Tier {
    WOOD("wooden", 0),
    COPPER("copper", 1),
    IRON("iron", 2),
    GOLD("gold", 3),
    DIAMOND("diamond", 4);

    private final String itemPrefix;
    private final int level;

    Tier(String itemPrefix, int level) {
        this.itemPrefix = itemPrefix;
        this.level = level;
    }

    public int level() {
        return level;
    }

    /** The item id path of this tray: {@code wooden_panning_tray}, {@code copper_panning_tray}, and so on. */
    public String itemPath() {
        return itemPrefix + "_panning_tray";
    }

    /** Ticks one panning ring takes: 12 percent shorter per tier. */
    public int panTicks() {
        int base = Settings.panTicksBase();
        return Settings.scaleWait(Math.max(4, base * (100 - 12 * level) / 100));
    }

    /** How many drop rolls one finished pan makes. */
    public int rolls() {
        return level >= 4 ? 3 : level >= 2 ? 2 : 1;
    }

    /** Durability of the tray: tougher metals last longer. */
    public int durability() {
        return switch (this) {
            case WOOD -> 120;
            case COPPER -> 220;
            case IRON -> 360;
            case GOLD -> 200;
            case DIAMOND -> 900;
        };
    }

    public static Tier byLevel(int level) {
        Tier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, level))];
    }
}
