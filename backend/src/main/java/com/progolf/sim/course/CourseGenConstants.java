package com.progolf.sim.course;

/**
 * The single tunables surface for course generation (mirrors {@code SimConstants} in the shot engine).
 * These define the current generation calibration and are expected to be tuned later; generation logic
 * never hard-codes values outside this class. Distances are in yards.
 */
public final class CourseGenConstants {

    private CourseGenConstants() {
    }

    /** The current generator algorithm version. Reproducibility is defined as (seed + version). */
    public static final int GENERATOR_VERSION = 1;

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
