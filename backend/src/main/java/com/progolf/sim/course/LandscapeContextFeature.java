package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** Read-only local projection of one shared V6 feature for rendering context. */
public record LandscapeContextFeature(String id, LandscapeFeatureKind kind, List<Position2d> boundary) {
    public LandscapeContextFeature {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("context feature id is required");
        Objects.requireNonNull(kind, "kind");
        boundary = List.copyOf(Objects.requireNonNull(boundary, "boundary"));
    }
}
