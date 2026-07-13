package com.progolf.app.api.dto;

/**
 * The GraphQL view of a resolved shot (capability graphql-api): where the ball ended ({@code finalSurface}
 * as its surface enum name), the carry and lateral it travelled, the distance remaining to the pin, whether
 * a hazard was entered, penalty strokes incurred, and total strokes for the shot. Projects
 * {@code sim.shot.ShotOutcome}; the engine's internal {@code FactorBreakdown} is not exposed.
 */
public record ShotOutcomeDto(String finalSurface, double carry, double lateral, double distanceRemaining,
                             boolean hazardEntered, int penaltyStrokes, int strokes) {
}
