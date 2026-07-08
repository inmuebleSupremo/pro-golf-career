package com.progolf.sim.player;

import java.util.Optional;

/**
 * A Player's temporary State (REQ-020/019/021): Fatigue, Live Skill Rating, and zero-or-one active
 * Injury. This type deliberately holds no reference to {@code Attributes} — by construction, changing
 * state cannot mutate permanent attributes (REQ-019/020).
 */
public final class PlayerState {

    private double fatigue;
    private LiveSkillRating rating;
    private Injury injury; // null when no active injury

    PlayerState(double fatigue, LiveSkillRating rating) {
        setFatigue(fatigue);
        this.rating = rating;
        this.injury = null;
    }

    /** A fresh state: no fatigue, rating at baseline, no injury. */
    public static PlayerState fresh() {
        return new PlayerState(0.0, LiveSkillRating.atBaseline(PlayerConstants.RATING_BASELINE));
    }

    public double fatigue() {
        return fatigue;
    }

    public void setFatigue(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("fatigue must be finite");
        }
        this.fatigue = Math.max(PlayerConstants.FATIGUE_MIN, Math.min(PlayerConstants.FATIGUE_MAX, value));
    }

    public LiveSkillRating rating() {
        return rating;
    }

    /** Records a performance result, moving the Live Skill Rating up (positive) or down (negative). */
    public void recordPerformance(double delta) {
        this.rating = rating.withPerformance(delta);
    }

    /** Applies inactivity decay of the Live Skill Rating toward baseline. */
    public void decayRating(int steps) {
        this.rating = rating.decayedTowardBaseline(steps);
    }

    public Optional<Injury> injury() {
        return Optional.ofNullable(injury);
    }

    public boolean hasActiveInjury() {
        return injury != null;
    }

    /** Applies a new injury; rejects if one is already active (REQ-021 zero-or-one). */
    void applyInjury(Injury newInjury) {
        if (injury != null) {
            throw new IllegalStateException("Player already has an active injury");
        }
        this.injury = java.util.Objects.requireNonNull(newInjury, "injury");
    }

    /** Advances injury recovery; returns true if the injury healed and cleared on this step. */
    boolean advanceInjuryRecovery(int steps) {
        if (injury == null) {
            return false;
        }
        injury = injury.advanceRecovery(steps);
        if (injury.isHealed()) {
            injury = null;
            return true;
        }
        return false;
    }
}
