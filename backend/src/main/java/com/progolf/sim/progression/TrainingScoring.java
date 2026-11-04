package com.progolf.sim.progression;

import com.progolf.sim.practice.PracticePuttingGreen;
import com.progolf.sim.practice.PracticeRange;

/** Scores only outcomes returned by the existing authoritative practice shot resolvers. */
public final class TrainingScoring {
    private TrainingScoring() { }

    public static double range(TrainingChallenge challenge, PracticeRange.Result shot) {
        if (challenge.putting() || shot.outcome().trace() == null)
            throw new IllegalArgumentException("a resolved range shot is required");
        double proximity = challenge.target().distanceTo(shot.outcome().trace().finalPoint());
        double distanceError = Math.abs(challenge.distance() - shot.totalDistance());
        double error = 0.7 * proximity + 0.3 * distanceError;
        return bounded(1.0 - error / (2.0 * challenge.tolerance()));
    }

    public static double putt(TrainingChallenge challenge, PracticePuttingGreen.Result shot) {
        if (!challenge.putting()) throw new IllegalArgumentException("a putting challenge is required");
        if (shot.holedCup() != null && shot.holedCup() == challenge.cupNumber()) return 1.0;
        double error = challenge.target().distanceTo(shot.outcome().finalPosition());
        return bounded(1.0 - error / (2.0 * challenge.tolerance()));
    }

    /** A weak eight-shot result earns nothing; perfect play cannot exceed 15% of the base. */
    public static double bonusFraction(double aggregateScore) {
        if (!Double.isFinite(aggregateScore) || aggregateScore < 0 || aggregateScore > 1)
            throw new IllegalArgumentException("aggregate score must be between zero and one");
        return 0.15 * Math.max(0.0, (aggregateScore - 0.35) / 0.65);
    }

    private static double bounded(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }
}
