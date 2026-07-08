package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import org.junit.jupiter.api.Test;

/** Course-difficulty spec: derived, bounded, deterministic, and reflects meaningful differences. */
class CourseDifficultyTest {

    private static Course generate(long id, EnvironmentClassification c) {
        return CourseGenerator.generate(new SeedCoordinate(1, 1, id, 0, 0, 0, 0), c);
    }

    @Test
    void difficultyIsDeterministicAndBounded() {
        Course course = generate(1, EnvironmentClassification.LINKS);
        CourseDifficulty a = CourseDifficulty.of(course);
        CourseDifficulty b = CourseDifficulty.of(course);
        assertThat(a).isEqualTo(b);
        assertThat(a.overall()).isBetween(0.0, 100.0);
        assertThat(a.lengthComponent()).isBetween(0.0, 100.0);
        assertThat(a.hazardComponent()).isBetween(0.0, 100.0);
    }

    @Test
    void difficultyReflectsMeaningfulDifferencesAcrossCourses() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        EnvironmentClassification[] classes = EnvironmentClassification.values();
        for (long id = 1; id <= 12; id++) {
            double overall = CourseDifficulty.of(generate(id, classes[(int) (id % classes.length)])).overall();
            min = Math.min(min, overall);
            max = Math.max(max, overall);
        }
        // Different courses genuinely differ in difficulty.
        assertThat(max).isGreaterThan(min);
    }

    @Test
    void exposureComponentTracksClassification() {
        // A high-exposure links course should score higher on exposure than a sheltered woodland course.
        double links = CourseDifficulty.of(generate(5, EnvironmentClassification.LINKS)).exposureComponent();
        double woodland = CourseDifficulty.of(generate(5, EnvironmentClassification.WOODLAND)).exposureComponent();
        assertThat(links).isGreaterThan(woodland);
    }
}
