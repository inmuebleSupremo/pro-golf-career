package com.progolf.app.api.dto;

/**
 * The GraphQL view of one World Ranking row (capability graphql-api): a golfer's position, id, name, and
 * decayed ranking value as of now. Enriches {@code sim.ranking.RankingStanding} with the golfer's name
 * (resolved in WorldService).
 */
public record RankingRowDto(int position, String golferId, String name, double rankingValue) {
}
