package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of the player's career records (spec: career-records) — the player-specific counterpart to
 * the world Record Book. Aggregates the player's rich per-event ledger into a headline {@link Summary} and a
 * per-event history, so the surface can show career totals, scoring records, and — grouped by event — the
 * player's historical dominance of recurring tournaments.
 *
 * @param summary headline career totals and scoring records
 * @param events  one entry per distinct event the player has competed in, most-decorated first
 */
public record CareerRecordsDto(Summary summary, List<EventHistory> events) {

    /**
     * Headline career records. Counts come from the career statistics; the scoring bests and best finish come
     * from the per-event ledger (null when the player has no counting result yet).
     */
    public record Summary(int events, int wins, int majors, int runnerUps, int topTens, int cutsMade,
                          Integer bestFinish, Integer lowestRoundToPar, Integer lowestTournamentToPar,
                          double careerEarnings) {
    }

    /**
     * The player's complete history in one event, grouped by the event's stable name so recurring wins
     * accumulate. Holds every result the player recorded there, best-first, for both the top-results preview
     * and the full victory deep-dive.
     *
     * @param eventName    the event's display name
     * @param location     the event's most recent display location
     * @param prestige     the event prestige label
     * @param tier         the event tier label
     * @param appearances  how many times the player competed in this event
     * @param wins         how many times the player won it
     * @param bestPosition the best finishing position achieved here
     * @param results      every result, ordered best-first (position, then score)
     */
    public record EventHistory(String eventName, String location, String prestige, String tier,
                               int appearances, int wins, int bestPosition, List<Result> results) {
    }

    /**
     * One of the player's results in an event — the when/where/how of a single appearance.
     *
     * @param season             the season it was played
     * @param date               ISO calendar date the event finished
     * @param position           finishing position (1 = win)
     * @param scoreToPar         total score relative to par
     * @param won                whether the player won
     * @param madeCut            whether the player made the cut
     * @param location           where it was played
     * @param prize              prize money earned
     * @param fairwaysHit        fairways hit
     * @param fairwaysPossible   driveable fairways faced
     * @param greensInRegulation greens in regulation
     * @param holesPlayed        holes completed
     * @param putts              total putts
     * @param roundScores        each round's score relative to par, in order (empty when not retained)
     */
    public record Result(int season, String date, int position, int scoreToPar, boolean won, boolean madeCut,
                         String location, double prize, int fairwaysHit, int fairwaysPossible,
                         int greensInRegulation, int holesPlayed, int putts, List<Integer> roundScores) {
    }
}
