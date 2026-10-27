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
    }),
    /** Four core visual families; Parkland has both open and Woodland course identities. */
    FOUR_FAMILIES(new EnvironmentClassification[] {
            EnvironmentClassification.LINKS, EnvironmentClassification.PARKLAND,
            EnvironmentClassification.DESERT, EnvironmentClassification.WOODLAND,
            EnvironmentClassification.HEATHLAND
    });

    private final EnvironmentClassification[] classifications;

    CoursePoolPolicy(EnvironmentClassification[] classifications) {
        this.classifications = classifications;
    }

    EnvironmentClassification classificationAt(int index) {
        return classifications[index % classifications.length];
    }

    /** Explicit presentation provenance for the historically ambiguous WOODLAND identity. */
    public String familyFor(EnvironmentClassification classification) {
        if (classification == EnvironmentClassification.WOODLAND) {
            return this == FOUR_FAMILIES ? "PARKLAND" : "HEATHLAND";
        }
        return classification.name();
    }
}
