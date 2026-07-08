package com.progolf.sim.player;

import java.util.Objects;

/**
 * An active injury (REQ-021): type, severity, remaining recovery duration (in calendar steps), and a
 * gameplay performance penalty. Recovery advances over time and never permanently alters attributes.
 */
public record Injury(InjuryType type, Severity severity, int recoveryRemaining, double performancePenalty) {

    public Injury {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(severity, "severity");
        if (recoveryRemaining < 0) {
            throw new IllegalArgumentException("recoveryRemaining must be >= 0: " + recoveryRemaining);
        }
        if (performancePenalty < 0 || performancePenalty > 1) {
            throw new IllegalArgumentException("performancePenalty must be in [0,1]: " + performancePenalty);
        }
    }

    /** Creates a fresh injury of the given type and severity, using the severity's defaults. */
    public static Injury of(InjuryType type, Severity severity) {
        return new Injury(type, severity, severity.baseRecoverySteps(), severity.performancePenalty());
    }

    /** Returns the injury after {@code steps} of recovery (remaining never below zero). */
    public Injury advanceRecovery(int steps) {
        int remaining = Math.max(0, recoveryRemaining - Math.max(0, steps));
        return new Injury(type, severity, remaining, performancePenalty);
    }

    /** True once recovery is complete. */
    public boolean isHealed() {
        return recoveryRemaining <= 0;
    }

    /** Injury body-area types available in Version 1. */
    public enum InjuryType {
        WRIST, BACK, SHOULDER, KNEE, ELBOW, HAND
    }

    /** Injury severity with default recovery duration (steps) and performance penalty. */
    public enum Severity {
        MINOR(2, 0.05),
        MODERATE(6, 0.15),
        SEVERE(16, 0.35);

        private final int baseRecoverySteps;
        private final double performancePenalty;

        Severity(int baseRecoverySteps, double performancePenalty) {
            this.baseRecoverySteps = baseRecoverySteps;
            this.performancePenalty = performancePenalty;
        }

        public int baseRecoverySteps() {
            return baseRecoverySteps;
        }

        public double performancePenalty() {
            return performancePenalty;
        }
    }
}
