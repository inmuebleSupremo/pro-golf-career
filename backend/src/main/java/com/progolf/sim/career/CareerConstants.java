package com.progolf.sim.career;

/**
 * The single tunables surface for the Career domain (mirrors {@code SimConstants}). The Hall-of-Fame
 * thresholds are placeholders — the real criteria are refined by a later Legacy Systems specification.
 */
public final class CareerConstants {

    private CareerConstants() {
    }

    /** Careers may begin between these ages, inclusive. */
    public static final int MIN_START_AGE = 16;
    public static final int MAX_START_AGE = 22;

    /** Mandatory retirement age. */
    public static final int RETIREMENT_AGE = 65;

    /** A finish at or better than this counts as a top-10. */
    public static final int TOP_10 = 10;

    // --- Hall of Fame: two-phase system (spec: career-legacy) ---
    // Phase 1 — baseline eligibility (ballot qualification): a golfer is nominable only if they meet BOTH
    // a status condition AND a statistical baseline.
    /** Status: competitive age at or above which a golfer is old enough to be nominable. */
    public static final int HOF_MIN_AGE = 45;
    /** Status: seasons retired at or above which a golfer is nominable regardless of age. */
    public static final int HOF_RETIRED_SEASONS = 3;
    /** Statistical baseline: minimum professional-tour wins (development-tier wins excluded). */
    public static final int HOF_MIN_PRO_WINS = 15;
    /** Statistical baseline: minimum majors won — one felt too shallow for the Hall. */
    public static final int HOF_MIN_MAJORS = 2;

    // Phase 2 — biennial election: score a career by achievement prestige, favouring the biggest events.
    /** Election is held once every this many seasons; only the top candidate is inducted per cycle. */
    public static final int HOF_ELECTION_CYCLE_SEASONS = 2;
    /** Maximum inductees per election cycle (kept small to preserve prestige). */
    public static final int HOF_INDUCTEES_PER_CYCLE = 1;
    /** Score weight for a major — the marquee accomplishment, weighted far above everything else. */
    public static final double HOF_SCORE_MAJOR = 12.0;
    /** Score weight for a high-importance (signature) event win. */
    public static final double HOF_SCORE_SIGNATURE = 4.0;
    /** Score weight for a regular professional win (non-major, non-signature, above development tier). */
    public static final double HOF_SCORE_REGULAR = 2.0;
    /** Score weight for a development-tier (amateur) win — the lightest credential. */
    public static final double HOF_SCORE_DEVELOPMENT = 0.5;

    // Ranking dominance + earnings add to the score (never to the baseline): a golfer who reigned at the
    // top of the world outscores a compiler of the same win total.
    /** Career-high ranking value used when a golfer never held a ranked position (no peak bonus). */
    public static final int HOF_UNRANKED = 1000;
    /** Career-high position at or better than which a peak bonus applies (linearly, most at #1). */
    public static final int HOF_RANK_PEAK_CAP = 10;
    /** Peak bonus for reaching World #1 (fading linearly to zero at the cap). */
    public static final double HOF_SCORE_RANK_PEAK = 15.0;
    /** Score weight per season finishing at World #1 — sustained dominance. */
    public static final double HOF_SCORE_SEASON_AT_ONE = 8.0;
    /** Score weight per $1M of career earnings — a small consistency/longevity credential. */
    public static final double HOF_SCORE_EARNINGS_PER_MILLION = 0.3;
}
