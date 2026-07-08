package com.progolf.sim.population;

/**
 * The single tunables surface for population generation. Values define the current calibration and are
 * expected to be tuned later (e.g. per-tour skill bands arrive with the Tour/Tournament domain).
 */
public final class PopulationConstants {

    private PopulationConstants() {
    }

    /** Default number of golfers in a generated population. */
    public static final int DEFAULT_SIZE = 120;

    /** Overall skill band a golfer's attributes centre on. */
    public static final double SKILL_MIN = 40.0;
    public static final double SKILL_MAX = 85.0;

    /** Maximum +/- deviation of an individual attribute from the golfer's overall skill (drives strengths/weaknesses). */
    public static final double ATTRIBUTE_SPREAD = 18.0;

    /** Reference year used to derive dates of birth from starting age. */
    public static final int REFERENCE_YEAR = 2000;
}
