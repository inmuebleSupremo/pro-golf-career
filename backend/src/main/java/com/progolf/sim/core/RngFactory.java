package com.progolf.sim.core;

import java.util.Objects;

/**
 * Produces a fresh {@link Rng} for any {@link SeedCoordinate} directly from the world master seed,
 * with no traversal or shared state (spec: deterministic-rng). Because each coordinate resolves to a
 * seed independently, shots can be resolved in any order — or lazily — with identical results.
 */
public final class RngFactory {

    private RngFactory() {
    }

    /** Returns a generator seeded at the given coordinate. */
    public static Rng forCoordinate(SeedCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "coordinate");
        return new SplitMix64Rng(Seeds.forCoordinate(coordinate));
    }
}
