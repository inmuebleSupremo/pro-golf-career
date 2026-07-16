package com.progolf.app.api.dto;

/**
 * The GraphQL view of the player's competitive statistics for a single season (capability graphql-api): the
 * season and its headline counts — events, wins, top-tens, cuts made, best finish, and earnings. Projects a
 * season {@code sim.statistics.SeasonStatistics} / {@code StatLine}.
 */
public record SeasonStatDto(int season, int events, int wins, int topTens, int cuts, int bestFinish,
                            double earnings) {
}
