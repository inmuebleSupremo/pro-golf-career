package com.progolf.sim.career;

/**
 * How a career win is classified for legacy / Hall-of-Fame weighting (spec: career-legacy). A win is
 * bucketed by priority — a major first, else a high-importance (signature) event, else a development-tier
 * (amateur) win, else a regular professional win — so the buckets are disjoint and regular pro wins are
 * derivable. {@link #NONE} is used for a non-winning finish (the category is then ignored).
 */
public enum WinCategory {
    MAJOR,
    SIGNATURE,
    REGULAR,
    DEVELOPMENT,
    NONE
}
