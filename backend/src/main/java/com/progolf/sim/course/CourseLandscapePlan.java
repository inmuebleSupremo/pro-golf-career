package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** The shared, deterministic V6 geographical plan. It contains no gameplay surface authority. */
public record CourseLandscapePlan(String identity, EnvironmentClassification environment, List<LandscapeFeature> features,
                                  List<HolePlacement> placements, boolean placementFallback) {
    public CourseLandscapePlan {
        if (identity == null || identity.isBlank()) throw new IllegalArgumentException("landscape identity is required");
        Objects.requireNonNull(environment, "environment");
        features = List.copyOf(Objects.requireNonNull(features, "features"));
        placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
        if (placements.size() != 18) throw new IllegalArgumentException("V6 landscape requires 18 placements");
        for (int i = 0; i < placements.size(); i++) {
            if (placements.get(i).holeNumber() != i + 1) throw new IllegalArgumentException("placements must be ordered 1..18");
        }
    }

    public HolePlacement placement(int holeNumber) {
        return placements.get(holeNumber - 1);
    }
}
