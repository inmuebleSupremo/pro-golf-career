package com.progolf.app.api.dto;

/**
 * The GraphQL view of one attribute's development gain in a season (capability player-profile-api): the
 * attribute's engine name, the points gained, and the season it developed. Powers the end-of-season report.
 */
public record DevelopmentDeltaDto(String attribute, int delta, int season) {
}
