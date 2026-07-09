package com.progolf.sim.tour;

import java.util.Objects;

/**
 * A Tour (REQ-126): a persistent, independently-identified competitive entity at one {@link TourTier}.
 * Version 1 runs one Tour per tier (a linear ladder).
 */
public record Tour(String id, String name, TourTier tier) {

    public Tour {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tier, "tier");
    }
}
