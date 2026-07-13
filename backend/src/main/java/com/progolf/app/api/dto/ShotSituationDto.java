package com.progolf.app.api.dto;

/**
 * The GraphQL view of the current shot situation in a paused player event (capability graphql-api): the hole
 * and its par, the shot number and strokes so far on the hole, the distance to the pin, the current
 * {@code lie} (as its surface enum name), the pin's lateral offset, and the reachable distance window
 * ({@code minReach}/{@code maxReach}). Projects {@code sim.play.ShotSituation}; the full reachable zone
 * geometry (needed only to make a shot decision) is deferred to the mutation slice.
 */
public record ShotSituationDto(int holeNumber, int par, int shotNumber, int strokesThisHole,
                               double distanceToPin, String lie, double pinLateral,
                               double minReach, double maxReach) {
}
