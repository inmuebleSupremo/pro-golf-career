package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of the player's career records (spec: career-records) — the player-specific counterpart to
 * the world Record Book. Aggregates the player's rich per-event ledger into a headline {@link Summary} and a
 * per-event history, so the surface can show career totals, scoring records, and — grouped by event — the
 * player's historical dominance of recurring tournaments.
 *
 * @param summary headline career totals and scoring records
 * @param events  one entry per distinct event the player has competed in (prestige-then-result ordered)
 */
public record CareerRecordsDto(Summary summary, List<EventHistory> events) {

    /**
     * Headline career records. Counts come from the career statistics; the scoring bests come from the
     * per-event ledger and carry their full context (which round/tournament, and when) so the UI can reveal
     * it — null when the player has no qualifying result yet.
     */
    public record Summary(int events, int wins, int majors, int runnerUps, int topTens, int cutsMade,
                          Integer bestFinish, ScoringHighlight lowestRound, ScoringHighlight lowestTournament,
                          double careerEarnings) {
    }

    /**
     * A standout scoring mark with the context that makes it a story: the score, where and when it happened,
     * and (for a single-round record) which round.
     *
     * @param scoreToPar the score relative to par
     * @param eventName  the event it was set in
     * @param location   where it was played
     * @param season     the season it was set
     * @param date       ISO calendar date the event finished
     * @param round      the round number (1-based) for a single-round mark; null for a tournament total
     */
    public record ScoringHighlight(int scoreToPar, String eventName, String location, int season, String date,
                                   Integer round) {
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
     * @param tourTier     the tour the event belongs to (PRO or DEVELOPMENT) — the section it groups under
     * @param appearances  how many times the player competed in this event
     * @param wins         how many times the player won it
     * @param bestPosition the best finishing position achieved here
     * @param results      every result, ordered best-first (position, then score)
     */
    public record EventHistory(String eventName, String location, String prestige, String tier, String tourTier,
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
