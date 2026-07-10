package com.progolf.sim.tournament;

import com.progolf.sim.course.Course;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The immutable definition of a Tournament (REQ-086): what it is and where/when it is played, plus the
 * seed coordinates that make its play reproducible. The definition never changes once play begins.
 */
public record TournamentDefinition(
        String name,
        Course course,
        Tier tier,
        EventPrestige prestige,
        EntryRequirements entryRequirements,
        PrizeStructure prizeStructure,
        TournamentFormat format,
        LocalDate scheduledDate,
        long worldSeed,
        long seasonId,
        long tournamentId) {

    public TournamentDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(course, "course");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(prestige, "prestige");
        Objects.requireNonNull(entryRequirements, "entryRequirements");
        Objects.requireNonNull(prizeStructure, "prizeStructure");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(scheduledDate, "scheduledDate");
    }

    /**
     * Convenience constructor for a Regular event (spec: event-prestige) — keeps callers and tests that
     * predate event prestige compiling and byte-identical, since Regular is the neutral reward weight.
     */
    public TournamentDefinition(String name, Course course, Tier tier, EntryRequirements entryRequirements,
                                PrizeStructure prizeStructure, TournamentFormat format, LocalDate scheduledDate,
                                long worldSeed, long seasonId, long tournamentId) {
        this(name, course, tier, EventPrestige.REGULAR, entryRequirements, prizeStructure, format,
                scheduledDate, worldSeed, seasonId, tournamentId);
    }
}
