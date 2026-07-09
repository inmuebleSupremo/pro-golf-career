package com.progolf.sim.economy;

/**
 * A banded view of commercial reputation (spec: sponsorship, REQ-186). Higher tiers unlock more and
 * richer sponsorship offers. Derived from a [0,1] commercial score via {@link #fromScore}.
 */
public enum ReputationTier {
    UNKNOWN,
    REGIONAL,
    NATIONAL,
    INTERNATIONAL,
    ELITE;

    /** Maps a [0,1] commercial score to its tier using the thresholds in {@link EconomyConstants}. */
    public static ReputationTier fromScore(double score) {
        if (score >= EconomyConstants.TIER_ELITE) {
            return ELITE;
        }
        if (score >= EconomyConstants.TIER_INTERNATIONAL) {
            return INTERNATIONAL;
        }
        if (score >= EconomyConstants.TIER_NATIONAL) {
            return NATIONAL;
        }
        if (score >= EconomyConstants.TIER_REGIONAL) {
            return REGIONAL;
        }
        return UNKNOWN;
    }
}
