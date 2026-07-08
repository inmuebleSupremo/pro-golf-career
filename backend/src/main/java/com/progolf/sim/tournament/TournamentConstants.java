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
}
