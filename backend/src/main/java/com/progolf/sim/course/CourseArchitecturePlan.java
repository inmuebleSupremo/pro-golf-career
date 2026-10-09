package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** The selected V5 architectural identity for a complete course. */
public record CourseArchitecturePlan(CourseArchitectureProfile profile, List<HoleArchitecturePlan> holes) {
    public CourseArchitecturePlan {
        Objects.requireNonNull(profile, "profile");
        holes = List.copyOf(Objects.requireNonNull(holes, "holes"));
        if (holes.size() != 18) throw new IllegalArgumentException("V5 architecture plan requires 18 holes");
    }
}
