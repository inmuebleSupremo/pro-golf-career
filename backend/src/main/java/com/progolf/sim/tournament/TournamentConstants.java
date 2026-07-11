package com.progolf.sim.tournament;

/**
 * The single tunables surface for the tournament engine (mirrors {@code SimConstants}). Field size and
 * cut line are placeholder defaults pending the Tour/World domain that sizes fields per tier.
 */
public final class TournamentConstants {

    private TournamentConstants() {
    }

    /** Number of rounds in a standard event. */
    public static final int ROUNDS = 4;

    /** Round after which the cut is evaluated. */
    public static final int CUT_AFTER_ROUND = 2;

    /** Default field size. */
    public static final int DEFAULT_FIELD_SIZE = 120;

    /** Number of competitors who make the cut (plus ties). */
    public static final int DEFAULT_CUT_SIZE = 60;

    /** Guard against a pathological playoff never resolving. */
    public static final int MAX_PLAYOFF_HOLES = 18;

    /** Top prize amount; positions below decay by {@link #PRIZE_DECAY} per place. */
    public static final double TOP_PRIZE = 1_000_000.0;
    public static final double PRIZE_DECAY = 0.78;
    /** Number of paid positions. */
    public static final int PAID_POSITIONS = 70;

    // Event-prestige multipliers (spec: event-prestige). Monotone: major > signature > regular; regular is
    // the neutral 1.0 baseline held implicitly in EventPrestige.
    /** Ranking-point multiplier for a Signature event. */
    public static final double SIGNATURE_RANKING_WEIGHT = 1.75;
    /** Ranking-point multiplier for a Major. */
    public static final double MAJOR_RANKING_WEIGHT = 3.0;
    /** Purse (top-prize) multiplier for a Signature event. */
    public static final double SIGNATURE_PURSE_WEIGHT = 2.0;
    /** Purse (top-prize) multiplier for a Major. */
    public static final double MAJOR_PURSE_WEIGHT = 4.0;

    // Tour-tier purse multipliers (spec: financial-strategy): higher tiers pay more, so climbing the ladder
    // unlocks real money. Monotone Development < Standard < Premier < Elite.
    public static final double PURSE_DEVELOPMENT = 0.08;
    public static final double PURSE_STANDARD = 0.22;
    public static final double PURSE_PREMIER = 0.50;
    public static final double PURSE_ELITE = 1.00;

    /** Fraction of the field that makes the cut (plays the weekend) — and thus the pay line; the rest are
     * cut and earn nothing (spec: financial-strategy). Sizes both the cut and the paid positions. */
    public static final double CUT_FRACTION = 0.45;
}
