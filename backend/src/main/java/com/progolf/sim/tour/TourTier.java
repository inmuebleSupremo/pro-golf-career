package com.progolf.sim.tour;

import java.util.Optional;

/**
 * The competitive tour ladder (REQ-127), modelled on real professional golf as two levels: a Development
 * tour where new professionals prove themselves, feeding a top Pro tour — the pinnacle, where the best
 * players compete. Declaration order is the rank, lowest first. The ladder is continuous, giving a complete
 * pathway from the entry level to the top (REQ-137): a golfer earns a Pro card by finishing high on the
 * Development tour, and loses it by finishing low on the Pro tour.
 */
public enum TourTier {
    DEVELOPMENT,
    PRO;

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
