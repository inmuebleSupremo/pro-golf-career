package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** One deterministic sample of the resolver-owned airborne path. */
public record AirbornePoint(double progress, Position2d position, double height) {
    public AirbornePoint {
        if (!Double.isFinite(progress) || progress < 0 || progress > 1 || !Double.isFinite(height) || height < 0) {
            throw new IllegalArgumentException("airborne point must be finite and in range");
        }
        Objects.requireNonNull(position, "position");
    }
}
