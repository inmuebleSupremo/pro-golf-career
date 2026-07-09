package com.progolf.sim.equipment;

import com.progolf.sim.core.Rng;

/**
 * Produces equipment items (spec: equipment-inventory / equipment-influence): the free {@link #standardItem}
 * every golfer starts with (baseline characteristics, zero cost, exactly neutral in play) and quality-scaled
 * {@link #generateUpgrade} candidates whose characteristics and cost rise with quality. Stateless and
 * deterministic — every draw comes from the supplied {@link Rng}.
 */
public final class EquipmentCatalogue {

    private EquipmentCatalogue() {
    }

    /** The standard, free baseline item for a category. */
    public static EquipmentItem standardItem(EquipmentCategory category) {
        return new EquipmentItem("Standard " + category, category,
                EquipmentConstants.BASELINE_CHARACTERISTIC, EquipmentCharacteristics.standard(), 0.0);
    }

    /** A quality-scaled upgrade candidate for a category. */
    public static EquipmentItem generateUpgrade(EquipmentCategory category, Rng rng) {
        double quality = clamp(
                EquipmentConstants.UPGRADE_QUALITY_MEAN + rng.nextGaussian() * EquipmentConstants.UPGRADE_QUALITY_SPREAD,
                EquipmentConstants.UPGRADE_QUALITY_MIN, EquipmentConstants.UPGRADE_QUALITY_MAX);
        double cost = category.baseCost() * quality;
        String name = category + "-Pro-" + Integer.toString((int) (rng.nextDouble() * 100_000));
        return new EquipmentItem(name, category, quality, EquipmentCharacteristics.uniform(quality), cost);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
