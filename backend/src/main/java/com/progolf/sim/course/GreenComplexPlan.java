package com.progolf.sim.course;

import java.util.Objects;
import java.util.Set;

/** Immutable V3 green intent, compiled into a modest rotated canonical polygon. */
public record GreenComplexPlan(Position2d centerOffset, double majorAxis, double minorAxis, double rotationRadians,
                               Position2d approachDirection, GreenSide openEntrySide, GreenSide protectedSide,
                               GreenSide bailoutSide, boolean runUpOpening, Set<GreenSurroundRole> surroundRoles) {
    public GreenComplexPlan {
        Objects.requireNonNull(centerOffset, "centerOffset");
        Objects.requireNonNull(approachDirection, "approachDirection");
        Objects.requireNonNull(openEntrySide, "openEntrySide");
        Objects.requireNonNull(protectedSide, "protectedSide");
        Objects.requireNonNull(bailoutSide, "bailoutSide");
        surroundRoles = Set.copyOf(Objects.requireNonNull(surroundRoles, "surroundRoles"));
        if (!Double.isFinite(majorAxis) || !Double.isFinite(minorAxis) || majorAxis <= 0.0 || minorAxis <= 0.0
                || !Double.isFinite(rotationRadians) || StrictMath.hypot(approachDirection.x(), approachDirection.y()) < 0.9) {
            throw new IllegalArgumentException("green-complex dimensions, rotation, and approach must be valid");
        }
        if (openEntrySide == protectedSide || openEntrySide == bailoutSide) {
            throw new IllegalArgumentException("green entry, protected, and bailout sides must differ");
        }
    }

    public Position2d center(HoleRoute route) {
        Objects.requireNonNull(route, "route");
        return route.approachAnchor().plus(centerOffset.x(), centerOffset.y());
    }

    GreenComplexPlan withLateralScale(double scale) {
        Position2d scaledApproach = new Position2d(approachDirection.x() * scale, approachDirection.y());
        double magnitude = StrictMath.hypot(scaledApproach.x(), scaledApproach.y());
        return new GreenComplexPlan(new Position2d(centerOffset.x() * scale, centerOffset.y()), majorAxis,
                minorAxis * scale, StrictMath.atan2(scaledApproach.y(), scaledApproach.x()),
                new Position2d(scaledApproach.x() / magnitude, scaledApproach.y() / magnitude), openEntrySide,
                protectedSide, bailoutSide, runUpOpening, surroundRoles);
    }
}
