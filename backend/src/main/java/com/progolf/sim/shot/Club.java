package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;

/**
 * The clubs available for a shot. Each declares a reference carry distance and the two attributes that
 * primarily govern its lateral consistency and its distance consistency/reach (REQ-053).
 */
public enum Club {
    DRIVER(290, Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE, 1.0),
    FAIRWAY_WOOD(245, Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE, 1.0),
    HYBRID(220, Attribute.IRONS_ACCURACY, Attribute.DRIVING_DISTANCE, 1.0),
    IRON(180, Attribute.IRONS_ACCURACY, Attribute.IRONS_CONTROL, 1.0),
    WEDGE(110, Attribute.WEDGES, Attribute.WEDGES, 1.0),
    // Putting spreads far more per yard than full shots: this is what makes mid-range putts miss at
    // realistic rates (near-certain tap-ins, ~1-in-6 from 20 feet) while the small floor keeps tap-ins in.
    PUTTER(20, Attribute.PUTTING_ACCURACY, Attribute.PUTTING_PROXIMITY, 2.6);

    private final double baseDistance;
    private final Attribute lateralAttribute;
    private final Attribute distanceAttribute;
    private final double dispersionMultiplier;

    Club(double baseDistance, Attribute lateralAttribute, Attribute distanceAttribute, double dispersionMultiplier) {
        this.baseDistance = baseDistance;
        this.lateralAttribute = lateralAttribute;
        this.distanceAttribute = distanceAttribute;
        this.dispersionMultiplier = dispersionMultiplier;
    }

    /** Multiplier applied to the distance-scaled dispersion term (not the floor). */
    public double dispersionMultiplier() {
        return dispersionMultiplier;
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
