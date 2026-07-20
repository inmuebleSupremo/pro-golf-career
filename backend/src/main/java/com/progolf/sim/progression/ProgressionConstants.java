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
    public static final int DP_PER_SEASON = 150;
    /** Development Points needed to raise an attribute by one rating point at low ratings. */
    public static final double POINTS_PER_RATING = 8.0;
    /** Extra cost factor per rating point above the reference (diminishing returns). */
    public static final double COST_GROWTH = 0.03;
    /** Reference rating above which development costs more. */
    public static final int COST_REFERENCE = 50;
    /**
     * Maximum total attribute rating a golfer may gain from development in one season, across all attributes.
     * Sized so a golfer can realistically close the gap to their potential over roughly a decade of good
     * seasons: at 4.0 the ceiling was unreachable, which made a golfer's starting draw their whole career.
     */
    public static final double MAX_DEVELOPMENT_PER_SEASON = 14.0;
    /**
     * How many development increments the human player realises per season, versus one for an AI golfer
     * (spec: player-development / golfer-creation). The player is the generational-talent protagonist: they
     * grind up from a teenage debut while the world is seeded with veterans already near their high ceilings,
     * so without an edge they only reach elite level in their 40s, long after their prime — never a superstar.
     * Several increments a season lets a well-played career realise its potential during its prime, reach the
     * top tour, and contend for wins and majors. Each increment still respects the cost curve, the per-season
     * cap, and the potential ceiling, so talent is realised faster but never exceeded.
     */
    public static final int PLAYER_TALENT_INCREMENTS = 4;

    // --- Performance-driven development ---
    /**
     * A season's Development Points are scaled by how the golfer finished against their field, between these
     * bounds. A golfer who competes and contends improves faster than one who misses cuts — progress is
     * earned on the course, which is where a career game should ask the player to earn it.
     */
    public static final double PERFORMANCE_DP_MIN = 0.90;
    public static final double PERFORMANCE_DP_MAX = 1.45;
    /** Weight on cut-making (competence) versus top-ten rate (excellence) in the performance scaling. */
    public static final double PERFORMANCE_CUT_WEIGHT = 0.4;
    /** Top-ten rate at which a golfer earns the full excellence share of the scaling — a genuine contender. */
    public static final double PERFORMANCE_TOP_TEN_TARGET = 0.35;

    // --- Development-point stage multipliers ---
    public static final double DP_MULT_DEVELOPMENT = 1.3;
    public static final double DP_MULT_PRIME = 1.0;
    /**
     * Late career still develops. A veteran improves more slowly than a prospect, but they do not stop: at
     * 0.5 the award no longer covered even the season's aging, so every golfer past the boundary declined by
     * arithmetic no matter how they played or what they worked on.
     */
    public static final double DP_MULT_LATE = 0.75;

    // --- Career stage boundaries (ages) ---
    public static final int PRIME_START_AGE = 25;
    /** A golfer's prime runs deep into their 40s; late career is the wind-down, not the mid-thirties. */
    public static final int LATE_CAREER_START_AGE = 45;

    // --- Aging ---
    /** Reference age at which the aging offset is zero (attributes as generated). */
    public static final int AGING_REFERENCE_AGE = 20;

    // --- Maturity (growing into potential) ---
    /** The age at and below which a golfer is at their rawest — the entry age to the professional game. */
    public static final int MATURITY_ENTRY_AGE = 16;
    /** The share of their potential a golfer has already realised at the entry age. */
    public static final double MATURITY_AT_ENTRY = 0.62;
    /**
     * The age by which a golfer is expected to have fully realised their potential. Late enough that a career
     * has two decades of visible improvement in it — a golfer should still be getting better at 35.
     */
    public static final int MATURITY_AGE = 38;
}
