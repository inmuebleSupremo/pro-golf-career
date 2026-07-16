package com.progolf.app.api.dto;

/**
 * The GraphQL view of one event on the player's season calendar (capability graphql-api): its schedule
 * details (id, week, tour {@code tier} and event {@code prestige} as enum names, entry status, name), whether
 * it has been {@code played}, and — once played — its {@code result} (null while upcoming). Joins
 * {@code sim.world.PlayerScheduleEntry} with the event's {@code TournamentResult}.
 */
public record CalendarEntryDto(long tournamentId, int week, String tier, String prestige, boolean entered,
                               String name, boolean played, EventResultDto result) {
}
