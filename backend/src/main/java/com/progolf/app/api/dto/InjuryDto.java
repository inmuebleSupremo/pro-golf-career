package com.progolf.app.api.dto;

/**
 * The GraphQL view of an active injury (capability graphql-api): body-area type, severity, and the weeks of
 * rehabilitation remaining. Mirrors {@code sim.health.Injury} with the enums surfaced as their names.
 */
public record InjuryDto(String type, String severity, int rehabWeeksRemaining) {
}
