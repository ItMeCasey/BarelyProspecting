package com.itmecasey.barelyprospecting.core;

/** The progress ring drawn around a block while a tray is held on it. */
public final class Ring {
    private Ring() {
    }

    /** How many of {@code points} ring points are lit after {@code done} of {@code total} ticks. */
    public static int litPoints(int done, int total, int points) {
        if (total <= 0) {
            return points;
        }
        double fraction = Math.max(0.0, Math.min(1.0, done / (double) total));
        return (int) Math.round(fraction * points);
    }

    /** X offset of point {@code i} of {@code points}, starting at the top (north) and going clockwise, for the given radius. */
    public static double x(int i, int points, double radius) {
        return Math.sin(2.0 * Math.PI * i / points) * radius;
    }

    /** Z offset of point {@code i} of {@code points}, starting at the top (north) and going clockwise. */
    public static double z(int i, int points, double radius) {
        return -Math.cos(2.0 * Math.PI * i / points) * radius;
    }
}
