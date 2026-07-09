package com.progolf.sim.health;

import java.util.Objects;

/**
 * An active injury (spec: injury-recovery, REQ-219/220): its body-area, severity, and the weeks of
 * rehabilitation remaining. Rehabilitation advances over time and never clears instantaneously. Immutable.
 */
public record Injury(InjuryType type, InjurySeverity severity, int rehabWeeksRemaining) {

    public Injury {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(severity, "severity");
        if (rehabWeeksRemaining < 0) {
            throw new IllegalArgumentException("rehabWeeksRemaining must be >= 0: " + rehabWeeksRemaining);
        }
    }

    /** A fresh injury requiring the full rehabilitation for its severity. */
    public static Injury of(InjuryType type, InjurySeverity severity) {
        return new Injury(type, severity, severity.rehabWeeks());
    }

    /** The injury after {@code weeks} of rehabilitation (remaining never below zero). */
    public Injury advance(int weeks) {
        return new Injury(type, severity, Math.max(0, rehabWeeksRemaining - Math.max(0, weeks)));
    }

    /** True once rehabilitation is complete. */
    public boolean isHealed() {
        return rehabWeeksRemaining <= 0;
    }
}
