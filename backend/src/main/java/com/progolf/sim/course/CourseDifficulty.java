package com.progolf.sim.course;

import java.util.Objects;

/**
 * An analytical, read-only difficulty profile derived from a Course's physical characteristics
 * (REQ-078): length, corridor narrowness, hazard density, green size, and environmental exposure.
 * It is descriptive only and never modifies player attributes or shot outcomes.
 *
 * <p>{@code overall} and each component are on a 0-100 scale (higher = harder).
 */
public record CourseDifficulty(
        double overall,
        double lengthComponent,
        double narrownessComponent,
        double hazardComponent,
        double greenComponent,
        double exposureComponent) {

    /** Computes the difficulty profile of a Course as a pure function of its generated characteristics. */
    public static CourseDifficulty of(Course course) {
        Objects.requireNonNull(course, "course");

        double totalLength = 0;
        double sumFairwayHalf = 0;
        double sumGreenHalf = 0;
        int hazards = 0;
        for (GeneratedHole h : course.holes()) {
            totalLength += h.length();
            sumFairwayHalf += h.fairwayHalfWidth();
            sumGreenHalf += h.greenHalfWidth();
            if (h.hasWater()) {
                hazards++;
            }
            if (h.hasGreensideBunker()) {
                hazards++;
            }
        }
        double avgFairwayHalf = sumFairwayHalf / 18.0;
        double avgGreenHalf = sumGreenHalf / 18.0;

        // Each component maps a characteristic onto 0-100 (higher = harder).
        double lengthComponent = clamp((totalLength - 6200.0) / 1200.0 * 100.0);
        double narrownessComponent = clamp((CourseGenConstants.FAIRWAY_HALF_MAX - avgFairwayHalf)
                / (CourseGenConstants.FAIRWAY_HALF_MAX - CourseGenConstants.FAIRWAY_HALF_MIN) * 100.0);
        double hazardComponent = clamp(hazards / 36.0 * 100.0);
        double greenComponent = clamp((CourseGenConstants.GREEN_HALF_MAX - avgGreenHalf)
                / (CourseGenConstants.GREEN_HALF_MAX - CourseGenConstants.GREEN_HALF_MIN) * 100.0);
        double exposureComponent = clamp(course.identity().classification().exposure() * 100.0);

        double overall = clamp(
                0.30 * lengthComponent
                        + 0.25 * narrownessComponent
                        + 0.20 * hazardComponent
                        + 0.15 * greenComponent
                        + 0.10 * exposureComponent);

        return new CourseDifficulty(overall, lengthComponent, narrownessComponent,
                hazardComponent, greenComponent, exposureComponent);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
