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
    public static final double WORKABILITY_SCALE = 0.30; // added to wind resistance (wind control)
    public static final double FEEL_SCALE = 0.15;        // max ~0.072 distance-dispersion reduction (proximity)

    // --- Equipment brand deals (a brand pays the player + kits their bag, in exchange for lock-in) ---
    /** Brand-deal offers presented when the player is a free agent: floor + reputation-scaled additions. */
    public static final int DEAL_OFFER_BASE = 2;
    public static final int DEAL_OFFER_REP_SPAN = 2;      // up to +2 more offers at full reputation
    /** Per-season retainer: a floor lifted by reputation (a bigger star commands more). */
    public static final double DEAL_RETAINER_BASE = 90_000.0;
    public static final double DEAL_RETAINER_REP_SCALE = 4.0;
    public static final double DEAL_SIGNING_FRACTION = 0.50; // signing bonus as a fraction of the retainer
    public static final double DEAL_VALUE_NOISE = 0.15;
    /** The tier (overall quality) of the bag a deal provides: a floor lifted by reputation, capped high. */
    public static final double DEAL_GEAR_TIER_BASE = 0.72;
    public static final double DEAL_GEAR_TIER_REP_SCALE = 0.24;
    public static final double DEAL_GEAR_TIER_MAX = 0.96;
    public static final int DEAL_MIN_DURATION = 2;        // seasons of lock-in
    public static final int DEAL_MAX_DURATION = 4;
    /** Distinct salt so brand-deal draws are isolated from the per-category upgrade stream. */
    public static final long DEAL_SALT = 135_792_468L;
}
