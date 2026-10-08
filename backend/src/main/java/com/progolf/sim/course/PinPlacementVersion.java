package com.progolf.sim.course;

/**
 * Versioned authority for tournament flag locations. This is deliberately independent from course
 * generation: a saved course can retain its historical geometry while a future event elects to use the
 * corrected effective-green placement policy.
 */
public enum PinPlacementVersion {
    /** Historical V1--V4 raw-offset placement, retained for exact replay of existing careers. */
    LEGACY_V1,
    /** Surface-aware placement on the setup-specific effective GREEN polygon. */
    V5_EFFECTIVE_GREEN
}
