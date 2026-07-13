package com.progolf.app.api.dto;

/**
 * The GraphQL view of one event on the player's reviewable schedule (capability graphql-api): its tournament
 * id, week, tour {@code tier} and event {@code prestige} (as enum names), and whether the player is currently
 * entered. Projects {@code sim.world.PlayerScheduleEntry}; enum-valued fields are surfaced as their names.
 */
public record ScheduleEntryDto(long tournamentId, int week, String tier, String prestige, boolean entered) {
}
