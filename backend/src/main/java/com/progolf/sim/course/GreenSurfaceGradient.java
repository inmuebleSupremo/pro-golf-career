package com.progolf.sim.course;

/** Local elevation gradient in yards of rise per horizontal yard. */
public record GreenSurfaceGradient(double x, double y) {
    public GreenSurfaceGradient {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("green-surface gradient must be finite");
        }
    }

    public double magnitude() {
        return StrictMath.hypot(x, y);
    }

    /** Unit downhill direction, or zero on a flat sample. */
    public Position2d downhillDirection() {
        double magnitude = magnitude();
        return magnitude < 1.0e-12 ? new Position2d(0.0, 0.0) : new Position2d(-x / magnitude, -y / magnitude);
    }
}
