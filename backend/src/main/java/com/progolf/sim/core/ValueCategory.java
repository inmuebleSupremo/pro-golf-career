package com.progolf.sim.core;

/**
 * The five numerical value categories every value in the simulation belongs to (spec: numerical-model).
 *
 * <p>The category governs whether a value may be developed (trained) and whether it may be consumed
 * as a raw input to a calculation. Outcomes must be reclassified before being reused as inputs.
 */
public enum ValueCategory {
    /** Permanent, persisted player ability on the 0-100 scale. The only trainable category. */
    ATTRIBUTE(true, true),
    /** Temporary additive/multiplicative adjustment; never persisted; expires with its source. */
    MODIFIER(false, true),
    /** Derived summary recalculated on demand (e.g. Live Skill Rating); never directly trainable. */
    RATING(false, true),
    /** Temporary player condition (e.g. Fatigue, current hole); mutable, non-permanent. */
    STATE(false, true),
    /** Final result of a calculation; must be reclassified before being reused as an input. */
    OUTCOME(false, false);

    private final boolean trainable;
    private final boolean validCalculationInput;

    ValueCategory(boolean trainable, boolean validCalculationInput) {
        this.trainable = trainable;
        this.validCalculationInput = validCalculationInput;
    }

    /** True only for {@link #ATTRIBUTE}: the sole category eligible for development. */
    public boolean isTrainable() {
        return trainable;
    }

    /** True unless {@link #OUTCOME}: whether a value of this category may be consumed as a raw input. */
    public boolean isValidCalculationInput() {
        return validCalculationInput;
    }
}
