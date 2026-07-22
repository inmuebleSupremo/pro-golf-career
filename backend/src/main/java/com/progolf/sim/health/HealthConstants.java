package com.progolf.sim.health;

/**
 * The single tunables surface for the Health, Fitness &amp; Recovery domain (spec: physical-state /
 * injury-recovery / availability). All magnitudes live here so durability and availability rates can be
 * calibrated in one place. Fatigue and fitness are normalized to [0,1]; rehabilitation is measured in
 * weeks (the world's turn).
 */
public final class HealthConstants {

    private HealthConstants() {
    }

    /**
     * Distinct salt mixed into the per-golfer/season/week health seed so injury rolls are isolated from
     * the shot, weather, and economy streams. A large prime, different from the other domain salts.
     */
    public static final long HEALTH_SALT = 777_777_773L;

    // --- Fitness (seeded per golfer, then evolving over a career) ---
    public static final double FITNESS_MEAN = 0.62;
    public static final double FITNESS_SPREAD = 0.15;
    public static final double FITNESS_MIN = 0.25;
    public static final double FITNESS_MAX = 1.0;

    // --- Fitness evolution (per season): drifts toward an age-based target, lifted by a fitness coach.
    // The peak sits near the seeded mean so the population's overall conditioning is roughly unchanged;
    // young golfers build up to it, veterans decline from it. Gradual and deterministic (no RNG).
    public static final int FITNESS_PRIME_AGE = 30;             // physical peak; decline begins beyond it
    public static final double FITNESS_AGE_PEAK = 0.66;         // natural conditioning target in the prime
    public static final double FITNESS_DECLINE_PER_YEAR = 0.02; // target lost per year past the prime
    public static final double FITNESS_DRIFT_PER_SEASON = 0.30; // fraction of the gap to target closed a season
    public static final double FITNESS_CONDITIONING_CAP = 0.14; // most a fitness coach can raise the target

    // --- Fatigue accrual (per event) ---
    /**
     * Fatigue added by competing. Sized against {@link #RECOVERY_PER_WEEK} and the season's shape: a golfer
     * playing a full schedule (~14 events over ~30 weeks) must be able to recover between them. At 0.20
     * against a 0.11 recovery this was unpayable by construction — fatigue only ever climbed, every golfer
     * pinned against {@link #REST_THRESHOLD}, and the whole tour played every event exhausted (worth ~9
     * strokes a round) while being forced to sit events out to shed it.
     */
    public static final double FATIGUE_PER_EVENT = 0.12;
    public static final double FITNESS_FATIGUE_FACTOR = 0.60;   // unfit golfers tire faster
    public static final double AGE_FATIGUE_PER_YEAR = 0.02;     // beyond the reference age

    // --- Recovery (per rested week) ---
    /** Recovery from a rested week. A busy run still builds fatigue; a rest week must meaningfully clear it. */
    public static final double RECOVERY_PER_WEEK = 0.16;
    public static final double AGE_RECOVERY_PENALTY_PER_YEAR = 0.01; // capped below
    public static final double AGE_RECOVERY_PENALTY_CAP = 0.50;

    // --- Availability ---
    public static final double REST_THRESHOLD = 0.85;          // fatigue at/above ⇒ forced rest

    // Age beyond which age begins to worsen fatigue, recovery, and injury risk.
    public static final int REFERENCE_AGE = 30;

    // --- Injury ---
    public static final double BASE_INJURY_CHANCE = 0.015;     // per event, at reference conditions
    public static final double FATIGUE_INJURY_FACTOR = 1.50;   // fatigue raises injury risk
    public static final double FITNESS_INJURY_FACTOR = 1.00;   // low fitness raises injury risk
    public static final double AGE_INJURY_PER_YEAR = 0.03;     // age raises injury risk

    // Severity mix (cumulative thresholds on a [0,1] roll): < MINOR ⇒ MINOR, < MODERATE ⇒ MODERATE, else SEVERE.
    public static final double SEVERITY_MINOR_CEILING = 0.60;
    public static final double SEVERITY_MODERATE_CEILING = 0.90;

    // An injury with this many rehab weeks left or fewer is "recovering" rather than "injured".
    public static final int RECOVERING_WEEKS_THRESHOLD = 2;
}
