package com.itmecasey.barelyprospecting.core;

/** The few random numbers the game logic needs, so it does not depend on Minecraft's random classes. */
public interface Dice {
    /** A number from 0 (inclusive) to bound (exclusive). */
    int nextInt(int bound);

    /** A number from 0.0 (inclusive) to 1.0 (exclusive). */
    double nextDouble();

    /** Wraps a standard {@link java.util.Random}. */
    static Dice of(java.util.Random random) {
        return new Dice() {
            @Override
            public int nextInt(int bound) {
                return random.nextInt(bound);
            }

            @Override
            public double nextDouble() {
                return random.nextDouble();
            }
        };
    }
}
