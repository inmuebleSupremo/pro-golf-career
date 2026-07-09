package com.progolf.sim.economy;

import com.progolf.sim.core.Rng;

/**
 * A golfer's commercial reputation (spec: sponsorship, REQ-186): a [0,1] {@code score} and its
 * {@link ReputationTier}. Derived from the competitive-reputation score the World supplies, with a small
 * bounded deterministic perturbation so it <em>broadly reflects competitive reputation without being
 * identical</em>.
 */
public record CommercialReputation(double score, ReputationTier tier) {

    /** Builds commercial reputation from a [0,1] competitive score plus bounded deterministic noise. */
    public static CommercialReputation fromCompetitive(double competitive, Rng rng) {
        double perturbed = competitive + rng.nextGaussian() * EconomyConstants.COMMERCIAL_NOISE;
        double score = clamp01(perturbed);
        return new CommercialReputation(score, ReputationTier.fromScore(score));
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1.0);
    }
}
