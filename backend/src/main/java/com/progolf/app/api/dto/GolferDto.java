package com.progolf.app.api.dto;

/**
 * The GraphQL view of a golfer (capability graphql-api): just the identity a client needs to render a row —
 * the stable id and the full name. Projects {@code sim.player.ProfessionalGolfer} at the API edge so the
 * engine record never appears in the schema.
 */
public record GolferDto(String id, String name) {
}
