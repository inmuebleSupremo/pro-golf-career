package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of the player's own golfer (capability player-profile-api): identity, attributes,
 * world ranking, earnings, tour, and career stats — the read the client needs to show whose career this is.
 * Enum fields are surfaced as their engine names; {@code worldRanking} is null when the golfer is unranked.
 * Projects engine reads at the API edge; no simulation record appears in the schema.
 */
public record PlayerProfileDto(
        String golferId,
        String firstName,
        String lastName,
        String nationality,
        int age,
        String archetype,
        String handedness,
        Integer worldRanking,
        double careerEarnings,
        double availableFunds,
        String tour,
        int events,
        int wins,
        int topTens,
        boolean retired,
        List<AttributeValueDto> attributes) {
}
