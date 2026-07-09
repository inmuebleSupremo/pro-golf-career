package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import org.junit.jupiter.api.Test;

/** Player-aging/development spec: a multi-decade trajectory rises to a peak then declines, staying in range. */
class ProgressionTrajectoryTest {

    @Test
    void overACareerAbilityRisesToAPeakThenDeclinesGraduallyInRange() {
        Attributes attrs = Attributes.uniform(55);
        double at20 = ProgressionEngine.overallAbility(attrs);

        double peak = at20;
        for (int age = 21; age <= 34; age++) { // development + prime
            attrs = ProgressionEngine.develop(attrs, age);
            attrs = ProgressionEngine.age(attrs, age);
            peak = Math.max(peak, ProgressionEngine.overallAbility(attrs));
        }
        double atPrimeEnd = ProgressionEngine.overallAbility(attrs);
        Attributes primeAttrs = attrs;

        for (int age = 35; age <= 58; age++) { // late career decline
            attrs = ProgressionEngine.develop(attrs, age);
            attrs = ProgressionEngine.age(attrs, age);
        }
        double atOld = ProgressionEngine.overallAbility(attrs);

        // Rises to a peak above the starting level ...
        assertThat(peak).isGreaterThan(at20);
        // ... and declines from the prime by old age (physical fade outpaces reduced late development).
        assertThat(atOld).isLessThan(atPrimeEnd);
        // Non-uniform: a physical attribute is below its prime while a mental one is at or above it.
        assertThat(attrs.get(Attribute.DRIVING_DISTANCE)).isLessThan(primeAttrs.get(Attribute.DRIVING_DISTANCE));
        assertThat(attrs.get(Attribute.COURSE_MANAGEMENT)).isGreaterThanOrEqualTo(primeAttrs.get(Attribute.COURSE_MANAGEMENT));
        // Everything stays within range throughout.
        for (Attribute a : Attribute.values()) {
            assertThat(attrs.get(a)).isBetween(0, 100);
        }
    }

    @Test
    void applyingASeasonIsAPureDeterministicFunction() {
        Attributes attrs = Attributes.uniform(62);
        Attributes once = ProgressionEngine.age(ProgressionEngine.develop(attrs, 29), 29);
        Attributes twice = ProgressionEngine.age(ProgressionEngine.develop(attrs, 29), 29);
        for (Attribute a : Attribute.values()) {
            assertThat(once.get(a)).isEqualTo(twice.get(a));
        }
    }
}
