package com.progolf.sim.statistics;

/**
 * The single tunables surface for the Statistics, Records &amp; Historical Archives domain (spec:
 * competitive-statistics / records-archive / historical-queries). Values live here so the definitions of
 * "top finish" and any record thresholds can be adjusted in one place.
 */
public final class StatisticsConstants {

    private StatisticsConstants() {
    }

    /** A finishing position at or better than this counts as a top-N finish. */
    public static final int TOP_N = 10;
}
