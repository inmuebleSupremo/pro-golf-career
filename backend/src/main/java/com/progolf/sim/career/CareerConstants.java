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

    // --- Placeholder Hall-of-Fame thresholds (refined later by the Legacy spec) ---
    /** A career is Hall-of-Fame eligible if it has at least this many wins ... */
    public static final int HOF_MIN_WINS = 15;
    /** ... or at least this many wins together with strong consistency (top-10s). */
    public static final int HOF_ALT_WINS = 8;
    public static final int HOF_ALT_TOP_10S = 40;
    /** ... or at least this many majors won — the marquee accomplishment (spec: event-prestige). */
    public static final int HOF_MIN_MAJORS = 3;
}
