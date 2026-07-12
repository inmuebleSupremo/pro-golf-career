package com.progolf.sim.tournament;

import com.progolf.sim.shot.Strategy;

/**
 * How the scoreboard bends a golfer's strategy on the closing rounds (spec: tournament-play). A golfer well
 * behind the lead presses — plays aggressively to make up ground — while a front-runner with a comfortable
 * lead protects it by playing conservatively; everyone else, and every opening round, plays their innate
 * disposition. A pure, deterministic function of the competitive situation, so the same standings always
 * bend a golfer the same way.
 */
public final class ScoreboardStrategy {

    private ScoreboardStrategy() {
    }

    /**
     * The strategy a golfer of the given {@code disposition} plays for {@code roundNo}, given how far they
     * are behind the leader and (if they lead) their margin over the field.
     */
    public static Strategy adjust(Strategy disposition, int roundNo, int strokesBehind, int leaderMargin) {
        if (roundNo < TournamentConstants.SCOREBOARD_CLOSING_ROUND) {
            return disposition; // the disposition stands until the closing rounds
        }
        if (strokesBehind >= TournamentConstants.SCOREBOARD_PRESS_BEHIND) {
            return Strategy.AGGRESSIVE; // a chaser presses
        }
        if (strokesBehind == 0 && leaderMargin >= TournamentConstants.SCOREBOARD_PROTECT_MARGIN) {
            return Strategy.CONSERVATIVE; // a comfortable front-runner protects
        }
        return disposition;
    }
}
