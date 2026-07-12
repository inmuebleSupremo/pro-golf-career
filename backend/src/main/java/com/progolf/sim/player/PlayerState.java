package com.progolf.sim.player;

/**
 * A Player's temporary State (REQ-020/019): Fatigue and Live Skill Rating. This type deliberately holds
 * no reference to {@code Attributes} — by construction, changing state cannot mutate permanent attributes
 * (REQ-019/020). Injuries live in the health domain ({@code sim.health.PhysicalState}), not here.
 */
public final class PlayerState {

    private double fatigue;
    private LiveSkillRating rating;
    private double equipmentForgiveness; // transient bag bonus, set before play; 0 = standard/neutral
    private double equipmentPower;
    private double equipmentWorkability;
    private double equipmentFeel;
    private double mentalSupport; // transient staff-support bonus, set before play; 0 = no support
    private double strategicSupport;
    private double injuryImpairment; // transient health-synced shot handicap, set before play; 0 = uninjured

    PlayerState(double fatigue, LiveSkillRating rating) {
        setFatigue(fatigue);
        this.rating = rating;
    }

    /** A fresh state: no fatigue, rating at baseline. */
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

    public double equipmentWorkability() {
        return equipmentWorkability;
    }

    public double equipmentFeel() {
        return equipmentFeel;
    }

    /**
     * Sets the transient equipment bonuses from the active Golf Bag before play (spec: equipment-influence):
     * forgiveness, power, workability, and feel. Temporary state only — this never touches permanent
     * attributes. Bonuses are non-negative in [0,1].
     */
    public void setEquipment(double forgiveness, double power, double workability, double feel) {
        this.equipmentForgiveness = requireUnit(forgiveness, "forgiveness");
        this.equipmentPower = requireUnit(power, "power");
        this.equipmentWorkability = requireUnit(workability, "workability");
        this.equipmentFeel = requireUnit(feel, "feel");
    }

    private static double requireUnit(double value, String name) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be in [0,1]: " + value);
        }
        return value;
    }

    public double mentalSupport() {
        return mentalSupport;
    }

    public double strategicSupport() {
        return strategicSupport;
    }

    public double injuryImpairment() {
        return injuryImpairment;
    }

    /**
     * Sets the transient injury impairment for the next shots, synced from the health domain
     * ({@code PhysicalState.injuryImpairment()}) before play — exactly as fatigue is (spec: injury-recovery
     * play-through). Temporary state only; never touches permanent attributes. In [0,1]; 0 = uninjured.
     */
    public void setInjuryImpairment(double impairment) {
        this.injuryImpairment = requireUnit(impairment, "injuryImpairment");
    }

    /**
     * Sets the transient staff-support bonuses from the active Support Team before play (spec:
     * staff-influence): mental support (psychologist) and strategic support (caddie). Temporary state only —
     * never touches permanent attributes. Bonuses are non-negative in [0,1].
     */
    public void setSupport(double mentalSupport, double strategicSupport) {
        if (!Double.isFinite(mentalSupport) || mentalSupport < 0 || mentalSupport > 1) {
            throw new IllegalArgumentException("mentalSupport must be in [0,1]: " + mentalSupport);
        }
        if (!Double.isFinite(strategicSupport) || strategicSupport < 0 || strategicSupport > 1) {
            throw new IllegalArgumentException("strategicSupport must be in [0,1]: " + strategicSupport);
        }
        this.mentalSupport = mentalSupport;
        this.strategicSupport = strategicSupport;
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
}
