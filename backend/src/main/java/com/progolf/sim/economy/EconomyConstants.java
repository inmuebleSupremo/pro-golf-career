package com.progolf.sim.economy;

/**
 * The single tunables surface for the Economy &amp; Sponsorship domain (spec: financial-identity /
 * sponsorship / financial-strategy). All magnitudes live here so the game's economics can be calibrated
 * in one place. Amounts are in abstract currency units, consistent with tournament prize money
 * ({@code TournamentConstants.TOP_PRIZE = 1_000_000}).
 */
public final class EconomyConstants {

    private EconomyConstants() {
    }

    /**
     * Distinct salt mixed into the per-golfer/season economy seed so sponsorship randomness is isolated
     * from the shot and weather streams. A large prime, different from the weather salt.
     */
    public static final long ECONOMY_SALT = 555_555_557L;

    // --- Financial identity ---
    public static final double STARTING_FUNDS = 50_000.0;

    // --- Career expenses (per event) ---
    public static final double ENTRY_FEE_ELITE = 40_000.0;
    public static final double ENTRY_FEE_PRIMARY = 25_000.0;
    public static final double ENTRY_FEE_SECONDARY = 15_000.0;
    public static final double ENTRY_FEE_DEVELOPMENT = 8_000.0;
    public static final double TRAVEL_COST = 12_000.0;

    // --- Financial milestones (career earnings thresholds) ---
    public static final double SIX_FIGURE_EARNINGS = 100_000.0;
    public static final double MILLION_EARNINGS = 1_000_000.0;
    public static final double MULTI_MILLION_EARNINGS = 10_000_000.0;

    // --- Reputation ---
    public static final double WIN_REPUTATION_WEIGHT = 0.10;   // 10 career wins ⇒ full win reputation
    public static final double POSITION_REPUTATION_WEIGHT = 0.70;
    public static final double WINS_REPUTATION_WEIGHT = 0.30;
    public static final double COMMERCIAL_NOISE = 0.08;        // commercial ≠ competitive, but correlated
    public static final double MOMENTUM_STEP = 0.05;           // objective success/failure nudges standing
    public static final double MOMENTUM_CAP = 0.20;

    // Reputation-tier thresholds on the [0,1] commercial score.
    public static final double TIER_REGIONAL = 0.20;
    public static final double TIER_NATIONAL = 0.40;
    public static final double TIER_INTERNATIONAL = 0.60;
    public static final double TIER_ELITE = 0.80;

    // --- Sponsorship offers ---
    public static final int OFFER_BASE = 1;                    // offers at zero reputation
    public static final int OFFER_REP_SPAN = 4;                // additional offers at full reputation
    public static final double BASE_PAYMENT = 80_000.0;        // per-season payment floor
    public static final double PAYMENT_REP_SCALE = 4.0;        // reputation multiplies payment
    public static final double OFFER_VALUE_NOISE = 0.15;
    public static final double MIN_PAYMENT = 20_000.0;
    public static final double SIGNING_FRACTION = 0.50;        // signing bonus as a fraction of payment
    public static final int MIN_DURATION = 2;                  // seasons
    public static final int MAX_DURATION = 4;

    // --- Sponsorship objectives ---
    public static final int MAX_OBJECTIVES = 2;
    public static final double OBJECTIVE_REWARD_FRACTION = 0.40; // bonus per met objective, of payment
    public static final double RENEWAL_MET_FRACTION = 0.50;      // met/total at/above this ⇒ renewable

    // --- Financial decisions ---
    public static final int MAX_CONCURRENT_AGREEMENTS = 3;
    public static final double OBJECTIVE_DIFFICULTY_DISCOUNT = 0.15; // per objective, in offer valuation
}
