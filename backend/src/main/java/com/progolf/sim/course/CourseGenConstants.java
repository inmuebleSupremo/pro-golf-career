package com.progolf.sim.course;

/**
 * The single tunables surface for course generation (mirrors {@code SimConstants} in the shot engine).
 * These define the current generation calibration and are expected to be tuned later; generation logic
 * never hard-codes values outside this class. Distances are in yards.
 */
public final class CourseGenConstants {

    private CourseGenConstants() {
    }

    /** Retained historical generator. Reproducibility is defined as (seed + version). */
    public static final int V1_GENERATOR_VERSION = 1;
    /** First design-aware generator. */
    public static final int V2_GENERATOR_VERSION = 2;
    /** First route-, landing-zone-, and green-complex-aware generator. */
    public static final int V3_GENERATOR_VERSION = 3;
    /** First role-based hazard generator. */
    public static final int V4_GENERATOR_VERSION = 4;
    /** First candidate-selected organic course architecture generator. */
    public static final int V5_GENERATOR_VERSION = 5;
    /** First retained generator with shared course-scale landscape and hole placement. */
    public static final int V6_GENERATOR_VERSION = 6;
    /** Generator selected only when a new world is created. Restore always uses its persisted pin. */
    public static final int CURRENT_GENERATOR_VERSION = V5_GENERATOR_VERSION;
    /** @deprecated Use an explicit retained generator version. */
    @Deprecated(forRemoval = false)
    public static final int GENERATOR_VERSION = V1_GENERATOR_VERSION;

    // --- Par distribution (sums to par 72 over 18 holes) ---
    public static final int PAR3_COUNT = 4;
    public static final int PAR4_COUNT = 10;
    public static final int PAR5_COUNT = 4;

    // --- Hole length ranges by par (yards) ---
    public static final double PAR3_MIN = 130, PAR3_MAX = 240;
    public static final double PAR4_MIN = 320, PAR4_MAX = 470;
    public static final double PAR5_MIN = 490, PAR5_MAX = 610;

    // --- Hole geometry ranges ---
    public static final double FAIRWAY_HALF_MIN = 14, FAIRWAY_HALF_MAX = 22;
    public static final double GREEN_HALF_MIN = 8, GREEN_HALF_MAX = 16;
    public static final double GREEN_DEPTH_MIN = 18, GREEN_DEPTH_MAX = 34;
    public static final double ELEVATION_RANGE = 25; // +/- yards of cumulative elevation change

    // --- Hazard probabilities ---
    public static final double GREENSIDE_BUNKER_PROB = 0.65;

    // --- Per-round pin variation ---
    public static final double PIN_DEPTH_RANGE = 6.0;   // +/- yards front-to-back
    public static final double PIN_LATERAL_FACTOR = 0.5; // fraction of green half-width
    /** How strongly the pin's depth shifts the green off-centre from the pin: a back pin puts the
     *  over-green trouble closer behind (attacking it risks going long), a front pin shortens the safe
     *  zone in front (coming up short risks the fringe). Fraction of the pin depth offset applied. */
    public static final double PIN_DEPTH_ASYMMETRY = 0.6;
    /** Minimum green depth kept on either side of the pin so it is never off its own green. */
    public static final double PIN_DEPTH_MIN_SIDE = 3.0;

    // --- Zone-band lateral widths (added cumulatively outward from centre) ---
    public static final double FRINGE_WIDTH = 3.0;
    public static final double FAIRWAY_FIRST_CUT_EXTRA = 6.0;
    public static final double FAIRWAY_ROUGH_EXTRA = 12.0;
    public static final double FAIRWAY_DEEP_EXTRA = 15.0;
    public static final double HAZARD_EXTRA = 15.0;
    public static final double GREEN_FRINGE_EXTRA = 4.0;
    public static final double GREEN_BUNKER_EXTRA = 6.0;
    public static final double GREEN_ROUGH_EXTRA = 12.0;

    // --- Longitudinal green-complex sizing ---
    public static final double APPROACH_FRINGE = 3.0;
    public static final double OVER_GREEN_MARGIN = 40.0;
}
