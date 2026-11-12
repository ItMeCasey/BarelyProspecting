package com.itmecasey.barelyprospecting.core;

/**
 * A crucible: a fire you feed by hand and a pot you fill with rubble. While the fire burns, queued rubble melts into lava. The
 * lava is taken with a clay bucket, or quenched with a bucket of water into stone and obsidian.
 */
public final class CrucibleState {
    public static final int CAPACITY = 2000;
    public static final int MAX_FUEL = 6000;
    /** Lava units melted per burning tick (so one rubble takes about two and a half seconds). */
    public static final int MELT_RATE = 2;

    private int fuel;
    private int queued;
    private int lava;

    public int fuel() {
        return fuel;
    }

    public int queued() {
        return queued;
    }

    public int lava() {
        return lava;
    }

    public boolean isBurning() {
        return fuel > 0;
    }

    /** Lava level 0..3 for the block's look: empty, a puddle, half, full. */
    public int fillLevel() {
        if (lava <= 0) {
            return 0;
        }
        return Math.min(3, 1 + lava * 3 / (CAPACITY + 1));
    }

    /** Adds fuel. Returns how many ticks were added (less than offered once the fire is full). */
    public int addFuel(int ticks) {
        int added = Math.max(0, Math.min(ticks, MAX_FUEL - fuel));
        fuel += added;
        return added;
    }

    /** Puts rubble in. Returns the lava units accepted (0 when the pot is full or the item cannot melt). */
    public int feed(String itemId) {
        int units = Recipes.meltUnits(itemId);
        if (units <= 0 || queued + lava + units > CAPACITY) {
            return 0;
        }
        queued += units;
        return units;
    }

    /** One game tick. Returns true when the visible state changed (lit, lava level). */
    public boolean tick() {
        if (fuel <= 0) {
            return false;
        }
        int levelBefore = fillLevel();
        fuel--;
        if (queued > 0) {
            int step = Math.min(queued, MELT_RATE * (Settings.fastMode() ? 4 : 1));
            queued -= step;
            lava += step;
        }
        return fuel == 0 || fillLevel() != levelBefore;
    }

    /** True when a clay bucket can take a full load of lava. */
    public boolean canFillBucket() {
        return lava >= Recipes.BUCKET;
    }

    /** Takes one bucket of lava. Returns whether it did. */
    public boolean takeBucket() {
        if (!canFillBucket()) {
            return false;
        }
        lava -= Recipes.BUCKET;
        return true;
    }

    /** Adds a bucket of lava poured in by hand (capacity allowing). */
    public boolean pourLava() {
        if (queued + lava + Recipes.BUCKET > CAPACITY) {
            return false;
        }
        lava += Recipes.BUCKET;
        return true;
    }

    /**
     * Quenches the molten lava with a bucket of water: a full block's worth turns into obsidian, a smaller amount into stone, one
     * stone per quarter bucket. Returns the result item id and count, or null when there is nothing to quench.
     */
    public Drop quench() {
        if (lava >= Recipes.OBSIDIAN_UNITS) {
            lava -= Recipes.OBSIDIAN_UNITS;
            return new Drop("minecraft:obsidian", 1);
        }
        int stones = lava / Recipes.STONE_UNITS;
        if (stones > 0) {
            lava -= stones * Recipes.STONE_UNITS;
            return new Drop("minecraft:stone", stones);
        }
        return null;
    }

    public void load(int fuel, int queued, int lava) {
        this.fuel = Math.max(0, Math.min(MAX_FUEL, fuel));
        this.queued = Math.max(0, queued);
        this.lava = Math.max(0, Math.min(CAPACITY, lava));
    }
}
