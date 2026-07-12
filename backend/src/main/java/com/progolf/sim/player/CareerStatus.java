package com.progolf.sim.player;

import java.util.Map;
import java.util.Set;

/**
 * The career status of a Player (REQ-022). Exactly one status is active at a time and only the defined
 * transitions are permitted; RETIRED and DECEASED are terminal for gameplay. An injury does not change
 * career status — a hurt golfer stays ACTIVE, and the transient inability to compete is expressed by the
 * health domain's {@code Availability}, not here.
 */
public enum CareerStatus {
    CREATED,
    ACTIVE,
    RETIRED,
    DECEASED;

    private static final Map<CareerStatus, Set<CareerStatus>> ALLOWED = Map.of(
            CREATED, Set.of(ACTIVE, DECEASED),
            ACTIVE, Set.of(RETIRED, DECEASED),
            RETIRED, Set.of(DECEASED),
            DECEASED, Set.of());

    /** Whether a transition from this status to {@code target} is permitted. */
    public boolean canTransitionTo(CareerStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    /** True if no further transitions (other than none) are possible from a gameplay standpoint. */
    public boolean isTerminal() {
        return this == RETIRED || this == DECEASED;
    }
}
