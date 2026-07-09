package com.progolf.sim.equipment;

/**
 * The single tunables surface for the Equipment &amp; Tournament Loadout domain (spec: equipment-inventory
 * / tournament-loadout / equipment-influence). All magnitudes live here so equipment economics and the
 * shot effect can be calibrated in one place. Characteristics are normalized to [0,1] around a baseline;
 * costs are in the same currency units as prize money.
 */
public final class EquipmentConstants {

    private EquipmentConstants() {
    }

    /**
     * Distinct salt mixed into the per-golfer/season equipment seed so acquisition is isolated from the
     * shot, weather, economy, health, and staff streams. A large prime, different from the others.
     */
    public static final long EQUIPMENT_SALT = 246_813_579L;

    /** Baseline characteristic level: standard gear sits here and produces exactly zero shot bonus. */
    public static final double BASELINE_CHARACTERISTIC = 0.50;

    // --- Upgrade quality (seeded per candidate) ---
    public static final double UPGRADE_QUALITY_MEAN = 0.75;
    public static final double UPGRADE_QUALITY_SPREAD = 0.12;
    public static final double UPGRADE_QUALITY_MIN = 0.55;
    public static final double UPGRADE_QUALITY_MAX = 0.98;

    // --- Shot effect scaling (applied to a bag's above-baseline aggregate) ---
    public static final double FORGIVENESS_SCALE = 0.20; // max ~0.096 dispersion reduction at full upgrade
    public static final double POWER_SCALE = 0.15;       // max ~0.072 reach extension at full upgrade
}
