package com.progolf.sim.shot;

/**
 * Temporary player condition inputs to a shot (spec: numerical-model STATE category). Both values are
 * in [0,1]: {@code fatigue} 0 = fully fresh, {@code pressure} 0 = no pressure situation.
 */
public record GolferState(double fatigue, double pressure) {

    public GolferState {
        if (fatigue < 0 || fatigue > 1 || !Double.isFinite(fatigue)) {
            throw new IllegalArgumentException("fatigue must be in [0,1]: " + fatigue);
        }
        if (pressure < 0 || pressure > 1 || !Double.isFinite(pressure)) {
            throw new IllegalArgumentException("pressure must be in [0,1]: " + pressure);
        }
    }

    /** Fresh and relaxed. */
    public static GolferState fresh() {
        return new GolferState(0.0, 0.0);
    }
}
