package com.progolf.sim.population;

/** A created golfer's starting accomplishment; Playing Style still determines their potential and shape. */
public enum StartingBackground {
    CLUB_CHAMPION(-3),
    ELITE_AMATEUR(0),
    HIGHLY_RATED_PROSPECT(6);

    private final int initialRatingOffset;

    StartingBackground(int initialRatingOffset) {
        this.initialRatingOffset = initialRatingOffset;
    }

    public int initialRatingOffset() {
        return initialRatingOffset;
    }
}
