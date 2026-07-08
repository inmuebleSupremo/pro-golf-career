package com.progolf.sim.spatial;

import java.util.Objects;

/**
 * A lateral sub-region of a {@link ZoneBand}, occupying {@code |lateral| <= outerHalfWidth} (outward
 * from the previous region) with a single {@link Surface}. The {@code weight} informs course
 * generation's landing distributions; shot resolution selects a surface purely by geometry.
 */
public record LateralRegion(double outerHalfWidth, Surface surface, double weight) {

    public LateralRegion {
        Objects.requireNonNull(surface, "surface");
        if (!(outerHalfWidth > 0) || !Double.isFinite(outerHalfWidth)) {
            throw new IllegalArgumentException("outerHalfWidth must be finite and > 0: " + outerHalfWidth);
        }
        if (!(weight >= 0) || !Double.isFinite(weight)) {
            throw new IllegalArgumentException("weight must be finite and >= 0: " + weight);
        }
    }

    /** Convenience constructor with unit weight. */
    public LateralRegion(double outerHalfWidth, Surface surface) {
        this(outerHalfWidth, surface, 1.0);
    }
}
