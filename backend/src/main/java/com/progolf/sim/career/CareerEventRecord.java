package com.progolf.sim.career;

import java.util.List;
import java.util.Objects;

/**
 * One rich, immutable record of a single event the PLAYER competed in (spec: career-records). Unlike the
 * thin {@link CareerHistoryEntry} (a dated description string kept for every golfer), this is captured only
 * for the human player, so its detail — where the event was played, the finishing score relative to par, the
 * round-by-round card, and the shot-level performance metrics — can be retained without inflating every
 * golfer's snapshot.
 *
 * <p>It answers the player-facing "career records" questions: <em>when</em> (season + week — the turn
 * coordinates, from which a realistic calendar date is derived at the read layer), <em>where</em>
 * (location), and <em>how</em> (position, scoreToPar, per-round scores, and metrics). Grouped by
 * {@link #eventName} it also reveals repetitive success — every time the player won a recurring event.
 *
 * @param eventName          the event's display name (stable across seasons for recurring tour events)
 * @param location           the event's display location (a fixed venue, or the host course's region)
 * @param prestige           the event prestige label ({@code EventPrestige.name()})
 * @param tier               the event tier label ({@code Tier.name()})
 * @param tourTier           the tour the event belongs to ({@code TourTier.name()} — PRO or DEVELOPMENT)
 * @param season             the season number this result was recorded in
 * @param week               the season week the event was played (1-based)
 * @param position           the player's finishing position (1 = win)
 * @param scoreToPar         the player's total score relative to par
 * @param won                whether the player won this event
 * @param madeCut            whether the player made the cut
 * @param prize              prize money earned
 * @param fairwaysHit        fairways hit off the tee
 * @param fairwaysPossible   driveable fairways faced
 * @param greensInRegulation greens reached in regulation
 * @param holesPlayed        holes completed in the event
 * @param putts              total putts taken
 * @param roundScores        each round's score relative to par, in order (empty when not retained)
 */
public record CareerEventRecord(String eventName, String location, String prestige, String tier,
                                String tourTier, int season, int week, int position, int scoreToPar,
                                boolean won, boolean madeCut, double prize, int fairwaysHit, int fairwaysPossible,
                                int greensInRegulation, int holesPlayed, int putts, List<Integer> roundScores) {

    public CareerEventRecord {
        Objects.requireNonNull(eventName, "eventName");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(prestige, "prestige");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(tourTier, "tourTier");
        if (position < 1) {
            throw new IllegalArgumentException("position must be >= 1: " + position);
        }
        roundScores = List.copyOf(roundScores);
    }
}
