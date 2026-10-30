package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenConstants;
import org.junit.jupiter.api.Test;

class CoastalLinksWorldCompatibilityTest {
    @Test
    void historicalGeneratorEightWorldKeepsItsCourseAfterRestore() {
        WorldConfig config = new WorldConfig(8, 4, 1, 4, 8);
        World historical = World.create(103L, config,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        Course before = historical.courseAt(0);
        World restored = World.restore(103L, config, historical.snapshot());
        assertThat(restored.courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(restored.courseAt(0)).isEqualTo(before);
        assertThat(restored.snapshot()).isEqualTo(historical.snapshot());
    }
}
