package com.progolf.sim.core;

import java.util.Objects;

/**
 * Guarded development operations. Only {@link ValueCategory#ATTRIBUTE} values may be developed
 * (spec: numerical-model "Development targets only Attributes").
 */
public final class Development {

    private Development() {
    }

    /** Rejects any attempt to develop a non-trainable category. */
    public static void assertTrainable(ValueCategory category) {
        Objects.requireNonNull(category, "category");
        if (!category.isTrainable()) {
            throw new IllegalArgumentException("Cannot apply development to category " + category
                    + "; only ATTRIBUTE values are trainable");
        }
    }

    /**
     * Applies {@code points} of development to {@code attribute}, returning updated attributes.
     * The result is clamped to the stored 0-100 range.
     */
    public static Attributes applyPoints(Attributes base, Attribute attribute, int points) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(attribute, "attribute");
        assertTrainable(attribute.category());
        return base.with(attribute, base.get(attribute) + points);
    }
}
