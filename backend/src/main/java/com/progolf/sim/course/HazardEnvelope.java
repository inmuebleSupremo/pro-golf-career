package com.progolf.sim.course;

/** Small compiler-facing V4 placement envelope, never canonical terrain itself. */
public record HazardEnvelope(double forwardOffset, double lateralOffset, double longitudinalRadius,
                             double lateralRadius) {
    public HazardEnvelope {
        if (!Double.isFinite(forwardOffset) || !Double.isFinite(lateralOffset)
                || !Double.isFinite(longitudinalRadius) || !Double.isFinite(lateralRadius)
                || longitudinalRadius <= 0.0 || lateralRadius <= 0.0) {
            throw new IllegalArgumentException("hazard envelope must be finite with positive radii");
        }
    }
}
