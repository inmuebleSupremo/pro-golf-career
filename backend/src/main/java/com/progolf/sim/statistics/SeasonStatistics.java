package com.progolf.sim.statistics;

import java.util.Objects;

/**
 * A golfer's competitive statistics for a single season (spec: competitive-statistics, REQ-252), preserved
 * permanently and never overwritten by later seasons. Immutable.
 */
public record SeasonStatistics(String golferId, int season, StatLine line) {

    public SeasonStatistics {
        Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(line, "line");
    }
}
