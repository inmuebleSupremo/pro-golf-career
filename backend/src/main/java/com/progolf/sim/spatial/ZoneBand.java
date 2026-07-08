package com.progolf.sim.spatial;

import java.util.List;
import java.util.Objects;

/**
 * One distance interval {@code [startDistance, endDistance)} of a hole's reachable line, partitioned
 * laterally into ordered {@link LateralRegion}s from the centre outward (spec: hole-spatial-model).
 *
 * <p>Zone bands are the first-class contract between course generation (which produces them) and shot
 * resolution (which consumes them). Any {@code |lateral|} beyond the widest region resolves to
 * {@link Surface#OUT_OF_BOUNDS}, so every landing maps to exactly one surface.
 */
public record ZoneBand(double startDistance, double endDistance, List<LateralRegion> regions) {

    public ZoneBand {
        Objects.requireNonNull(regions, "regions");
        if (!Double.isFinite(startDistance) || !Double.isFinite(endDistance) || startDistance < 0) {
            throw new IllegalArgumentException("Invalid distance interval: [" + startDistance + "," + endDistance + ")");
        }
        if (endDistance <= startDistance) {
            throw new IllegalArgumentException("endDistance must exceed startDistance: [" + startDistance + "," + endDistance + ")");
        }
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A zone band requires at least one lateral region");
        }
        regions = List.copyOf(regions);
        double previous = 0.0;
        for (LateralRegion r : regions) {
            if (r.outerHalfWidth() <= previous) {
                throw new IllegalArgumentException("Lateral regions must have strictly increasing outerHalfWidth");
            }
            previous = r.outerHalfWidth();
        }
    }

    /** True if {@code distance} falls within this band's half-open interval. */
    public boolean contains(double distance) {
        return distance >= startDistance && distance < endDistance;
    }

    /** Resolves the surface at absolute lateral offset {@code absLateral}; beyond the widest region -&gt; OUT_OF_BOUNDS. */
    public Surface surfaceAt(double absLateral) {
        for (LateralRegion r : regions) {
            if (absLateral <= r.outerHalfWidth()) {
                return r.surface();
            }
        }
        return Surface.OUT_OF_BOUNDS;
    }
}
