package com.progolf.sim.core;

/**
 * The nine permanent player attributes defined for Version 1 (REQ-039).
 *
 * <p>Every attribute is a {@link ValueCategory#ATTRIBUTE} on the common 0-100 scale and has exactly
 * one primary gameplay responsibility (REQ-041). No additional permanent attributes exist in V1.
 */
public enum Attribute {
    DRIVING_ACCURACY,
    DRIVING_DISTANCE,
    IRONS_ACCURACY,
    IRONS_CONTROL,
    WEDGES,
    PUTTING_ACCURACY,
    PUTTING_PROXIMITY,
    COMPOSURE,
    COURSE_MANAGEMENT;

    /** All permanent attributes are of category {@link ValueCategory#ATTRIBUTE}. */
    public ValueCategory category() {
        return ValueCategory.ATTRIBUTE;
    }
}
