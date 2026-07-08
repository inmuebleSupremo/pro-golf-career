package com.progolf.sim.shot;

import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/**
 * The complete result of a resolved shot (REQ-062). Every field is populated; nothing is left
 * undefined. {@code strokes} counts this shot plus any penalty strokes incurred.
 */
public record ShotOutcome(
        Surface finalSurface,
        double carry,
        double lateral,
        double distanceRemaining,
        boolean hazardEntered,
        int penaltyStrokes,
        int strokes,
        FactorBreakdown factors) {

    public ShotOutcome {
        Objects.requireNonNull(finalSurface, "finalSurface");
        Objects.requireNonNull(factors, "factors");
    }
}
