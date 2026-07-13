package com.progolf.app.api.dto;

/**
 * The GraphQL view of one live leaderboard row in a player's event (capability graphql-api): the shared
 * position, the competitor (as a {@link GolferDto}), the cumulative score relative to par, and rounds
 * completed. Projects {@code sim.tournament.LeaderboardEntry}, replacing its raw golfer object with a DTO.
 */
public record LeaderboardRowDto(int position, GolferDto golfer, int score, int roundsPlayed) {
}
