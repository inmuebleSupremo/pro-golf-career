package com.progolf.app.api.dto;

import java.util.List;

/**
 * The GraphQL view of the current shot situation in a paused player event (capability graphql-api): the hole
 * and its par, the shot number and strokes so far on the hole, the distance to the pin, the current
 * {@code lie} (as its surface enum name), the pin's lateral offset, the reachable distance window
 * ({@code minReach}/{@code maxReach}), and the reachable surface profile ({@code reachable}) — the ordered
 * distance bands of surface the shot could find, for a truthful shot-preview overlay. Projects
 * {@code sim.play.ShotSituation}.
 */
public record ShotSituationDto(int holeNumber, int par, int shotNumber, int strokesThisHole,
                               double distanceToPin, String lie, double pinLateral,
                               double minReach, double maxReach, List<SurfaceBandDto> reachable) {
}
