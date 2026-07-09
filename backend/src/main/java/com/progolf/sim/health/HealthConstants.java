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

    // --- Fitness (seeded per golfer, long-term preparedness) ---
    public static final double FITNESS_MEAN = 0.62;
    public static final double FITNESS_SPREAD = 0.15;
    public static final double FITNESS_MIN = 0.25;
    public static final double FITNESS_MAX = 1.0;

    // --- Fatigue accrual (per event) ---
    public static final double FATIGUE_PER_EVENT = 0.20;
    public static final double FITNESS_FATIGUE_FACTOR = 0.60;   // unfit golfers tire faster
    public static final double AGE_FATIGUE_PER_YEAR = 0.02;     // beyond the reference age

    // --- Recovery (per rested week) ---
    public static final double RECOVERY_PER_WEEK = 0.11;
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
