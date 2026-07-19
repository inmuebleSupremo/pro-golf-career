package com.progolf.sim.shot;

import com.progolf.sim.core.Attribute;

/**
 * The clubs available for a shot. Each declares a reference carry distance, the two attributes that
 * primarily govern its lateral consistency and its distance consistency/reach (REQ-053), and independent
 * per-club lateral and distance dispersion multipliers so each club has its own accuracy profile — a driver
 * sprays wider off the tee while a wedge is a precision club — calibrated to realistic driving-accuracy and
 * greens-in-regulation rates (spec: shot-resolution).
 */
public enum Club {
    //                                                                       lateral, distance dispersion
    DRIVER(290, Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE,        0.87,   1.15),
    FAIRWAY_WOOD(245, Attribute.DRIVING_ACCURACY, Attribute.DRIVING_DISTANCE,  0.95,   1.10),
    HYBRID(220, Attribute.IRONS_ACCURACY, Attribute.DRIVING_DISTANCE,          1.00,   1.05),
    // Iron dispersion is split more evenly between line and length than it was (1.22/1.00). The old split put
    // most of an approach's error on the offline axis, which is why irons accuracy dwarfed every other stat —
    // it rode the most-played shot's dominant axis. Approaches now reward distance control (irons control)
    // roughly as much as line, so a golfer's two iron stats are worth developing together.
    IRON(180, Attribute.IRONS_ACCURACY, Attribute.IRONS_CONTROL,               1.05,   1.20),
    // Wedge dispersion was the lowest of any club (0.85/0.80), which made wedge skill nearly worthless to
    // develop — a scoring club that did not separate golfers. Raised so wedge play matters, while staying
    // below the longer clubs: a wedge is still the most precise club in the bag, just not a free green.
    WEDGE(110, Attribute.WEDGES, Attribute.WEDGES,                             1.00,   0.98),
    // A putt from the green is resolved by the dedicated putting make-% model, which ignores these
    // multipliers; they only apply when a putter is used off the green (e.g. from the fringe).
    PUTTER(20, Attribute.PUTTING_ACCURACY, Attribute.PUTTING_PROXIMITY,        2.60,   2.60);

    private final double baseDistance;
    private final Attribute lateralAttribute;
    private final Attribute distanceAttribute;
    private final double lateralDispersion;
    private final double distanceDispersion;

    Club(double baseDistance, Attribute lateralAttribute, Attribute distanceAttribute,
         double lateralDispersion, double distanceDispersion) {
        this.baseDistance = baseDistance;
        this.lateralAttribute = lateralAttribute;
        this.distanceAttribute = distanceAttribute;
        this.lateralDispersion = lateralDispersion;
        this.distanceDispersion = distanceDispersion;
    }

    /** Multiplier applied to this club's lateral (offline) dispersion term (not the floor). */
    public double lateralDispersion() {
        return lateralDispersion;
    }

    /** Multiplier applied to this club's distance (long/short) dispersion term (not the floor). */
    public double distanceDispersion() {
        return distanceDispersion;
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
