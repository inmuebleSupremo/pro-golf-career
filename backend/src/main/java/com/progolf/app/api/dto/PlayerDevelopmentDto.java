package com.progolf.app.api.dto;

/**
 * The GraphQL view of the player's development state (capability player-profile-api): the banked Development
 * Points available to spend, plus the cost-curve constants so the client can preview the cost of raising an
 * attribute (each +1 costs {@code pointsPerRating * (1 + max(0, rating - costReference) * costGrowth)}).
 */
public record PlayerDevelopmentDto(int points, double pointsPerRating, double costGrowth, int costReference) {
}
