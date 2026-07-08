package com.progolf.sim.ranking;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An immutable ranking-points award earned by a golfer at a dated tournament (REQ-143/144). Awards are
 * the append-only source of truth; standings are derived from them by applying decay as of a date.
 */
public record RankingAward(String golferId, LocalDate date, double points, long tournamentId) {

    public RankingAward {
        Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(date, "date");
        if (!(points >= 0) || !Double.isFinite(points)) {
            throw new IllegalArgumentException("points must be finite and >= 0: " + points);
        }
    }
}
