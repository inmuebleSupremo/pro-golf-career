package com.progolf.sim.course;

import java.util.Objects;

/** Stable inputs needed to reconstruct a physical green surface without persisting its samples. */
public record GreenSurfaceProvenance(GreenSurfaceVersion version, long holeSeed, long surfaceSeed,
                                     long footprintFingerprint, Position2d approachDirection) {
    public GreenSurfaceProvenance {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(approachDirection, "approachDirection");
        if (StrictMath.hypot(approachDirection.x(), approachDirection.y()) < 0.999) {
            throw new IllegalArgumentException("approach direction must be a unit vector");
        }
    }
}
