package com.progolf.sim.world;

import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tour.TourTier;
import java.util.Objects;

/**
 * One tournament allocated in a season schedule (REQ-101/106): the week it is played, the tour tier whose
 * members contest it (for a major, the tier of its base competitive level — its field is cross-tour), the
 * course (by pool index) it is played on, its {@link EventPrestige}, and its unique tournament id.
 */
public record ScheduledTournament(int week, TourTier tier, int courseIndex, EventPrestige prestige,
                                  long tournamentId) {

    public ScheduledTournament {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(prestige, "prestige");
        if (week < 1) {
            throw new IllegalArgumentException("week must be >= 1: " + week);
        }
    }

    /** A regular tour event of the given tier (the common case). */
    public ScheduledTournament(int week, TourTier tier, int courseIndex, long tournamentId) {
        this(week, tier, courseIndex, EventPrestige.REGULAR, tournamentId);
    }
}
