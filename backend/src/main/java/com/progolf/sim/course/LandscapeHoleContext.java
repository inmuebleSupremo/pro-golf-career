package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** Immutable local V6 presentation context. It is explicitly non-authoritative for gameplay. */
public record LandscapeHoleContext(String courseIdentity, LandscapeRelationship relationship,
                                   List<LandscapeContextFeature> features) {
    public LandscapeHoleContext {
        if (courseIdentity == null || courseIdentity.isBlank()) throw new IllegalArgumentException("course identity is required");
        Objects.requireNonNull(relationship, "relationship");
        features = List.copyOf(Objects.requireNonNull(features, "features"));
    }
}
