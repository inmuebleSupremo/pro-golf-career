package com.progolf.app.api.dto;

/**
 * The GraphQL view of one world news item (capability graphql-api): the season it occurred, its
 * {@code type} (a {@code NewsType} enum name), the headline, a prominence score, and the subject golfer's id
 * when the item is about a specific golfer (null otherwise). Projects {@code sim.media.NewsEvent}.
 */
public record NewsItemDto(int season, String type, String headline, int prominence, String subjectGolferId) {
}
