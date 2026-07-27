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

    /** The standard, free baseline item for a category (neutral STANDARD brand, exactly neutral in play). */
    public static EquipmentItem standardItem(EquipmentCategory category) {
        return new EquipmentItem("Standard " + label(category), category, EquipmentBrand.STANDARD,
                EquipmentConstants.BASELINE_CHARACTERISTIC, EquipmentCharacteristics.standard(), 0.0);
    }

    /** A quality-scaled upgrade candidate for a category, from a randomly-drawn brand (its bias shapes it). */
    public static EquipmentItem generateUpgrade(EquipmentCategory category, Rng rng) {
        EquipmentBrand brand = EquipmentBrand.UPGRADE_BRANDS[
                (int) Math.floorMod(rng.nextLong(), EquipmentBrand.UPGRADE_BRANDS.length)];
        return generateUpgrade(category, brand, rng);
    }

    /**
     * A quality-scaled upgrade candidate for a category from a specific brand: the base level (overall budget)
     * is drawn from the quality band, then the brand's bias shapes the four characteristics — so same-level
     * items from different brands cost the same but trade off differently. {@code quality} is the resulting
     * mean characteristic (the bias is a redistribution, so it stays close to the base level).
     */
    public static EquipmentItem generateUpgrade(EquipmentCategory category, EquipmentBrand brand, Rng rng) {
        double baseLevel = clamp(
                EquipmentConstants.UPGRADE_QUALITY_MEAN + rng.nextGaussian() * EquipmentConstants.UPGRADE_QUALITY_SPREAD,
                EquipmentConstants.UPGRADE_QUALITY_MIN, EquipmentConstants.UPGRADE_QUALITY_MAX);
        EquipmentCharacteristics characteristics = brand.characteristics(baseLevel);
        double quality = meanOf(characteristics);
        double cost = category.baseCost() * quality;
        // A serial keeps the name unique (the loadout addresses items by name); the UI shows brand + category.
        int serial = 1000 + (int) (rng.nextDouble() * 9000);
        String name = brand.displayName() + " " + label(category) + " " + serial;
        return new EquipmentItem(name, category, brand, quality, characteristics, cost);
    }

    /**
     * The full bag a brand deal provides: one item per category from the brand at {@code tier}, provided free
     * (cost 0). Deterministic — no rng, so the same deal always kits the same bag. The season disambiguates
     * item names across deals with the same brand over a career (the loadout addresses items by name).
     */
    public static java.util.List<EquipmentItem> dealBag(EquipmentBrand brand, double tier, int season) {
        java.util.List<EquipmentItem> bag = new java.util.ArrayList<>(EquipmentCategory.values().length);
        EquipmentCharacteristics characteristics = brand.characteristics(tier);
        double quality = meanOf(characteristics);
        for (EquipmentCategory category : EquipmentCategory.values()) {
            String name = brand.displayName() + " " + label(category) + " S" + season;
            bag.add(new EquipmentItem(name, category, brand, quality, characteristics, 0.0));
        }
        return bag;
    }

    private static double meanOf(EquipmentCharacteristics c) {
        return (c.forgiveness() + c.power() + c.workability() + c.feel()) / 4.0;
    }

    /** A readable category label, e.g. GOLF_BALL → "Golf Ball". */
    private static String label(EquipmentCategory category) {
        String[] words = category.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
