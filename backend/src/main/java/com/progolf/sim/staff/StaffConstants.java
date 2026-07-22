package com.progolf.sim.staff;

/**
 * The single tunables surface for the Support Team &amp; Professional Staff domain (spec: support-team /
 * staff-relationships / staff-influence). All magnitudes live here so staff economics and influence can be
 * calibrated in one place. Costs are in the same currency units as prize money and expenses.
 */
public final class StaffConstants {

    private StaffConstants() {
    }

    /**
     * Distinct salt mixed into the per-golfer/season/role staff seed so staff generation is isolated from
     * the shot, weather, economy, and health streams. A large prime, different from the others.
     */
    public static final long STAFF_SALT = 333_333_331L;

    // --- Candidate demographics (descriptive; no gameplay effect) ---
    public static final int STAFF_MIN_AGE = 34;
    public static final int STAFF_MAX_AGE = 64;

    // --- Hire pool (spec: support-team) ---
    public static final int POOL_PER_ROLE = 10;   // profiles generated per role → ~50 across the world
    public static final int OFFERS_PER_SEASON = 6; // candidates surfaced to the player each season

    // --- Quality (seeded per candidate) ---
    public static final double QUALITY_MEAN = 0.60;
    public static final double QUALITY_SPREAD = 0.18;
    public static final double QUALITY_MIN = 0.30;
    public static final double QUALITY_MAX = 1.00;

    // --- Costs (scaled by quality off the role's base salary) ---
    public static final double SALARY_QUALITY_FLOOR = 0.60;  // salary = base * (floor + quality*span)
    public static final double SALARY_QUALITY_SPAN = 0.80;
    public static final double HIRING_COST_FRACTION = 0.50;  // hiring cost as a fraction of seasonal salary

    // --- Influence (per unit of quality, per employed member of the role) ---
    public static final double COACH_DEVELOPMENT_PER_QUALITY = 0.25;   // scales awarded Development Points
    public static final double FITNESS_RECOVERY_PER_QUALITY = 0.04;    // extra weekly fatigue recovery
    public static final double FITNESS_CONDITIONING_PER_QUALITY = 0.12; // lifts the seasonal fitness target
    public static final double PHYSIO_RECOVERY_PER_QUALITY = 0.05;
    public static final double PSYCH_MENTAL_PER_QUALITY = 0.20;        // mental support: softens fatigue in shots
    public static final double CADDIE_STRATEGIC_PER_QUALITY = 0.20;    // strategic support: reduces mishits

    // --- Hiring policy ---
    public static final int TARGET_TEAM_DEVELOPMENT = 1;  // young golfers prioritise a coach
    public static final int TARGET_TEAM_PRIME = 3;
    public static final int TARGET_TEAM_LATE = 3;
    public static final int DEVELOPMENT_STAGE_MAX_AGE = 24; // < 25 develops, < 35 prime, else late
    public static final int PRIME_STAGE_MAX_AGE = 34;

    // --- Release under financial pressure ---
    public static final double RELEASE_THRESHOLD = 0.0;   // release the costliest member while funds are below this
}
