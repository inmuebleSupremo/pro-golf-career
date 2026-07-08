package com.progolf.sim.shot;

import com.progolf.sim.spatial.ShotZoneProfile;

/**
 * The minimal hole geometry consumed by {@link RoundResolver}. Course generation (a separate change)
 * will produce concrete implementations; this core only depends on the contract.
 *
 * <p>A hole exposes its starting distance to the pin and, for any remaining distance, the reachable
 * {@link ShotZoneProfile} for the next shot.
 */
public interface HoleModel {

    /** Distance from the tee to the pin (yards). */
    double startDistance();

    /** The reachable zone profile for a shot played from {@code remainingDistance} to the pin. */
    ShotZoneProfile zoneProfileFor(double remainingDistance);
}
