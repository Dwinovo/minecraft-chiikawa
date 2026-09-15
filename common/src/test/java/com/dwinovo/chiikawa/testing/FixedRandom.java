package com.dwinovo.chiikawa.testing;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/**
 * A random source that returns preset values, each kind in turn and repeating its last
 * one. Unused kinds throw.
 */
public final class FixedRandom implements RandomSource {
    private final float[] floats;
    private final int[] ints;
    private int nextFloat;
    private int nextInt;

    private FixedRandom(float[] floats, int[] ints) {
        this.floats = floats;
        this.ints = ints;
    }

    public static FixedRandom floats(float... values) {
        return new FixedRandom(values, new int[0]);
    }

    /** Values for {@link #nextInt(int)}, each taken modulo the bound. */
    public static FixedRandom ints(int... values) {
        return new FixedRandom(new float[0], values);
    }

    @Override
    public float nextFloat() {
        if (floats.length == 0) {
            throw new UnsupportedOperationException();
        }
        return floats[Math.min(nextFloat++, floats.length - 1)];
    }

    @Override
    public int nextInt(int bound) {
        if (ints.length == 0) {
            throw new UnsupportedOperationException();
        }
        return Math.floorMod(ints[Math.min(nextInt++, ints.length - 1)], bound);
    }

    @Override
    public RandomSource fork() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSeed(long seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nextInt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean nextBoolean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double nextDouble() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double nextGaussian() {
        throw new UnsupportedOperationException();
    }
}
