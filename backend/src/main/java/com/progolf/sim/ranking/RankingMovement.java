package com.progolf.sim.ranking;

/**
 * A change in a golfer's ranking position between two snapshots (REQ-145). A negative delta is an
 * improvement (moving toward #1); positive is a decline.
 */
public record RankingMovement(int fromPosition, int toPosition) {

    /** Positive when the golfer improved (moved to a lower/better position number). */
    public int placesGained() {
        return fromPosition - toPosition;
    }

    public boolean improved() {
        return toPosition < fromPosition;
    }
}
