package com.progolf.app.api.dto;

/**
 * The GraphQL view of a stored saved game's metadata (capability graphql-api): the save id, the ISO-8601
 * instant it was saved, the season and week it captured, and the player's golfer id (null for an autonomous
 * world). Projects {@code com.progolf.app.persistence.SaveMetadata}.
 */
public record SaveDto(String saveId, String savedAt, int season, int week, String playerGolferId) {
}
