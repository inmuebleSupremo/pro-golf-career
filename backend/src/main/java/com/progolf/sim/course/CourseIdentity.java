package com.progolf.sim.course;

import java.util.Objects;

/**
 * A Course's unique, stable identity (REQ-079). Contains only descriptive identity — no tournament
 * state (leaderboards, prize money, rankings, competitors, scores are all forbidden here, REQ-083).
 */
public record CourseIdentity(String id, String name, String region, EnvironmentClassification classification, String style) {

    public CourseIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(style, "style");
    }
}
