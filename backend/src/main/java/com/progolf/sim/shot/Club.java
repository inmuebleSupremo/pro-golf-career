package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;

/**
 * The clubs available for a shot. Each declares a reference carry distance and the two attributes that
 * primarily govern its lateral consistency and its distance consistency/reach (REQ-053).
 */
public enum Club {
    DRIVER(290, Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE),
    FAIRWAY_WOOD(245, Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE),
    HYBRID(220, Attribute.IRONS_ACCURACY, Attribute.DRIVING_DISTANCE),
    IRON(180, Attribute.IRONS_ACCURACY, Attribute.IRONS_CONTROL),
    WEDGE(110, Attribute.WEDGES, Attribute.WEDGES),
    PUTTER(20, Attribute.PUTTING_ACCURACY, Attribute.PUTTING_PROXIMITY);

    private final double baseDistance;
    private final Attribute lateralAttribute;
    private final Attribute distanceAttribute;

    Club(double baseDistance, Attribute lateralAttribute, Attribute distanceAttribute) {
        this.baseDistance = baseDistance;
        this.lateralAttribute = lateralAttribute;
        this.distanceAttribute = distanceAttribute;
    }

    /** Reference carry distance (yards) at neutral attributes. */
    public double baseDistance() {
        return baseDistance;
    }

    /** Attribute that primarily reduces lateral dispersion for this club. */
    public Attribute lateralAttribute() {
        return lateralAttribute;
    }

    /** Attribute that primarily governs distance consistency and reach for this club. */
    public Attribute distanceAttribute() {
        return distanceAttribute;
    }
}
