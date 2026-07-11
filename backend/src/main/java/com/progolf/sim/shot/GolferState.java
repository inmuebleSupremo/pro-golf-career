package com.progolf.sim.shot;

/**
 * Temporary player condition inputs to a shot (spec: numerical-model STATE category). {@code fatigue} and
 * {@code pressure} are in [0,1] (0 = fresh / no pressure). {@code equipmentForgiveness} and
 * {@code equipmentPower} are non-negative bonuses in [0,1] from the golfer's active bag (spec:
 * equipment-influence): forgiveness reduces dispersion, power extends reach. {@code mentalSupport} and
 * {@code strategicSupport} are non-negative staff-support bonuses in [0,1] (spec: staff-influence): mental
 * support (psychologist) reduces the effect of fatigue, strategic support (caddie) reduces mishits.
 * {@code equipmentWorkability} and {@code equipmentFeel} are the remaining bag bonuses (spec:
 * equipment-influence): workability improves ball-flight control in wind, feel improves distance control.
 * Every bonus defaults to 0 (neutral), so a golfer with standard equipment and no support reproduces prior
 * shot behaviour exactly. (Equipment bonuses sit at positions 3-4 and 7-8, split by the two support inputs,
 * so the shorter convenience constructors stay backward-compatible.)
 */
public record GolferState(double fatigue, double pressure, double equipmentForgiveness, double equipmentPower,
                          double mentalSupport, double strategicSupport,
                          double equipmentWorkability, double equipmentFeel) {

    public GolferState {
        requireUnit(fatigue, "fatigue");
        requireUnit(pressure, "pressure");
        requireUnit(equipmentForgiveness, "equipmentForgiveness");
        requireUnit(equipmentPower, "equipmentPower");
        requireUnit(mentalSupport, "mentalSupport");
        requireUnit(strategicSupport, "strategicSupport");
        requireUnit(equipmentWorkability, "equipmentWorkability");
        requireUnit(equipmentFeel, "equipmentFeel");
    }

    private static void requireUnit(double value, String name) {
        if (value < 0 || value > 1 || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be in [0,1]: " + value);
        }
    }

    /** Condition with neutral (standard) equipment and no staff support. */
    public GolferState(double fatigue, double pressure) {
        this(fatigue, pressure, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /** Condition with forgiveness/power equipment but no support or workability/feel. */
    public GolferState(double fatigue, double pressure, double equipmentForgiveness, double equipmentPower) {
        this(fatigue, pressure, equipmentForgiveness, equipmentPower, 0.0, 0.0, 0.0, 0.0);
    }

    /** Condition with equipment (forgiveness/power) and staff support but no workability/feel. */
    public GolferState(double fatigue, double pressure, double equipmentForgiveness, double equipmentPower,
                       double mentalSupport, double strategicSupport) {
        this(fatigue, pressure, equipmentForgiveness, equipmentPower, mentalSupport, strategicSupport, 0.0, 0.0);
    }

    /** Fresh and relaxed, with standard equipment and no support. */
    public static GolferState fresh() {
        return new GolferState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
}
