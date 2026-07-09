package com.progolf.sim.statistics;

import java.util.Objects;

/**
 * A read-only comparison of two golfers' career statistics (spec: historical-queries, REQ-259). Computed
 * from archived stat lines without modifying them. Leader helpers return the leading golfer id (ties favour
 * the first golfer). Immutable.
 */
public record CareerComparison(String golferA, StatLine a, String golferB, StatLine b) {

    public CareerComparison {
        Objects.requireNonNull(golferA, "golferA");
        Objects.requireNonNull(golferB, "golferB");
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
    }

    /** The golfer with more career wins (ties favour A). */
    public String moreWins() {
        return a.wins() >= b.wins() ? golferA : golferB;
    }

    /** The golfer with higher career earnings (ties favour A). */
    public String higherEarnings() {
        return a.earnings() >= b.earnings() ? golferA : golferB;
    }

    /** The golfer with the better (lower) career scoring average, considering only those with events. */
    public String betterScoringAverage() {
        if (a.events() == 0) {
            return golferB;
        }
        if (b.events() == 0) {
            return golferA;
        }
        return a.scoringAverage() <= b.scoringAverage() ? golferA : golferB;
    }
}
