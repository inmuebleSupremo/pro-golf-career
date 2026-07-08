package com.progolf.sim.core;

/**
 * SplitMix64 pseudo-random generator — the documented, version-pinned algorithm backing all
 * simulation randomness (spec: deterministic-rng "Documented Generator Algorithm").
 *
 * <p>The algorithm is fixed by contract: changing it is a breaking change to save reproducibility.
 * A generator is a pure function of its 64-bit seed, so the same seed always yields the same stream
 * across processes and application restarts.
 */
public final class SplitMix64Rng implements Rng {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private long state;
    private boolean hasSpareGaussian;
    private double spareGaussian;

    public SplitMix64Rng(long seed) {
        this.state = seed;
    }

    @Override
    public long nextLong() {
        long z = (state += GOLDEN_GAMMA);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    @Override
    public double nextDouble() {
        // 53-bit mantissa in [0, 1).
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    @Override
    public double nextGaussian() {
        // Marsaglia polar method; caches the spare deviate for the next call.
        if (hasSpareGaussian) {
            hasSpareGaussian = false;
            return spareGaussian;
        }
        double u, v, s;
        do {
            u = 2.0 * nextDouble() - 1.0;
            v = 2.0 * nextDouble() - 1.0;
            s = u * u + v * v;
        } while (s >= 1.0 || s == 0.0);
        // StrictMath (not Math) guarantees bit-identical results across JVMs/platforms, which the
        // deterministic-rng spec requires for cross-process save reproducibility (REQ-265/299).
        double mul = StrictMath.sqrt(-2.0 * StrictMath.log(s) / s);
        spareGaussian = v * mul;
        hasSpareGaussian = true;
        return u * mul;
    }
}
