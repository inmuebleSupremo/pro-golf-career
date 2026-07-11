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

    // --- Situational pressure (spec: shot-resolution pressure) ---
    // Pressure in [0,1] = base × round weight × prestige weight × contention, fed into the shot/putt models
    // where COMPOSURE resists it. It builds on the closing rounds, scales with event prestige (a major's
    // Sunday is the most), and weighs only on those in contention — so nerves are situational, not constant.
    /** Peak situational pressure (a leader on a major's final round with no composure). */
    public static final double PRESSURE_BASE = 1.0;
    /** Round weight for the third round ("moving day"): some pressure, less than the final round. */
    public static final double PRESSURE_ROUND_3_WEIGHT = 0.4;
    /** Round weight for the final round ("Sunday"): full pressure. Rounds 1-2 carry none. */
    public static final double PRESSURE_ROUND_4_WEIGHT = 1.0;
    /** Prestige weight for a regular event's closing pressure. */
    public static final double PRESSURE_PRESTIGE_REGULAR = 0.6;
    /** Prestige weight for a signature event's closing pressure. */
    public static final double PRESSURE_PRESTIGE_SIGNATURE = 0.8;
    /** Prestige weight for a major's closing pressure — the pinnacle. */
    public static final double PRESSURE_PRESTIGE_MAJOR = 1.0;
    /** Strokes behind the leader at or beyond which a competitor is out of contention (no pressure). */
    public static final int PRESSURE_CONTENTION_STROKES = 8;
}
