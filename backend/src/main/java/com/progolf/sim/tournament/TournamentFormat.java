package com.progolf.sim.tournament;

/**
 * The competition format (REQ-086). Version 1 is a four-round stroke-play event, optionally with a cut.
 */
public record TournamentFormat(int rounds, boolean hasCut, int cutSize) {

    public TournamentFormat {
        if (rounds < 1) {
            throw new IllegalArgumentException("rounds must be >= 1: " + rounds);
        }
        if (hasCut && cutSize < 1) {
            throw new IllegalArgumentException("cutSize must be >= 1 when hasCut");
        }
    }

    /** The standard four-round event with a cut. */
    public static TournamentFormat standard() {
        return new TournamentFormat(TournamentConstants.ROUNDS, true, TournamentConstants.DEFAULT_CUT_SIZE);
    }

    /** A four-round event with no cut (whole field plays all rounds). */
    public static TournamentFormat noCut() {
        return new TournamentFormat(TournamentConstants.ROUNDS, false, 0);
    }
}
