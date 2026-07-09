package com.progolf.sim.progression;

/**
 * A golfer's career stage derived from age (REQ-152). Stages shape development opportunity (they scale
 * Development-Point awards) but never change the gameplay rules.
 */
public enum CareerStage {
    DEVELOPMENT,
    PRIME,
    LATE_CAREER;

    /** The stage for a given age. */
    public static CareerStage of(int age) {
        if (age < ProgressionConstants.PRIME_START_AGE) {
            return DEVELOPMENT;
        }
        if (age < ProgressionConstants.LATE_CAREER_START_AGE) {
            return PRIME;
        }
        return LATE_CAREER;
    }

    /** Multiplier applied to Development-Point awards for this stage (more opportunity when younger). */
    public double developmentMultiplier() {
        return switch (this) {
            case DEVELOPMENT -> ProgressionConstants.DP_MULT_DEVELOPMENT;
            case PRIME -> ProgressionConstants.DP_MULT_PRIME;
            case LATE_CAREER -> ProgressionConstants.DP_MULT_LATE;
        };
    }
}
