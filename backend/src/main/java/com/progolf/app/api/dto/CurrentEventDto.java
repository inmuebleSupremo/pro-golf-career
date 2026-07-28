package com.progolf.app.api.dto;

/**
 * The GraphQL view of the player's in-progress event (capability graphql-api): its display name, place, and
 * host course type (an {@code EnvironmentClassification} name). Presentation context for the play screen —
 * the course type drives the scene backdrop.
 */
public record CurrentEventDto(String name, String location, String courseType) {
}
