package com.progolf.sim.tournament;

/**
 * A Tournament's tour tier (REQ-086) — the competitive level of the tour whose members contest it,
 * mirroring the {@code TourTier} ladder (Development / Standard / Premier / Elite). Distinct from a
 * Tournament's {@link EventPrestige} (regular / signature / major), which is an orthogonal reward weight.
 */
public enum Tier {
    DEVELOPMENT,
    STANDARD,
    PREMIER,
    ELITE
}
