package com.progolf.sim.course;

import com.progolf.sim.spatial.Surface;
import java.util.List;
import java.util.Objects;

/** Pure polygon operations used by the versioned pin-placement policy. */
final class GreenPinGeometry {
    static final double CLEARANCE_YARDS = 2.0;
    private static final double NUMERIC_MARGIN = 1.0e-8;
    private static final int PROJECTION_STEPS = 64;

    private GreenPinGeometry() {
    }

    static Position2d projectIntoEligibleGreen(CourseGeometry geometry, Position2d center, Position2d intended) {
        Objects.requireNonNull(geometry, "geometry");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(intended, "intended");
        List<Position2d> boundary = greenBoundary(geometry);
        if (!eligible(geometry, boundary, center)) {
            throw new IllegalStateException("Effective GREEN has no eligible " + CLEARANCE_YARDS
                    + " yard inset at its centre");
        }
        if (eligible(geometry, boundary, intended)) {
            return intended;
        }

        // Generated greens are convex polygonal ellipses. Bisection along the intended ray is fixed-work,
        // uses true edge clearance for every decision, and preserves both local intent signs.
        double low = 0.0;
        double high = 1.0;
        Position2d best = center;
        for (int step = 0; step < PROJECTION_STEPS; step++) {
            double t = (low + high) * 0.5;
            Position2d candidate = interpolate(center, intended, t);
            if (eligible(geometry, boundary, candidate)) {
                low = t;
                best = candidate;
            } else {
                high = t;
            }
        }
        if (!eligible(geometry, boundary, best)) {
            throw new IllegalStateException("Unable to project cup into effective GREEN inset");
        }
        return best;
    }

    static boolean eligible(CourseGeometry geometry, Position2d point) {
        return eligible(geometry, greenBoundary(geometry), point);
    }

    static double clearanceToGreenBoundary(CourseGeometry geometry, Position2d point) {
        return clearanceToBoundary(greenBoundary(geometry), point);
    }

    private static boolean eligible(CourseGeometry geometry, List<Position2d> boundary, Position2d point) {
        return geometry.surfaceAt(point) == Surface.GREEN
                && clearanceToBoundary(boundary, point) >= CLEARANCE_YARDS + NUMERIC_MARGIN;
    }

    private static List<Position2d> greenBoundary(CourseGeometry geometry) {
        return geometry.regions().stream().filter(region -> region.surface() == Surface.GREEN)
                .findFirst().orElseThrow(() -> new IllegalStateException("Effective geometry has no GREEN region"))
                .boundary();
    }

    private static double clearanceToBoundary(List<Position2d> boundary, Position2d point) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < boundary.size(); i++) {
            nearest = Math.min(nearest, pointToSegmentDistance(point, boundary.get(i),
                    boundary.get((i + 1) % boundary.size())));
        }
        return nearest;
    }

    private static double pointToSegmentDistance(Position2d point, Position2d a, Position2d b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0.0 ? 0.0
                : Math.clamp(((point.x() - a.x()) * dx + (point.y() - a.y()) * dy) / lengthSquared, 0.0, 1.0);
        return StrictMath.hypot(point.x() - (a.x() + t * dx), point.y() - (a.y() + t * dy));
    }

    private static Position2d interpolate(Position2d from, Position2d to, double t) {
        return new Position2d(from.x() + (to.x() - from.x()) * t, from.y() + (to.y() - from.y()) * t);
    }
}
