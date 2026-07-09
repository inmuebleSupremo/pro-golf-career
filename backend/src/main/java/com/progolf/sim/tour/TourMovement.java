package com.progolf.sim.tour;

import java.util.Objects;

/**
 * An immutable record of a golfer's Tour membership change (REQ-130/131). {@code fromTier} is null for a
 * golfer's initial qualification into the system.
 */
public record TourMovement(String golferId, TourTier fromTier, TourTier toTier, int season, MovementType type) {

    public TourMovement {
        Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(toTier, "toTier");
        Objects.requireNonNull(type, "type");
    }
}
