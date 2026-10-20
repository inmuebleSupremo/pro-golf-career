package com.progolf.sim.course;

import java.util.Objects;

/** A route-relative landing area for the retained Generator 8 strategic architecture. */
public record ArchitectureLandingZone(LandingZoneRole role, double routeDistance, double centreOffset,
                                     double depth, double halfWidth, PreferredApproachSide preferredApproachSide) {
    public ArchitectureLandingZone {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(preferredApproachSide, "preferredApproachSide");
        if (!Double.isFinite(routeDistance) || routeDistance <= 0.0 || !Double.isFinite(centreOffset)
                || !Double.isFinite(depth) || depth <= 0.0 || !Double.isFinite(halfWidth) || halfWidth <= 0.0) {
            throw new IllegalArgumentException("architecture landing-zone dimensions must be finite and positive");
        }
    }

    public Position2d center(ArchitectureRoute route) {
        Position2d point = route.pointAt(routeDistance);
        Position2d before = route.pointAt(Math.max(0.0, routeDistance - 1.0));
        Position2d after = route.pointAt(Math.min(route.length(), routeDistance + 1.0));
        double dx = after.x() - before.x();
        double dy = after.y() - before.y();
        double magnitude = StrictMath.hypot(dx, dy);
        return point.plus(dy / magnitude * centreOffset, -dx / magnitude * centreOffset);
    }
}
