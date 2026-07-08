package com.progolf.sim.tournament;

/**
 * The entry requirements for a Tournament (REQ-089). Kept intentionally small for a standalone event:
 * a maximum field size and whether an ACTIVE career status is required. Tour-membership eligibility
 * arrives with the Tour domain.
 */
public record EntryRequirements(int maxFieldSize, boolean requiresActiveStatus) {

    public EntryRequirements {
        if (maxFieldSize < 1) {
            throw new IllegalArgumentException("maxFieldSize must be >= 1: " + maxFieldSize);
        }
    }

    /** Default: standard field size, active status required. */
    public static EntryRequirements standard() {
        return new EntryRequirements(TournamentConstants.DEFAULT_FIELD_SIZE, true);
    }
}
