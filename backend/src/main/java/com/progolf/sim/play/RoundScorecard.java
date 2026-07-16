package com.progolf.sim.play;

import java.util.List;

/**
 * A read-only view of the player's current round in a playable event (spec: playable-event): which round it
 * is, the hole currently being played, the score on each completed hole, and the running totals over those
 * completed holes. Feeds the play surface's scorecard; immutable.
 */
public record RoundScorecard(int roundNumber, int currentHole, int scoreToPar, int totalStrokes,
                             List<HoleScore> holes) {

    public RoundScorecard {
        holes = List.copyOf(holes);
    }

    /** The player's score on one completed hole. */
    public record HoleScore(int holeNumber, int par, int strokes) {
    }
}
