package com.progolf.app.api.dto;

/**
 * The GraphQL view of one Hall-of-Fame induction (capability graphql-api): the inducted golfer's id and
 * name, the season the election inducted them, the prestige-weighted score, and their career wins. Enriches
 * {@code sim.career.HallOfFameInduction} with the golfer's name and win count (resolved in WorldService).
 */
public record HallOfFameDto(String golferId, String name, int season, double score, int careerWins) {
}
