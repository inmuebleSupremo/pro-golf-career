package com.progolf.sim.world;

import com.progolf.sim.tour.TourTier;
import java.util.Objects;

/**
 * One tournament allocated in a season schedule (REQ-101/106): the week it is played, the tour tier
 * whose members contest it, the course (by pool index) it is played on, and its unique tournament id.
 */
public record ScheduledTournament(int week, TourTier tier, int courseIndex, long tournamentId) {

    public ScheduledTournament {
        Objects.requireNonNull(tier, "tier");
        if (week < 1) {
            throw new IllegalArgumentException("week must be >= 1: " + week);
        }
    }
}
