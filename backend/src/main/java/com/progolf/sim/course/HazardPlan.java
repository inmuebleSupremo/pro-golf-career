package com.progolf.sim.course;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable V4 semantic hazard plan associated with a generated hole, not terrain authority. */
public record HazardPlan(List<HazardFeature> features) {
    public HazardPlan {
        features = List.copyOf(Objects.requireNonNull(features, "features"));
        if (features.isEmpty()) throw new IllegalArgumentException("V4 requires at least one intentional hazard feature");
        if (features.size() > 4) throw new IllegalArgumentException("V4 hazard budget is at most four features per hole");
        Set<String> unique = new HashSet<>();
        for (HazardFeature feature : features) {
            String key = feature.role() + ":" + feature.anchor() + ":" + feature.side();
            if (!unique.add(key)) throw new IllegalArgumentException("duplicate V4 hazard feature: " + key);
        }
    }
}
