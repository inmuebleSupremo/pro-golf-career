package com.progolf.sim.play;

import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;

/**
 * The information a human needs to decide the current shot (spec: playable-round): which hole and its par,
 * the shot number and strokes so far on the hole, the distance to the pin, the current lie, the pin's
 * lateral offset from the green centre (so the player can choose to attack a tucked flag or play safe), and
 * the reachable {@link ShotZoneProfile} (the surfaces and hazards the shot could find). The player responds
 * with a club/target/risk decision. Immutable.
 */
public record ShotSituation(int holeNumber, int par, int shotNumber, int strokesThisHole,
                            double distanceToPin, Surface lie, double pinLateral, ShotZoneProfile reachable) {
}
