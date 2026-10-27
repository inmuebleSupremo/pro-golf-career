package com.progolf.sim.world;

import com.progolf.sim.course.EnvironmentClassification;

/** Persisted selection policy for the seed-derived course pool. Enum order is part of legacy save provenance. */
public enum CoursePoolPolicy {
    LEGACY_SIX(new EnvironmentClassification[] {
            EnvironmentClassification.LINKS, EnvironmentClassification.PARKLAND,
            EnvironmentClassification.DESERT, EnvironmentClassification.MOUNTAIN,
            EnvironmentClassification.COASTAL, EnvironmentClassification.WOODLAND
    }),
    CORE_FOUR(new EnvironmentClassification[] {
            EnvironmentClassification.LINKS, EnvironmentClassification.PARKLAND,
            EnvironmentClassification.DESERT, EnvironmentClassification.WOODLAND
    });

    private final EnvironmentClassification[] classifications;

    CoursePoolPolicy(EnvironmentClassification[] classifications) {
        this.classifications = classifications;
    }

    EnvironmentClassification classificationAt(int index) {
        return classifications[index % classifications.length];
    }
}
