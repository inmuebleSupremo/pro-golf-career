package com.progolf.sim.equipment;

/**
 * The V1 equipment categories (spec: equipment-inventory, REQ-204), each with a base cost used to price a
 * top-quality item (scaled down by an item's quality). Future versions may add categories without
 * changing this set.
 */
public enum EquipmentCategory {
    DRIVER(60_000.0),
    FAIRWAY_WOODS(30_000.0),
    HYBRIDS(25_000.0),
    IRONS(80_000.0),
    WEDGES(30_000.0),
    PUTTER(40_000.0),
    GOLF_BALL(5_000.0);

    private final double baseCost;

    EquipmentCategory(double baseCost) {
        this.baseCost = baseCost;
    }

    /** The cost of a top-quality item in this category, before quality scaling. */
    public double baseCost() {
        return baseCost;
    }
}
