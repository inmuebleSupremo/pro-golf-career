package com.progolf.app.api.dto;

/**
 * The GraphQL view of the player's in-progress event (capability graphql-api): its display name, place, and
 * host course type (an {@code EnvironmentClassification} name), with optional procedural host details.
 * The course type drives the play screen's scene backdrop.
 */
public record CurrentEventDto(String name, String location, String courseType, String hostName, String hostRegion) {
}
