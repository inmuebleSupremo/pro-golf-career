package com.progolf.app.api.dto;

/**
 * The GraphQL view of the player's Physical State (capability graphql-api): long-term fitness, accumulated
 * fatigue, the derived availability, whether they can compete / play through a recovering injury, and any
 * active injury. {@code fitness} and {@code fatigue} are in [0,1]; {@code injury} is null when healthy.
 */
public record PlayerFitnessDto(
        String availability,
        double fitness,
        double fatigue,
        boolean canCompete,
        boolean canPlayThroughInjury,
        InjuryDto injury) {
}
