package com.progolf.sim.play;

import com.progolf.sim.weather.PlayingConditions;
import java.util.List;
import java.util.Objects;

/**
 * A record of one round the player completed in an event (spec: career-achievements): its shot-by-shot
 * holes, the conditions it was played in, and the player's standing on the field leaderboard once the whole
 * field had finished that round. Retained by {@link PlayableEvent} so achievement detection can judge
 * round-level feats (a bogey-free card, a round shot in a storm) and event-level drama (leading wire-to-wire,
 * charging from behind on the final day). Immutable.
 *
 * @param roundNumber        the 1-based round number (1..4)
 * @param holes              the completed holes with full shot detail
 * @param conditions         the weather the round was played in
 * @param positionAfter      the player's leaderboard position after this round (1 = leading or tied for lead)
 * @param strokesBehindAfter the player's strokes behind the leader after this round (0 when leading)
 */
public record PlayerRoundRecord(int roundNumber, List<PlayedHole> holes, PlayingConditions conditions,
                                int positionAfter, int strokesBehindAfter) {

    public PlayerRoundRecord {
        Objects.requireNonNull(holes, "holes");
        Objects.requireNonNull(conditions, "conditions");
        holes = List.copyOf(holes);
    }

    /** Whether the player led (or shared the lead) after this round. */
    public boolean led() {
        return positionAfter == 1;
    }

    /** The round's score relative to par, summed over its holes. */
    public int scoreVsPar() {
        int total = 0;
        for (PlayedHole h : holes) {
            total += h.scoreVsPar();
        }
        return total;
    }
}
