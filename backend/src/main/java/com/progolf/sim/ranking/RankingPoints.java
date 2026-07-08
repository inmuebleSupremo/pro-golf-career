package com.progolf.sim.ranking;

import com.progolf.sim.tournament.Tier;

/**
 * The pure points model (REQ-143): how a finishing position, tier, and field strength convert to ranking
 * points, and how points decay with age. All functions are deterministic and monotone by construction.
 */
final class RankingPoints {

    private RankingPoints() {
    }

    /** Weight for a 1-based finishing position; decreasing, zero beyond the point-paying positions. */
    static double positionWeight(int position) {
        if (position < 1 || position > RankingConstants.POINT_PAYING_POSITIONS) {
            return 0.0;
        }
        return Math.pow(RankingConstants.POSITION_DECAY, position - 1);
    }

    /** Points awarded for a finish: base(tier) × positionWeight × fieldStrengthFactor. */
    static double award(Tier tier, int position, double fieldStrengthFactor) {
        return RankingConstants.basePoints(tier) * positionWeight(position) * fieldStrengthFactor;
    }

    /**
     * Field-strength factor from an average field strength, clamped. A field at the reference strength
     * yields 1.0; stronger fields award proportionally more.
     */
    static double fieldStrengthFactor(double averageFieldStrength) {
        double factor = averageFieldStrength / RankingConstants.REFERENCE_FIELD_STRENGTH;
        return Math.max(RankingConstants.MIN_FIELD_FACTOR, Math.min(RankingConstants.MAX_FIELD_FACTOR, factor));
    }

    /**
     * Decay multiplier in [0,1] for points of a given age in days: full value within the full-value
     * period, declining linearly to zero at the window edge, and zero beyond it.
     */
    static double decay(long ageDays) {
        if (ageDays < 0) {
            return 1.0; // future-dated (shouldn't happen); treat as full value
        }
        if (ageDays <= RankingConstants.FULL_VALUE_DAYS) {
            return 1.0;
        }
        if (ageDays >= RankingConstants.WINDOW_DAYS) {
            return 0.0;
        }
        long declineSpan = RankingConstants.WINDOW_DAYS - RankingConstants.FULL_VALUE_DAYS;
        long into = ageDays - RankingConstants.FULL_VALUE_DAYS;
        return 1.0 - (double) into / declineSpan;
    }
}
