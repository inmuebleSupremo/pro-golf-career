package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Player-aging/development spec: a golfer keeps getting better for as long as a real one does. Ability rises
 * deep into a career rather than topping out in the early 30s, and inside a normal career only raw power
 * actually fades — a 45-year-old is a better player than they were at 33, just a shorter one.
 */
class ProgressionTrajectoryTest {

    /** A talented golfer's ability at each age, developed and aged season by season toward their ceiling. */
    private static Map<Integer, Double> abilityByAge(int from, int to) {
        Attributes potential = Attributes.uniform(88);
        Attributes attrs = Maturity.abilityAt(potential, from);
        Map<Integer, Double> ability = new TreeMap<>();
        for (int age = from; age <= to; age++) {
            attrs = ProgressionEngine.develop(attrs, potential, age);
            attrs = ProgressionEngine.age(attrs, age);
            ability.put(age, ProgressionEngine.overallAbility(attrs));
        }
        return ability;
    }

    @Test
    void abilityKeepsRisingThroughAGolfersFortiesInsteadOfPeakingInTheEarlyThirties() {
        Map<Integer, Double> ability = abilityByAge(21, 58);

        // The regression this guards: ability used to top out at ~33 and fall every season after, so a
        // career's best years were always its earliest ones.
        assertThat(ability.get(40)).isGreaterThan(ability.get(33));
        assertThat(ability.get(45)).isGreaterThan(ability.get(40));
        // No sustained backslide anywhere from the mid-20s to the mid-40s. Compared over a five-year window
        // rather than season to season: a single season may still tick down a fraction when driving distance
        // rounds off a point and that season's development lands elsewhere, which is the power fade working
        // as intended — the regression was a decade-long slide, not a rounding blip.
        for (int age = 26; age <= 45; age++) {
            assertThat(ability.get(age))
                    .as("age %d should be better off than age %d", age, age - 5)
                    .isGreaterThan(ability.get(age - 5));
        }
    }

    @Test
    void onlyRawPowerFadesInsideANormalCareerWhileSkillAndJudgmentSharpen() {
        // Aging alone, so the shape of the curves is isolated from where development points happen to land.
        Attributes at30 = Attributes.uniform(60);
        for (int age = 21; age <= 30; age++) {
            at30 = ProgressionEngine.age(at30, age);
        }
        Attributes at50 = at30;
        for (int age = 31; age <= 50; age++) {
            at50 = ProgressionEngine.age(at50, age);
        }

        assertThat(at50.get(Attribute.DRIVING_DISTANCE)).isLessThan(at30.get(Attribute.DRIVING_DISTANCE));
        // ...but everything else is sharper at 50 than it was at 30, including finding the fairway.
        assertThat(at50.get(Attribute.DRIVING_ACCURACY)).isGreaterThan(at30.get(Attribute.DRIVING_ACCURACY));
        assertThat(at50.get(Attribute.PUTTING_ACCURACY)).isGreaterThan(at30.get(Attribute.PUTTING_ACCURACY));
        assertThat(at50.get(Attribute.COURSE_MANAGEMENT)).isGreaterThan(at30.get(Attribute.COURSE_MANAGEMENT));
        for (Attribute a : Attribute.values()) {
            assertThat(at50.get(a)).isBetween(0, 100);
        }
    }

    @Test
    void applyingASeasonIsAPureDeterministicFunction() {
        Attributes attrs = Attributes.uniform(62);
        Attributes potential = Attributes.uniform(88);
        Attributes once = ProgressionEngine.age(ProgressionEngine.develop(attrs, potential, 29), 29);
        Attributes twice = ProgressionEngine.age(ProgressionEngine.develop(attrs, potential, 29), 29);
        for (Attribute a : Attribute.values()) {
            assertThat(once.get(a)).isEqualTo(twice.get(a));
        }
    }
}
