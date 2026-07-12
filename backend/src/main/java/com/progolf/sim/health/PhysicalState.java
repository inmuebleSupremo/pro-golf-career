package com.progolf.sim.health;

import java.util.Optional;

/**
 * A Professional Golfer's physical readiness for competition (spec: physical-state, REQ-215): long-term
 * {@code fitness}, accumulated {@code fatigue}, and an optional active {@link Injury}. Immutable, with
 * functional updates. {@link #availability()} is <em>derived</em> from this state (REQ-221), never stored,
 * so it can never desync. Both {@code fitness} and {@code fatigue} are in [0,1].
 */
public record PhysicalState(double fitness, double fatigue, Optional<Injury> injury) {

    public PhysicalState {
        if (!inUnit(fitness)) {
            throw new IllegalArgumentException("fitness must be in [0,1]: " + fitness);
        }
        if (!inUnit(fatigue)) {
            throw new IllegalArgumentException("fatigue must be in [0,1]: " + fatigue);
        }
        if (injury == null) {
            throw new IllegalArgumentException("injury must not be null (use Optional.empty())");
        }
    }

    /** A healthy state at the given fitness: no fatigue, no injury. */
    public static PhysicalState healthy(double fitness) {
        return new PhysicalState(fitness, 0.0, Optional.empty());
    }

    /** The availability derived from this state (REQ-221). */
    public Availability availability() {
        if (injury.isPresent()) {
            return injury.get().rehabWeeksRemaining() <= HealthConstants.RECOVERING_WEEKS_THRESHOLD
                    ? Availability.RECOVERING
                    : Availability.INJURED;
        }
        if (fatigue >= HealthConstants.REST_THRESHOLD) {
            return Availability.RESTING;
        }
        return Availability.AVAILABLE;
    }

    /** Whether the golfer may enter a competitive event unimpaired. */
    public boolean canCompete() {
        return availability() == Availability.AVAILABLE;
    }

    /**
     * Whether the golfer could play THROUGH the injury — i.e. it has reached its final rehabilitation stage
     * (RECOVERING) rather than the early INJURED stage (spec: injury-recovery play-through). A MINOR injury
     * qualifies from the outset; a MODERATE/SEVERE injury qualifies only in its rehab tail. Whether to take
     * that option is a decision the caller makes; competing while recovering incurs {@link #injuryImpairment()}.
     */
    public boolean canPlayThroughInjury() {
        return availability() == Availability.RECOVERING;
    }

    /** The shot impairment ([0,1]) suffered by playing through a recovering injury; 0 when not recovering. */
    public double injuryImpairment() {
        return canPlayThroughInjury() ? injury.get().severity().impairment() : 0.0;
    }

    public PhysicalState withFatigue(double newFatigue) {
        return new PhysicalState(fitness, newFatigue, injury);
    }

    public PhysicalState withInjury(Injury newInjury) {
        return new PhysicalState(fitness, fatigue, Optional.of(newInjury));
    }

    public PhysicalState clearedInjury() {
        return new PhysicalState(fitness, fatigue, Optional.empty());
    }

    private static boolean inUnit(double v) {
        return Double.isFinite(v) && v >= 0.0 && v <= 1.0;
    }
}
