package com.progolf.sim.progression;

import com.progolf.sim.course.Position2d;

/** Compact authoritative result retained for resume and duplicate-request detection. */
public record TrainingAttempt(int number, double score, double proximity, double totalDistance,
                              boolean holedTarget, Position2d finalPosition) {
    public TrainingAttempt {
        if (number < 1 || !Double.isFinite(score) || score < 0 || score > 1
                || !Double.isFinite(proximity) || proximity < 0
                || !Double.isFinite(totalDistance) || totalDistance < 0 || finalPosition == null)
            throw new IllegalArgumentException("invalid training attempt");
    }
}
