package com.progolf.sim.shot;

/**
 * Temporary player condition inputs to a shot (spec: numerical-model STATE category). {@code fatigue} and
 * {@code pressure} are in [0,1] (0 = fresh / no pressure). {@code equipmentForgiveness} and
 * {@code equipmentPower} are non-negative bonuses in [0,1] from the golfer's active bag (spec:
 * equipment-influence): forgiveness reduces dispersion, power extends reach. Both default to 0 (neutral),
 * so standard equipment reproduces prior shot behaviour exactly.
 */
public record GolferState(double fatigue, double pressure, double equipmentForgiveness, double equipmentPower) {

    public GolferState {
        if (fatigue < 0 || fatigue > 1 || !Double.isFinite(fatigue)) {
            throw new IllegalArgumentException("fatigue must be in [0,1]: " + fatigue);
        }
        if (pressure < 0 || pressure > 1 || !Double.isFinite(pressure)) {
            throw new IllegalArgumentException("pressure must be in [0,1]: " + pressure);
        }
        if (equipmentForgiveness < 0 || equipmentForgiveness > 1 || !Double.isFinite(equipmentForgiveness)) {
            throw new IllegalArgumentException("equipmentForgiveness must be in [0,1]: " + equipmentForgiveness);
        }
        if (equipmentPower < 0 || equipmentPower > 1 || !Double.isFinite(equipmentPower)) {
            throw new IllegalArgumentException("equipmentPower must be in [0,1]: " + equipmentPower);
        }
    }

    /** Condition with neutral (standard) equipment. */
    public GolferState(double fatigue, double pressure) {
        this(fatigue, pressure, 0.0, 0.0);
    }

    /** Fresh and relaxed, with standard equipment. */
    public static GolferState fresh() {
        return new GolferState(0.0, 0.0, 0.0, 0.0);
    }
}
