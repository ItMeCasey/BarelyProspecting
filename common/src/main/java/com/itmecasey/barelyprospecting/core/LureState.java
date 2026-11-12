package com.itmecasey.barelyprospecting.core;

import java.util.List;

/**
 * A hand-fed lure: it takes three offerings of the same kind of bait, then something answers after a wait. Nothing about it runs
 * on its own, because without your hand there is no bait. Shared by everyone.
 */
public final class LureState {
    public static final int MEALS = 3;
    public static final int HATCH_TICKS = 300;

    private String table = "";
    private int meals;
    private int ticksLeft;

    public String table() {
        return table;
    }

    public int meals() {
        return meals;
    }

    public boolean isHatching() {
        return meals >= MEALS && ticksLeft > 0;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    /** Offers one bait item. Returns true when the lure ate it. A lure that is hatching, or fed another kind of bait, refuses. */
    public boolean feed(String itemId) {
        String wanted = Recipes.lureTable(itemId);
        if (wanted == null || meals >= MEALS) {
            return false;
        }
        if (meals > 0 && !wanted.equals(table)) {
            return false;
        }
        table = wanted;
        meals++;
        if (meals == MEALS) {
            ticksLeft = Settings.scaleWait(HATCH_TICKS);
        }
        return true;
    }

    /** One game tick. Returns the drops when the hatch just finished, else null. The lure is empty again afterwards. */
    public List<Drop> tick(Dice dice) {
        if (!isHatching()) {
            return null;
        }
        if (--ticksLeft > 0) {
            return null;
        }
        List<Drop> drops = DropTables.roll(table, 0, 3, dice);
        table = "";
        meals = 0;
        ticksLeft = 0;
        return drops;
    }

    public void load(String table, int meals, int ticksLeft) {
        this.table = table == null ? "" : table;
        this.meals = Math.max(0, Math.min(MEALS, meals));
        this.ticksLeft = Math.max(0, ticksLeft);
    }
}
