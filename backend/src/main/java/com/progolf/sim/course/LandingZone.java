package com.progolf.sim.course;

import java.util.Objects;

/** Immutable route-relative V3 landing intent. */
public record LandingZone(LandingZoneRole role, double routeDistance, double centreOffset, double depth,
                          double halfWidth, PreferredApproachSide preferredApproachSide,
                          ReferenceCarryBand referenceCarryBand) {
    public LandingZone {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(preferredApproachSide, "preferredApproachSide");
        Objects.requireNonNull(referenceCarryBand, "referenceCarryBand");
        if (!Double.isFinite(routeDistance) || routeDistance <= 0.0) {
            throw new IllegalArgumentException("routeDistance must be finite and > 0");
        }
        if (!Double.isFinite(centreOffset)) {
            throw new IllegalArgumentException("centreOffset must be finite");
        }
        if (!Double.isFinite(depth) || depth <= 0.0 || !Double.isFinite(halfWidth) || halfWidth <= 0.0) {
            throw new IllegalArgumentException("landing-zone dimensions must be finite and positive");
        }
    }

    /** Canonical centre of this semantic zone. */
    public Position2d center(HoleRoute route) {
        Objects.requireNonNull(route, "route");
        return route.pointAt(routeDistance, centreOffset);
    }

    LandingZone withLateralScale(double scale) {
        return new LandingZone(role, routeDistance, centreOffset * scale, depth, halfWidth * scale,
                preferredApproachSide, referenceCarryBand);
    }
}
