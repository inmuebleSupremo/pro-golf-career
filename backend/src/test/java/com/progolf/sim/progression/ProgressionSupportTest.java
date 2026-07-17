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

    /** Ample headroom, so these tests exercise the support factor rather than the potential ceiling. */
    private static final Attributes UNCAPPED = Attributes.uniform(100);

    @Test
    void unsupportedOverloadMatchesTheBaseline() {
        Attributes attrs = Attributes.uniform(50);
        assertSameRatings(ProgressionEngine.develop(attrs, UNCAPPED, 20, 1.0),
                ProgressionEngine.develop(attrs, UNCAPPED, 20));
    }

    @Test
    void supportFactorIncreasesDevelopment() {
        // A coach's extra points only buy development when the golfer is not already saturating the
        // per-season cap, so use a rating high enough that each point is expensive.
        Attributes attrs = Attributes.uniform(90);
        double baseline = ProgressionEngine.overallAbility(ProgressionEngine.develop(attrs, UNCAPPED, 40, 1.0));
        double supported = ProgressionEngine.overallAbility(ProgressionEngine.develop(attrs, UNCAPPED, 40, 3.0));
        assertThat(supported).isGreaterThan(baseline);
    }

    @Test
    void noAmountOfCoachingPushesAGolferPastTheirPotential() {
        Attributes potential = Attributes.uniform(60);
        Attributes attrs = Attributes.uniform(59);

        // A season's coaching worth a hundredfold the usual points, on a golfer one point from their ceiling.
        Attributes developed = ProgressionEngine.develop(attrs, potential, 22, 100.0);

        for (Attribute a : Attribute.values()) {
            assertThat(developed.get(a)).as(a.name()).isLessThanOrEqualTo(potential.get(a));
        }
        // The attributes the points did reach are at the ceiling, not past it.
        assertThat(Attribute.values()).anySatisfy(a -> assertThat(developed.get(a)).isEqualTo(60));
    }
}
