package com.progolf.sim.shot;

/**
 * An absolute point in canonical hole yard-space. It is the player's intended carry/first-contact
 * point, never a final-resting target or a display coordinate.
 */
public record AimPoint(double x, double y) {
    public AimPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("aim coordinates must be finite");
        }
    }
}
