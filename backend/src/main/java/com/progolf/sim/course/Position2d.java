package com.progolf.sim.course;

/** A finite point in a hole's local yard-space. */
public record Position2d(double x, double y) {
    public Position2d {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("position must be finite");
        }
    }

    public double distanceTo(Position2d other) {
        return StrictMath.hypot(x - other.x, y - other.y);
    }

    public Position2d plus(double dx, double dy) {
        return new Position2d(x + dx, y + dy);
    }
}
