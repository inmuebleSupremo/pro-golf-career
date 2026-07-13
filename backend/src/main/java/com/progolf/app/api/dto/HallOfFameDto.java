package com.progolf.app.api.dto;

/**
 * The GraphQL view of one Hall-of-Fame induction (capability graphql-api): the inducted golfer's id, the
 * season the election inducted them, and the prestige-weighted score. Projects
 * {@code sim.career.HallOfFameInduction}.
 */
public record HallOfFameDto(String golferId, int season, double score) {
}
