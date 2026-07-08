package com.progolf.sim.tournament;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.Objects;

/**
 * One golfer competing in one Tournament (REQ-088). The {@code fieldIndex} is assigned when the field
 * is confirmed and is the golfer id used for deterministic seeding.
 */
public record TournamentEntry(ProfessionalGolfer golfer, int fieldIndex) {

    public TournamentEntry {
        Objects.requireNonNull(golfer, "golfer");
    }

    /** The unique player id behind this entry (used to reject duplicates). */
    public String playerId() {
        return golfer.player().id();
    }
}
