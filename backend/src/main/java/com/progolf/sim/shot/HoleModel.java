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

    /** The hole's par. Used by the decision policy (e.g. only a par 5 offers a lay-up); defaults to 4. */
    default int par() {
        return 4;
    }

    /**
     * The pin's lateral offset from the green's centre line (yards; signed). A tucked pin sits toward a
     * green edge, so attacking it risks the flanking hazard while aiming at centre plays safe. Defaults to
     * a centre pin (0) for holes that do not model pin placement.
     */
    default double pinLateral() {
        return 0.0;
    }
}
