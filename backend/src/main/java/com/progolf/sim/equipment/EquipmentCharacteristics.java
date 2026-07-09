package com.progolf.sim.equipment;

/**
 * The gameplay characteristics of an equipment item (spec: equipment-influence, REQ-207): forgiveness,
 * power, workability, and feel, each in [0,1]. The specification defines their existence; V1 applies
 * forgiveness and power to shot resolution and keeps workability and feel as data for future use.
 */
public record EquipmentCharacteristics(double forgiveness, double power, double workability, double feel) {

    public EquipmentCharacteristics {
        requireUnit(forgiveness, "forgiveness");
        requireUnit(power, "power");
        requireUnit(workability, "workability");
        requireUnit(feel, "feel");
    }

    /** Uniform characteristics at the given level. */
    public static EquipmentCharacteristics uniform(double level) {
        return new EquipmentCharacteristics(level, level, level, level);
    }

    /** Baseline (standard) characteristics — the neutral level. */
    public static EquipmentCharacteristics standard() {
        return uniform(EquipmentConstants.BASELINE_CHARACTERISTIC);
    }

    private static void requireUnit(double v, String field) {
        if (!Double.isFinite(v) || v < 0 || v > 1) {
            throw new IllegalArgumentException(field + " must be in [0,1]: " + v);
        }
    }
}
