package com.progolf.sim.shot;

import java.util.Objects;

/**
 * A complete player shot decision (REQ-051): exactly one club, an intended target (carry distance plus
 * an aim offset from the shot line), and one strategy. A shot cannot be resolved without all of these.
 */
/**
 * Transitional compatibility shape for legacy fixtures and the simplified execution sampler. New production
 * callers create it only through {@link #fromIntent(BallStrikeIntent, double, Strategy)}.
 */
public record ShotDecision(Club club, double targetDistance, double targetLateral, Strategy strategy, ClubSpec clubSpec,
                           ShotFamily shotFamily, ShotShape shotShape) {

    public ShotDecision {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(shotFamily, "shotFamily");
        Objects.requireNonNull(shotShape, "shotShape");
        if (clubSpec == null) clubSpec = ClubSpec.forLegacy(club);
        if (!Double.isFinite(targetDistance) || targetDistance < 0) {
            throw new IllegalArgumentException("targetDistance must be finite and >= 0: " + targetDistance);
        }
        if (!Double.isFinite(targetLateral)) {
            throw new IllegalArgumentException("targetLateral must be finite: " + targetLateral);
        }
    }

    public ShotDecision(Club club, double targetDistance, double targetLateral, Strategy strategy) {
        this(club, targetDistance, targetLateral, strategy, ClubSpec.forLegacy(club), ShotFamily.FULL, ShotShape.STRAIGHT);
    }

    public ShotDecision(Club club, double targetDistance, double targetLateral, Strategy strategy, ClubSpec clubSpec) {
        this(club, targetDistance, targetLateral, strategy, clubSpec, ShotFamily.FULL, ShotShape.STRAIGHT);
    }

    public ShotDecision(Club club, double targetDistance, double targetLateral, Strategy strategy, ClubSpec clubSpec,
                        ShotFamily shotFamily) {
        this(club, targetDistance, targetLateral, strategy, clubSpec, shotFamily, ShotShape.STRAIGHT);
    }

    /** A decision aimed straight down the line (no lateral aim offset). */
    public static ShotDecision straight(Club club, double targetDistance, Strategy strategy) {
        return new ShotDecision(club, targetDistance, 0.0, strategy);
    }

    public static ShotDecision fromIntent(BallStrikeIntent intent, double requestedCarry, Strategy executionStrategy) {
        ClubSpec spec = ClubSpec.of(intent.club());
        return new ShotDecision(spec.family(), requestedCarry, 0.0, executionStrategy, spec, intent.shotFamily(), intent.shotShape());
    }
}
