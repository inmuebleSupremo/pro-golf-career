package com.progolf.sim.statistics;

import java.util.Objects;

/**
 * One golfer's outcome in one tournament, as observed by the archive (spec: competitive-statistics). All
 * fields come straight from a real result: the season, the golfer, their finishing position, cumulative
 * score relative to par, whether they made the cut, whether they withdrew, and prize money. Immutable.
 */
public record EventOutcome(int season, String golferId, int position, int scoreVsPar,
                           boolean madeCut, boolean withdrawn, double prize,
                           int fairwaysHit, int fairwaysPossible, int greensInRegulation, int holesPlayed,
                           int putts) {

    public EventOutcome {
        Objects.requireNonNull(golferId, "golferId");
        if (position < 1) {
            throw new IllegalArgumentException("position must be >= 1: " + position);
        }
        if (prize < 0 || !Double.isFinite(prize)) {
            throw new IllegalArgumentException("prize must be finite and >= 0: " + prize);
        }
    }

    /** Convenience for an outcome with no recorded shot statistics (all zero). */
    public EventOutcome(int season, String golferId, int position, int scoreVsPar, boolean madeCut,
                        boolean withdrawn, double prize) {
        this(season, golferId, position, scoreVsPar, madeCut, withdrawn, prize, 0, 0, 0, 0, 0);
    }

    /** Whether this outcome counts toward competitive statistics (a completed, non-withdrawn appearance). */
    public boolean counts() {
        return !withdrawn;
    }
}
