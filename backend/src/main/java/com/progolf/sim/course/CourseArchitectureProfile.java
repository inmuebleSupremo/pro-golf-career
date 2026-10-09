package com.progolf.sim.course;

/** Continuous V5 venue tendencies used to correlate, but never template, candidate hole architecture. */
public record CourseArchitectureProfile(double routingMovement, double lateralAsymmetry, double widthRhythm,
                                      double approachOpenness, double greenDefence) {
    public CourseArchitectureProfile {
        requireUnit(routingMovement, "routingMovement");
        requireUnit(lateralAsymmetry, "lateralAsymmetry");
        requireUnit(widthRhythm, "widthRhythm");
        requireUnit(approachOpenness, "approachOpenness");
        requireUnit(greenDefence, "greenDefence");
    }

    private static void requireUnit(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }
}
