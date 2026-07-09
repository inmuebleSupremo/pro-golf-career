package com.progolf.sim.statistics;

/**
 * The world records recognised by the archive (spec: records-archive, REQ-254). Each declares whether a
 * higher value is better, so the record book knows what "surpassed" means. Lowest tournament score is the
 * only record where a lower value wins.
 */
public enum RecordType {
    MOST_CAREER_WINS(true),
    LOWEST_TOURNAMENT_SCORE(false),
    MOST_CONSECUTIVE_CUTS(true),
    LONGEST_CAREER(true);

    private final boolean higherIsBetter;

    RecordType(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }

    /** Whether {@code candidate} surpasses {@code current} for this record. */
    public boolean surpasses(double candidate, double current) {
        return higherIsBetter ? candidate > current : candidate < current;
    }
}
