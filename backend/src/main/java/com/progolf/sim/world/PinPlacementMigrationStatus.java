package com.progolf.sim.world;

import com.progolf.sim.course.PinPlacementVersion;

/** Read-only migration state for the narrow career-management seam. */
public record PinPlacementMigrationStatus(PinPlacementVersion defaultVersion, int legacyScheduledEvents,
                                          boolean canAdoptV5) {
}
