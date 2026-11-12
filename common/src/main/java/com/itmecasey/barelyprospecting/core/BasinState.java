package com.itmecasey.barelyprospecting.core;

/**
 * What one soaking basin holds: a fluid with up to {@link #CAPACITY} levels, and at most one item that is soaking. The result
 * waits in the basin until a hand takes it. All of it is shared by everyone who uses the basin.
 */
public final class BasinState {
    public static final int CAPACITY = 4;

    private Recipes.Fluid fluid = Recipes.Fluid.NONE;
    private int levels;
    private String soaking = "";
    private int ticksLeft;
    private int totalTicks;
    private String resultItem = "";
    private int resultCount;

    public Recipes.Fluid fluid() {
        return levels == 0 ? Recipes.Fluid.NONE : fluid;
    }

    public int levels() {
        return levels;
    }

    public boolean isSoaking() {
        return !soaking.isEmpty();
    }

    public String soakingItem() {
        return soaking;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public boolean hasResult() {
        return resultCount > 0;
    }

    public String resultItem() {
        return resultItem;
    }

    public int resultCount() {
        return resultCount;
    }

    /** Soak progress 0..1 (1 once a result is waiting). */
    public double progress() {
        if (hasResult()) {
            return 1.0;
        }
        return totalTicks <= 0 ? 0.0 : 1.0 - ticksLeft / (double) totalTicks;
    }

    /** Adds fluid levels (a bucket fills it). Returns how many levels were taken. A basin holds one kind of fluid at a time. */
    public int fill(Recipes.Fluid kind, int amount) {
        if (kind == Recipes.Fluid.NONE || amount <= 0) {
            return 0;
        }
        if (levels > 0 && fluid != kind) {
            return 0;
        }
        int taken = Math.min(amount, CAPACITY - levels);
        if (taken > 0) {
            fluid = kind;
            levels += taken;
        }
        return taken;
    }

    /** Takes all fluid out (a clay bucket scoops it). Returns what was taken as a fluid and level pair via {@link #levels()} before the call. */
    public int drain() {
        int taken = levels;
        levels = 0;
        fluid = Recipes.Fluid.NONE;
        return taken;
    }

    /** Starts soaking one item when it is free and the fluid and dimension fit. Returns the recipe, or null when it did not start. */
    public Recipes.Soak start(String itemId, String dimensionId) {
        if (isSoaking() || hasResult() || levels <= 0) {
            return null;
        }
        Recipes.Soak recipe = Recipes.soak(itemId, fluid(), dimensionId);
        if (recipe == null) {
            return null;
        }
        soaking = itemId;
        totalTicks = Settings.scaleWait(recipe.ticks());
        ticksLeft = totalTicks;
        levels--;
        if (levels == 0) {
            fluid = Recipes.Fluid.NONE;
        }
        return recipe;
    }

    /** One game tick. Returns true when the soak just finished. */
    public boolean tick(String dimensionId) {
        if (!isSoaking()) {
            return false;
        }
        if (--ticksLeft > 0) {
            return false;
        }
        // The recipe is looked up by item and dimension only, because the fluid was spent when the soak began.
        for (Recipes.Soak s : Recipes.soaks()) {
            if (s.input().equals(soaking) && s.worksIn(dimensionId)) {
                resultItem = s.output();
                resultCount = s.count();
                break;
            }
        }
        soaking = "";
        ticksLeft = 0;
        totalTicks = 0;
        return hasResult();
    }

    /** Hands the waiting result over and empties the slot. */
    public Drop takeResult() {
        if (!hasResult()) {
            return null;
        }
        Drop drop = new Drop(resultItem, resultCount);
        resultItem = "";
        resultCount = 0;
        return drop;
    }

    /** Restores saved values. */
    public void load(int fluidOrdinal, int levels, String soaking, int ticksLeft, int totalTicks, String resultItem, int resultCount) {
        Recipes.Fluid[] all = Recipes.Fluid.values();
        this.fluid = all[Math.max(0, Math.min(all.length - 1, fluidOrdinal))];
        this.levels = Math.max(0, Math.min(CAPACITY, levels));
        this.soaking = soaking == null ? "" : soaking;
        this.ticksLeft = Math.max(0, ticksLeft);
        this.totalTicks = Math.max(0, totalTicks);
        this.resultItem = resultItem == null ? "" : resultItem;
        this.resultCount = Math.max(0, resultCount);
    }

    public int fluidOrdinal() {
        return fluid.ordinal();
    }
}
