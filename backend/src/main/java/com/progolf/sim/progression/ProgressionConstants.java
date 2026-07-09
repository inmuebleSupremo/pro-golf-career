package com.progolf.sim.progression;

/**
 * The single tunables surface for progression and aging (mirrors {@code SimConstants}). All placeholder
 * calibration — structural behaviour (development raises, aging is non-uniform, a peak exists, growth is
 * bounded) is what matters now; magnitudes are tuned later against desired career arcs.
 */
public final class ProgressionConstants {

    private ProgressionConstants() {
    }

    // --- Development Points ---
    /** Base Development Points awarded per completed season. */
    public static final int DP_PER_SEASON = 40;
    /** Development Points needed to raise an attribute by one rating point at low ratings. */
    public static final double POINTS_PER_RATING = 8.0;
    /** Extra cost factor per rating point above the reference (diminishing returns). */
    public static final double COST_GROWTH = 0.03;
    /** Reference rating above which development costs more. */
    public static final int COST_REFERENCE = 50;
    /** Maximum total attribute rating a golfer may gain from development in one season (gradual growth). */
    public static final double MAX_DEVELOPMENT_PER_SEASON = 4.0;
    /** Number of attributes the AI policy concentrates development on (specialisation). */
    public static final int AI_FOCUS_ATTRIBUTES = 3;

    // --- Development-point stage multipliers ---
    public static final double DP_MULT_DEVELOPMENT = 1.3;
    public static final double DP_MULT_PRIME = 1.0;
    public static final double DP_MULT_LATE = 0.5;

    // --- Career stage boundaries (ages) ---
    public static final int PRIME_START_AGE = 25;
    public static final int LATE_CAREER_START_AGE = 35;

    // --- Aging ---
    /** Reference age at which the aging offset is zero (attributes as generated). */
    public static final int AGING_REFERENCE_AGE = 20;
}
