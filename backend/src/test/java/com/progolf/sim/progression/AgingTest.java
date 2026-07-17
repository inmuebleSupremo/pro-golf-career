package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import org.junit.jupiter.api.Test;

/** Player-aging spec: attribute-specific curves (non-uniform), gradual change, and career stages. */
class AgingTest {

    @Test
    void agingClassesPeakAtDifferentAges() {
        assertThat(AgingClass.PHYSICAL.peakAge()).isLessThan(AgingClass.SKILL.peakAge());
        assertThat(AgingClass.SKILL.peakAge()).isLessThan(AgingClass.MENTAL.peakAge());
    }

    @Test
    void physicalDeclinesWhileMentalRisesAcrossAges() {
        Attributes attrs = Attributes.uniform(60);
        for (int age = 25; age <= 55; age++) {
            attrs = ProgressionEngine.age(attrs, age); // aging only, no development
        }
        // Non-uniform: a physical attribute has faded, a mental one has grown.
        assertThat(attrs.get(Attribute.DRIVING_DISTANCE)).isLessThan(60);
        assertThat(attrs.get(Attribute.COURSE_MANAGEMENT)).isGreaterThan(60);
    }

    @Test
    void perSeasonAgingIsGradual() {
        for (int age = 18; age <= 64; age++) {
            for (Attribute a : Attribute.values()) {
                assertThat(Math.abs(AgingCurves.seasonDelta(a, age))).isLessThanOrEqualTo(2);
            }
        }
    }

    @Test
    void careerStageDerivesFromAge() {
        assertThat(CareerStage.of(20)).isEqualTo(CareerStage.DEVELOPMENT);
        assertThat(CareerStage.of(28)).isEqualTo(CareerStage.PRIME);
        assertThat(CareerStage.of(42)).isEqualTo(CareerStage.PRIME); // a golfer in their early 40s is not winding down
        assertThat(CareerStage.of(48)).isEqualTo(CareerStage.LATE_CAREER);
    }
}
