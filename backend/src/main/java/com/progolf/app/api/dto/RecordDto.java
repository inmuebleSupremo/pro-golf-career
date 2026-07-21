package com.progolf.app.api.dto;

/**
 * The GraphQL view of one world record (capability graphql-api): the record type, the golfer who currently
 * holds it (id + name), the value achieved, and the season it was established. Enriches
 * {@code sim.statistics.RecordHolder} with the holder's name (resolved in WorldService). The value is a count
 * for most records; for LOWEST_TOURNAMENT_SCORE it is a score-to-par.
 */
public record RecordDto(String type, String holderGolferId, String holderName, double value, int season) {
}
