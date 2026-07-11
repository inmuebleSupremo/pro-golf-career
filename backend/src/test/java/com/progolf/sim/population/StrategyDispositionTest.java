package com.progolf.sim.population;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.shot.Strategy;
import org.junit.jupiter.api.Test;

/** golfer-population: a golfer's innate strategic disposition is derived from their attributes. */
class StrategyDispositionTest {

    @Test
    void aBomberWithWeakDisciplineAttacks() {
        Attributes bomber = Attributes.uniform(50)
                .with(Attribute.DRIVING_DISTANCE, 90)
                .with(Attribute.COURSE_MANAGEMENT, 30)
                .with(Attribute.COMPOSURE, 30);
        assertThat(StrategyDisposition.fromAttributes(bomber)).isEqualTo(Strategy.AGGRESSIVE);
    }

    @Test
    void aDisciplinedComposedGolferProtectsPar() {
        Attributes grinder = Attributes.uniform(50)
                .with(Attribute.DRIVING_DISTANCE, 30)
                .with(Attribute.COURSE_MANAGEMENT, 90)
                .with(Attribute.COMPOSURE, 90);
        assertThat(StrategyDisposition.fromAttributes(grinder)).isEqualTo(Strategy.CONSERVATIVE);
    }

    @Test
    void aFlatProfilePlaysBalanced() {
        assertThat(StrategyDisposition.fromAttributes(Attributes.uniform(50))).isEqualTo(Strategy.BALANCED);
    }

    @Test
    void dispositionIsNeutralToOverallSkill() {
        // The appetite formula's weights cancel overall skill, so a flat profile is Balanced at any level.
        assertThat(StrategyDisposition.fromAttributes(Attributes.uniform(30))).isEqualTo(Strategy.BALANCED);
        assertThat(StrategyDisposition.fromAttributes(Attributes.uniform(85))).isEqualTo(Strategy.BALANCED);
    }
}
