package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** A simple continuous course-space landscape shape shared by more than one potential hole context. */
public record LandscapeFeature(String id, LandscapeFeatureKind kind, List<Position2d> boundary) {
    public LandscapeFeature {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("landscape feature id is required");
        Objects.requireNonNull(kind, "kind");
        boundary = List.copyOf(Objects.requireNonNull(boundary, "boundary"));
        if (boundary.size() < 3 || TerrainRegion.signedArea(boundary) <= 0.0 || !TerrainRegion.isSimplePolygon(boundary)) {
            throw new IllegalArgumentException("landscape feature boundary must be a counter-clockwise simple polygon");
        }
    }
}
