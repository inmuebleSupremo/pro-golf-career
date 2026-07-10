package com.progolf.sim.ranking;

import com.progolf.sim.tournament.Tier;

/**
 * The single tunables surface for the World Ranking (mirrors {@code SimConstants}). Base points, the
 * position curve, and the decay window are placeholder calibration — the structural properties
 * (monotonicity, decay, expiry) are what matter now; magnitudes are tuned later.
 */
public final class RankingConstants {

    private RankingConstants() {
    }

    // --- Base points per tier (points awarded to a winner of a nominal-strength field) ---
    public static final double BASE_DEVELOPMENT = 6.0;
    public static final double BASE_STANDARD = 24.0;
    public static final double BASE_PREMIER = 50.0;
    public static final double BASE_ELITE = 100.0;

    /** Base points for a tier. */
    public static double basePoints(Tier tier) {
        return switch (tier) {
            case DEVELOPMENT -> BASE_DEVELOPMENT;
            case STANDARD -> BASE_STANDARD;
            case PREMIER -> BASE_PREMIER;
            case ELITE -> BASE_ELITE;
        };
    }

    // --- Position curve: weight = POSITION_DECAY^(position-1), so the winner gets the full weight. ---
    public static final double POSITION_DECAY = 0.85;
    /** Positions beyond this earn no ranking points. */
    public static final int POINT_PAYING_POSITIONS = 60;

    // --- Field strength ---
    /** Nominal reference field strength; a field at this strength yields factor 1.0. */
    public static final double REFERENCE_FIELD_STRENGTH = 20.0;
    /** Field-strength factor is clamped to this range so a weak/strong field cannot dominate. */
    public static final double MIN_FIELD_FACTOR = 0.4;
    public static final double MAX_FIELD_FACTOR = 2.5;

    // --- Rolling decay window (days) ---
    /** Points retain full value for this many days after the event. */
    public static final long FULL_VALUE_DAYS = 91;   // ~13 weeks
    /** Points decline linearly to zero by this age; beyond it they contribute nothing. */
    public static final long WINDOW_DAYS = 730;       // ~2 years
}
