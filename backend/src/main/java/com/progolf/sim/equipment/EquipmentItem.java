package com.progolf.sim.equipment;

import java.util.Objects;

/**
 * A single owned piece of equipment (spec: equipment-inventory, REQ-203/207): its name, category, a
 * summary {@code quality} in [0,1] (used to compare upgrades), its gameplay {@link EquipmentCharacteristics},
 * and its cost. Immutable.
 */
public record EquipmentItem(String name, EquipmentCategory category, double quality,
                            EquipmentCharacteristics characteristics, double cost) {

    public EquipmentItem {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(characteristics, "characteristics");
        if (!Double.isFinite(quality) || quality < 0 || quality > 1) {
            throw new IllegalArgumentException("quality must be in [0,1]: " + quality);
        }
        if (cost < 0 || !Double.isFinite(cost)) {
            throw new IllegalArgumentException("cost must be finite and >= 0: " + cost);
        }
    }
}
