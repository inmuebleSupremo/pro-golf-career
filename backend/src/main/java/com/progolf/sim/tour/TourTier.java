package com.progolf.sim.tour;

import java.util.Optional;

/**
 * The ordered competitive tier ladder (REQ-127). Declaration order is the rank, lowest first: higher
 * tiers hold stronger competition. The ladder is continuous, giving a complete pathway from the
 * entry-level tier to the elite tier (REQ-137).
 */
public enum TourTier {
    DEVELOPMENT,
    SECONDARY,
    PRIMARY,
    ELITE;

    /** Rank, 0 = lowest tier. */
    public int rank() {
        return ordinal();
    }

    /** The tier immediately above (for promotion), if any. */
    public Optional<TourTier> above() {
        return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
    }

    /** The tier immediately below (for relegation), if any. */
    public Optional<TourTier> below() {
        return ordinal() > 0 ? Optional.of(values()[ordinal() - 1]) : Optional.empty();
    }
}
