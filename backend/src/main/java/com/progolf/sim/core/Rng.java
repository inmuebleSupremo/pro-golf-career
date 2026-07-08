package com.progolf.sim.core;

/**
 * A deterministic pseudo-random source. Every random value in the simulation is drawn through an
 * {@code Rng} obtained from the world seed hierarchy (spec: deterministic-rng) — there is no ambient
 * or global randomness anywhere in the core.
 */
public interface Rng {

    /** Next 64-bit value in the stream. */
    long nextLong();

    /** Uniform double in [0, 1). */
    double nextDouble();

    /** Standard normal sample (mean 0, standard deviation 1). */
    double nextGaussian();
}
