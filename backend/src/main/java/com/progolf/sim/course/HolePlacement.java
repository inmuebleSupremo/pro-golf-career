package com.progolf.sim.course;

import java.util.Objects;

/** One oriented local-hole envelope within the shared V6 course coordinate frame. */
public record HolePlacement(int holeNumber, CourseCoordinateTransform transform, double envelopeHalfWidth,
                            double envelopeLength, double transitionToNextTee, LandscapeRelationship relationship) {
    public HolePlacement {
        if (holeNumber < 1 || holeNumber > 18 || envelopeHalfWidth <= 0.0 || envelopeLength <= 0.0
                || transitionToNextTee < 0.0 || !Double.isFinite(envelopeHalfWidth)
                || !Double.isFinite(envelopeLength) || !Double.isFinite(transitionToNextTee)) {
            throw new IllegalArgumentException("invalid V6 hole placement");
        }
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(relationship, "relationship");
    }

    public Position2d greenEstimate() {
        return transform.toCourse(new Position2d(0.0, envelopeLength - 70.0));
    }
}
