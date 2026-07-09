package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import org.junit.jupiter.api.Test;

/** player-development (modified): development may be enhanced by a support factor (a coach). */
class ProgressionSupportTest {

    private static void assertSameRatings(Attributes actual, Attributes expected) {
        for (Attribute a : Attribute.values()) {
            assertThat(actual.get(a)).as(a.name()).isEqualTo(expected.get(a));
        }
    }

    @Test
    void unsupportedOverloadMatchesTheBaseline() {
        Attributes attrs = Attributes.uniform(50);
        assertSameRatings(ProgressionEngine.develop(attrs, 20, 1.0), ProgressionEngine.develop(attrs, 20));
    }

    @Test
    void supportFactorIncreasesDevelopment() {
        // Use a late-career golfer, whose scarce Development Points sit below the per-season cap, so extra
        // coaching points buy more development (a young golfer already saturates the cap).
        Attributes attrs = Attributes.uniform(50);
        double baseline = ProgressionEngine.overallAbility(ProgressionEngine.develop(attrs, 40, 1.0));
        double supported = ProgressionEngine.overallAbility(ProgressionEngine.develop(attrs, 40, 3.0));
        assertThat(supported).isGreaterThan(baseline);
    }
}
