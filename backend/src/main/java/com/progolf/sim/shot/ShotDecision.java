package com.progolf.sim.shot;

import java.util.Objects;

/**
 * A complete player shot decision (REQ-051): exactly one club, an intended target (carry distance plus
 * an aim offset from the shot line), and one strategy. A shot cannot be resolved without all of these.
 */
public record ShotDecision(Club club, double targetDistance, double targetLateral, Strategy strategy) {

    public ShotDecision {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(strategy, "strategy");
        if (!Double.isFinite(targetDistance) || targetDistance < 0) {
            throw new IllegalArgumentException("targetDistance must be finite and >= 0: " + targetDistance);
        }
        if (!Double.isFinite(targetLateral)) {
            throw new IllegalArgumentException("targetLateral must be finite: " + targetLateral);
        }
    }

    /** A decision aimed straight down the line (no lateral aim offset). */
    public static ShotDecision straight(Club club, double targetDistance, Strategy strategy) {
        return new ShotDecision(club, targetDistance, 0.0, strategy);
    }
}
