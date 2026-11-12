package com.itmecasey.barelyprospecting.core;

/**
 * One row of a drop table.
 *
 * @param item         item id such as {@code minecraft:flint}; an empty id is a "nothing found" row
 * @param weight       how often the row wins against the others
 * @param min          fewest items when it wins
 * @param max          most items when it wins
 * @param minTier      lowest tray level that can find this row
 * @param boostPerTier extra weight (a fraction of {@code weight}) for every tray level above {@code minTier}
 */
public record DropEntry(String item, int weight, int min, int max, int minTier, double boostPerTier) {
    public DropEntry {
        if (weight < 0 || min < 0 || max < min) {
            throw new IllegalArgumentException("bad drop row for " + item);
        }
    }

    /** The weight this row has for the given tray level, 0 when the tray is too weak. */
    public double weightFor(int tierLevel) {
        if (tierLevel < minTier) {
            return 0.0;
        }
        return weight * (1.0 + boostPerTier * (tierLevel - minTier));
    }

    public boolean isNothing() {
        return item.isEmpty();
    }
}
