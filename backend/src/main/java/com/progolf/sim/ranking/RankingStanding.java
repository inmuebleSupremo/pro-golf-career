package com.progolf.sim.ranking;

import java.util.Objects;

/**
 * A read-only row of the World Ranking (REQ-141/146): a golfer's position and ranking value as of a
 * date. Dependent systems consume this without touching the ledger.
 */
public record RankingStanding(int position, String golferId, double rankingValue) {

    public RankingStanding {
        Objects.requireNonNull(golferId, "golferId");
        if (position < 1) {
            throw new IllegalArgumentException("position must be >= 1: " + position);
        }
    }
}
