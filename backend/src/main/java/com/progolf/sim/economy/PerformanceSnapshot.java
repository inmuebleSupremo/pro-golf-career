package com.progolf.sim.economy;

/**
 * A season's competitive performance, supplied by the World from career/ranking state so the Economy can
 * evaluate sponsorship objectives without importing those domains (spec: sponsorship, REQ-180/185/190).
 * Positions use {@link Integer#MAX_VALUE} to mean "none / unranked".
 */
public record PerformanceSnapshot(
        int seasonEvents,
        int seasonWins,
        int seasonMadeCuts,
        int seasonBestPosition,
        int seasonRankingPosition,
        int careerWins) {

    /** Fraction of played events in which the golfer made the cut ([0,1]; 0 when no events played). */
    public double consistency() {
        return seasonEvents > 0 ? (double) seasonMadeCuts / seasonEvents : 0.0;
    }
}
