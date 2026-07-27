package com.progolf.sim.equipment;

/**
 * An equipment brand (spec: equipment-influence) — the identity behind a piece of gear and, crucially, its
 * <b>trade-off bias</b>. Every brand distributes the same overall quality budget differently across the four
 * characteristics (forgiveness, power, workability, feel): the deltas sum to zero, so two same-quality items
 * from different brands cost the same and have the same mean, but a very different <i>shape</i> — one is a
 * bomber's driver, another a game-improvement driver. This is what makes equipment choice a real decision
 * that should fit a golfer's build, rather than a strictly-ordered quality ladder.
 *
 * <p>Deltas are applied to an item's base level and clamped to [0,1] by {@link #characteristics(double)}.
 * {@link #STANDARD} is the neutral free-gear brand (no bias); {@link #MERIDIAN} is a balanced premium brand.
 */
public enum EquipmentBrand {

    // deltas: (forgiveness, power, workability, feel) — each row sums to 0 (a pure redistribution).
    STANDARD("Standard", 0.00, 0.00, 0.00, 0.00),
    MERIDIAN("Meridian", 0.00, 0.00, 0.00, 0.00),  // balanced all-rounder
    APEX("Apex", -0.15, 0.30, -0.05, -0.10),        // power / distance
    EVERMAN("Everman", 0.30, -0.15, -0.05, -0.10),  // forgiveness / game-improvement
    ARTISAN("Artisan", -0.18, -0.15, 0.15, 0.18);   // feel + workability / shotmaker

    /** The brands a paid upgrade can come from (excludes STANDARD, the free baseline). */
    public static final EquipmentBrand[] UPGRADE_BRANDS = {MERIDIAN, APEX, EVERMAN, ARTISAN};

    private final String displayName;
    private final double forgivenessDelta;
    private final double powerDelta;
    private final double workabilityDelta;
    private final double feelDelta;

    EquipmentBrand(String displayName, double forgivenessDelta, double powerDelta, double workabilityDelta,
                   double feelDelta) {
        this.displayName = displayName;
        this.forgivenessDelta = forgivenessDelta;
        this.powerDelta = powerDelta;
        this.workabilityDelta = workabilityDelta;
        this.feelDelta = feelDelta;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * This brand's characteristics for an item of the given base level (the overall quality budget): the
     * level shifted by the brand's bias and clamped to [0,1]. A balanced brand returns a uniform level.
     */
    public EquipmentCharacteristics characteristics(double baseLevel) {
        return new EquipmentCharacteristics(
                clampUnit(baseLevel + forgivenessDelta),
                clampUnit(baseLevel + powerDelta),
                clampUnit(baseLevel + workabilityDelta),
                clampUnit(baseLevel + feelDelta));
    }

    private static double clampUnit(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
