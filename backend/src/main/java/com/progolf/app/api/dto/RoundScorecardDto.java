package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of the player's current-round scorecard in a pending event (capability graphql-api): the
 * round number, the hole being played, the running score to par and total strokes over completed holes, and
 * the completed holes themselves. Projects {@code sim.play.RoundScorecard}.
 */
public record RoundScorecardDto(int roundNumber, int currentHole, int toPar, int totalStrokes,
                                List<HoleScoreDto> holes) {

    /** The player's score on one completed hole. */
    public record HoleScoreDto(int holeNumber, int par, int strokes) {
    }
}
