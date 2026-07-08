package com.progolf.sim.core;

import java.util.Objects;

/**
 * Deterministic seed derivation for the world hierarchy (spec: deterministic-rng).
 *
 * <p>A derived seed is a pure function of the parent seed and a child identifier — computed by
 * hashing, NOT by sequentially advancing a shared generator. This guarantees:
 * <ul>
 *   <li>same coordinate -&gt; same seed (reproducibility),</li>
 *   <li>order independence (a seed depends only on its own coordinate), and</li>
 *   <li>sibling isolation (different identifiers -&gt; independent streams).</li>
 * </ul>
 */
public final class Seeds {

    private Seeds() {
    }

    /**
     * Derives a child seed from {@code parentSeed} and a stable {@code childId} using a MurmurHash3-style
     * finalising mix. Pure and version-stable.
     */
    public static long deriveSeed(long parentSeed, long childId) {
        long z = parentSeed * 0xff51afd7ed558ccdL
                + childId * 0xc4ceb9fe1a85ec53L
                + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    /** Resolves the fully-derived seed for a coordinate by chaining derivation down the hierarchy. */
    public static long forCoordinate(SeedCoordinate c) {
        Objects.requireNonNull(c, "coordinate");
        long seed = c.worldSeed();
        seed = deriveSeed(seed, c.seasonId());
        seed = deriveSeed(seed, c.tournamentId());
        seed = deriveSeed(seed, c.roundNo());
        seed = deriveSeed(seed, c.golferId());
        seed = deriveSeed(seed, c.holeNo());
        seed = deriveSeed(seed, c.shotNo());
        return seed;
    }
}
