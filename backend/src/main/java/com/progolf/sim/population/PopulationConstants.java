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

    /**
     * Overall <em>potential</em> band a golfer's ceiling centres on — not their starting ability. A golfer is
     * generated as the player they could become, and their attributes at generation are then discounted back
     * to their age by {@link Maturity}. The top of the band is a generational talent; the bottom is a career
     * journeyman who will never leave the entry tier.
     */
    public static final double SKILL_MIN = 55.0;
    public static final double SKILL_MAX = 95.0;

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

    // --- Seeded-world age structure ---
    /**
     * The age spread a world is seeded across. Golfers arriving later come through the entry age instead, so
     * this shapes the opening field only: a tour that begins with an age structure rather than one cohort.
     */
    public static final int SEED_AGE_MIN = 16;
    public static final int SEED_AGE_MAX = 44;

    // Create-your-golfer: an archetype-shaped starting build around a prospect baseline (spec: golfer-creation).
    /**
     * The ceiling every created attribute is capable of reaching. Set high in the population's potential
     * band: the player is a real prospect, so a well-developed career can reach the elite tour on merit. It
     * is a ceiling, not a starting point — the created golfer's opening attributes are this discounted to
     * their age by {@link com.progolf.sim.progression.Maturity}, so they still start as a raw talent who has
     * to be developed (spec: competitive-entry / player-development).
     */
    public static final int CREATION_POTENTIAL_BASELINE = 93;
    /** How much an archetype's strength attributes start above the baseline. */
    public static final int CREATION_EMPHASIS = 12;
    /** How much an archetype's weakness attributes start below the baseline. */
    public static final int CREATION_DEEMPHASIS = 10;
    /** Inclusive start-age bounds allowed for a created golfer. */
    public static final int CREATION_MIN_AGE = 16;
    public static final int CREATION_MAX_AGE = 30;
}
