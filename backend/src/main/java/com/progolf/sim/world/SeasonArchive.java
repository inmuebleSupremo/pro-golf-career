package com.progolf.sim.world;

import com.progolf.sim.tournament.TournamentResult;
import java.util.List;
import java.util.Objects;

/**
 * A completed season's permanent record (REQ-107): its schedule and the results it produced. Archived at
 * season end and never modified.
 */
public record SeasonArchive(int season, List<ScheduledTournament> schedule, List<TournamentResult> results) {

    public SeasonArchive {
        Objects.requireNonNull(schedule, "schedule");
        Objects.requireNonNull(results, "results");
        schedule = List.copyOf(schedule);
        results = List.copyOf(results);
    }
}
