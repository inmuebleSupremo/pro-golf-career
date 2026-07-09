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
    private double equipmentForgiveness; // transient bag bonus, set before play; 0 = standard/neutral
    private double equipmentPower;

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

    public double equipmentForgiveness() {
        return equipmentForgiveness;
    }

    public double equipmentPower() {
        return equipmentPower;
    }

    /**
     * Sets the transient equipment bonuses from the active Golf Bag before play (spec: equipment-influence).
     * Temporary state only — this never touches permanent attributes. Bonuses are non-negative in [0,1].
     */
    public void setEquipment(double forgiveness, double power) {
        if (!Double.isFinite(forgiveness) || forgiveness < 0 || forgiveness > 1) {
            throw new IllegalArgumentException("forgiveness must be in [0,1]: " + forgiveness);
        }
        if (!Double.isFinite(power) || power < 0 || power > 1) {
            throw new IllegalArgumentException("power must be in [0,1]: " + power);
        }
        this.equipmentForgiveness = forgiveness;
        this.equipmentPower = power;
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
