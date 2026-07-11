package com.progolf.sim.career;

import java.util.Objects;

/**
 * A permanent record of one Hall-of-Fame induction (spec: career-legacy): the golfer inducted, the season
 * the election inducted them, and the prestige-weighted score they were inducted on. Immutable.
 */
public record HallOfFameInduction(String golferId, int season, double score) {

    public HallOfFameInduction {
        Objects.requireNonNull(golferId, "golferId");
    }
}
