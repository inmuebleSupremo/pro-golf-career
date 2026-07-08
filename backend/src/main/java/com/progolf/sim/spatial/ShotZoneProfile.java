package com.progolf.sim.spatial;

import java.util.List;
import java.util.Objects;

/**
 * The ordered set of {@link ZoneBand}s describing the reachable landing line for one shot context
 * (spec: hole-spatial-model). This is the sole surface authority the shot engine consumes — the
 * engine never queries 2D geometry.
 *
 * <p>Bands must fully partition the reachable range: contiguous, gap-free, starting at
 * {@code minReach}. A carry beyond {@code maxReach} is an overshoot and resolves to
 * {@link Surface#OUT_OF_BOUNDS}.
 */
public record ShotZoneProfile(List<ZoneBand> bands) {

    private static final double EPS = 1e-9;

    public ShotZoneProfile {
        Objects.requireNonNull(bands, "bands");
        if (bands.isEmpty()) {
            throw new IllegalArgumentException("A shot zone profile requires at least one band");
        }
        bands = List.copyOf(bands);
        for (int i = 1; i < bands.size(); i++) {
            double prevEnd = bands.get(i - 1).endDistance();
            double currStart = bands.get(i).startDistance();
            if (Math.abs(currStart - prevEnd) > EPS) {
                throw new IllegalArgumentException(
                        "Zone bands must be contiguous with no gaps/overlaps: gap between "
                                + prevEnd + " and " + currStart);
            }
        }
    }

    /** The nearest reachable distance covered by the profile. */
    public double minReach() {
        return bands.get(0).startDistance();
    }

    /** The farthest reachable distance covered by the profile. */
    public double maxReach() {
        return bands.get(bands.size() - 1).endDistance();
    }

    /**
     * Resolves the surface for a landing at carry {@code carryDistance} and signed lateral offset
     * {@code lateral}. Surface is read solely from the matching band — from no other source.
     */
    public Surface surfaceAt(double carryDistance, double lateral) {
        if (carryDistance < minReach() - EPS || carryDistance >= maxReach()) {
            return Surface.OUT_OF_BOUNDS;
        }
        double absLateral = Math.abs(lateral);
        for (ZoneBand band : bands) {
            if (band.contains(carryDistance)) {
                return band.surfaceAt(absLateral);
            }
        }
        return Surface.OUT_OF_BOUNDS;
    }
}
