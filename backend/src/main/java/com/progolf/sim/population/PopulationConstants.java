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

    /**
     * Risk-appetite magnitude (in normalised attribute units) beyond which a generated golfer's innate
     * strategic disposition tips from Balanced to Aggressive (above) or Conservative (below). Larger =
     * fewer golfers stray from Balanced. See {@link StrategyDisposition}.
     */
    public static final double STRATEGY_APPETITE_THRESHOLD = 0.10;

    /** Reference year used to derive dates of birth from starting age. */
    public static final int REFERENCE_YEAR = 2000;

    // Create-your-golfer: an archetype-shaped starting build around a prospect baseline (spec: golfer-creation).
    /**
     * The starting baseline every created attribute begins from. Set as a "talented prospect" — above the
     * entry (Development) tier's median — so a newly-created golfer makes entry-tier fields on merit and can
     * begin competing rather than being cut from every field (spec: competitive-entry).
     */
    public static final int CREATION_BASELINE = 56;
    /** How much an archetype's strength attributes start above the baseline. */
    public static final int CREATION_EMPHASIS = 12;
    /** How much an archetype's weakness attributes start below the baseline. */
    public static final int CREATION_DEEMPHASIS = 10;
    /** Inclusive start-age bounds allowed for a created golfer. */
    public static final int CREATION_MIN_AGE = 16;
    public static final int CREATION_MAX_AGE = 30;
}
