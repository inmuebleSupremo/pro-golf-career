package com.progolf.sim.shot;

/**
 * The single configuration surface for every realism constant used by shot resolution (task 7.5).
 *
 * <p>These values define the CURRENT calibration and are expected to be tuned in a dedicated pass
 * against target scoring distributions. They live here — and only here — so tuning never requires
 * touching resolution logic. All dispersion values are in yards.
 */
public final class SimConstants {

    private SimConstants() {
    }

    // --- Base dispersion (scales with intended shot length) ---
    // Dispersion = FRACTION * shotDistance + FLOOR, before attribute/condition modifiers. Scaling with
    // shot length is what makes a short putt far tighter than a full drive; a fixed absolute sigma made
    // putts scatter like drives, so holes never actually holed out.
    /** Lateral dispersion as a fraction of intended shot distance. */
    public static final double LATERAL_DISPERSION_FRACTION = 0.025;
    /** Minimum lateral dispersion (yards) regardless of shot length. */
    public static final double LATERAL_DISPERSION_FLOOR = 0.5;
    /** Longitudinal dispersion as a fraction of intended shot distance. */
    public static final double DISTANCE_DISPERSION_FRACTION = 0.020;
    /** Minimum longitudinal dispersion (yards) regardless of shot length. */
    public static final double DISTANCE_DISPERSION_FLOOR = 1.0;

    // --- Attribute influence ---
    /** Attribute normalised value maps to a factor in [MIN_FACTOR, MIN_FACTOR + FACTOR_SPAN]. */
    public static final double MIN_ATTRIBUTE_FACTOR = 0.5;
    public static final double ATTRIBUTE_FACTOR_SPAN = 1.0;
    /** Fraction of club base distance reachable at attribute 0 vs. the span added by distance skill. */
    public static final double REACH_FLOOR = 0.80;
    public static final double REACH_SPAN = 0.40;
    /** Wind resistance from distance skill: floor and span (higher distance skill resists wind more). */
    public static final double WIND_RESIST_FLOOR = 0.30;
    public static final double WIND_RESIST_SPAN = 0.50;

    // --- Condition penalties (sigma multipliers / mean adjustments) ---
    public static final double HEADWIND_MEAN_WEIGHT = 0.70;
    public static final double TAILWIND_MEAN_WEIGHT = 0.50;
    public static final double CROSSWIND_SIGMA_WEIGHT = 0.15;
    public static final double PRESSURE_SIGMA_WEIGHT = 0.50;
    public static final double FATIGUE_SIGMA_WEIGHT = 0.60;
    public static final double FATIGUE_MEAN_WEIGHT = 0.10;
    public static final double LIE_SIGMA_WEIGHT = 0.80;

    // --- Rare extremes (mixture tail) ---
    /** Base probability of a mishit before Course Management reduces it. */
    public static final double BASE_MISHIT_PROBABILITY = 0.030;
    /** Fraction of mishit probability removed at maximum Course Management. */
    public static final double MISHIT_MANAGEMENT_RELIEF = 0.50;
    /** Probability of an exceptional recovery / hero shot. */
    public static final double HERO_PROBABILITY = 0.020;
    /** Error inflation applied on a mishit. */
    public static final double MISHIT_ERROR_MULTIPLIER = 4.0;
    /** Fraction of intended carry lost on a mishit. */
    public static final double MISHIT_SHORTFALL_FRACTION = 0.15;
    /** Error reduction applied on a hero shot. */
    public static final double HERO_ERROR_MULTIPLIER = 0.30;

    // --- Safety net ---
    /** Lateral magnitude beyond which outcomes are compressed. */
    public static final double SAFETY_LATERAL_CAP = 45.0;
    /** Distance-error magnitude beyond which outcomes are compressed. */
    public static final double SAFETY_DISTANCE_CAP = 60.0;
    /** Compression applied to the portion of an error beyond the cap (keeps poor shots, bounds catastrophe). */
    public static final double SAFETY_COMPRESSION = 0.15;
    /** Hard ceiling as a multiple of the cap. */
    public static final double SAFETY_HARD_MULTIPLE = 2.0;

    // --- Round resolution ---
    /** Distance (yards) at or under which the ball is considered holed during round resolution. */
    public static final double HOLED_THRESHOLD = 2.0;
    /** Maximum shots resolved for a single hole (guards against pathological loops). */
    public static final int MAX_SHOTS_PER_HOLE = 12;
}
