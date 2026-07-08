package com.progolf.sim.core;

import java.util.Objects;

/**
 * Guard enforcing that an {@link ValueCategory#OUTCOME} is never consumed as a raw calculation input
 * (spec: numerical-model "Outcomes are not reused as raw inputs"). An outcome must first be
 * reclassified into an explicit input category (State or Modifier).
 */
public final class CalculationInputs {

    private CalculationInputs() {
    }

    /** Rejects an attempt to use a value of a non-input category as a calculation input. */
    public static void require(ValueCategory category) {
        Objects.requireNonNull(category, "category");
        if (!category.isValidCalculationInput()) {
            throw new IllegalArgumentException("Category " + category
                    + " cannot be used as a calculation input; reclassify it first");
        }
    }
}
