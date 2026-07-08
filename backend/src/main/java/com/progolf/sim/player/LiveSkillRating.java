package com.progolf.sim.player;

/**
 * A Player's current competitive form (REQ-019): volatile, self-correcting, and never a permanent
 * attribute. Immutable value — mutating operations return a new instance. Values are bounded to
 * [{@link PlayerConstants#RATING_MIN}, {@link PlayerConstants#RATING_MAX}].
 */
public record LiveSkillRating(double baseline, double value) {

    public LiveSkillRating {
        if (!Double.isFinite(baseline) || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Rating values must be finite");
        }
    }

    /** A rating sitting exactly at its baseline. */
    public static LiveSkillRating atBaseline(double baseline) {
        return new LiveSkillRating(baseline, clamp(baseline));
    }

    /** Returns a rating adjusted by a performance {@code delta} (positive = better form), clamped. */
    public LiveSkillRating withPerformance(double delta) {
        return new LiveSkillRating(baseline, clamp(value + delta));
    }

    /** Returns a rating drifted toward the baseline over {@code steps} of inactivity. */
    public LiveSkillRating decayedTowardBaseline(int steps) {
        if (steps <= 0) {
            return this;
        }
        double fraction = Math.min(1.0, PlayerConstants.RATING_DECAY_RATE * steps);
        double next = value + (baseline - value) * fraction;
        return new LiveSkillRating(baseline, clamp(next));
    }

    private static double clamp(double v) {
        return Math.max(PlayerConstants.RATING_MIN, Math.min(PlayerConstants.RATING_MAX, v));
    }
}
