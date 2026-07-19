package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.SetupDifficulty;
import com.progolf.sim.tournament.Tier;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the balance of what each attribute is worth (spec: shot-resolution). Every attribute must move a
 * golfer's score by a meaningful amount, and none may dominate — otherwise a stat is either a wasted
 * development choice or the only one that matters, and a golfer's overall ability stops predicting results.
 *
 * <p>This exists because both failures happened. Once, course management and composure moved scoring by
 * essentially zero while irons accuracy was worth 5x any other stat, so a balanced build was a trap and mean
 * ability barely correlated with finishing. The bands below are wide enough to allow real tuning and
 * deliberate design (irons matter more than wedges; composure only bites under pressure) but tight enough to
 * fail if any stat goes dead or runaway again.
 *
 * <p>Deterministic: fixed course, fixed setup, fixed seeds — the same measurement every run.
 */
class AttributeValueBalanceTest {

    private static final int ROUNDS = 150;
    private static final int LOW_RATING = 70;
    private static final int HIGH_RATING = 95;

    /** Composure only acts under competitive pressure, so it is measured and asserted separately. */
    private static final double PRESSURE_FOR_COMPOSURE = 0.8;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(4242L, 3, 0, 0, 0, 0, 0),
                EnvironmentClassification.PARKLAND);
    }

    private static double strokesPerRound(Course c, CourseSetup setup, Attributes attrs, double pressure) {
        GolferState state = new GolferState(0.35, pressure, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        double total = 0;
        for (int r = 0; r < ROUNDS; r++) {
            for (int h = 1; h <= 18; h++) {
                total += RoundResolver.resolveHole(c.holeModel(h, 1, setup), attrs, state, Environment.calm(),
                        Strategy.BALANCED, new SeedCoordinate(999L, 2, 1, r, h, 9, 0)).totalStrokes();
            }
        }
        return total / ROUNDS;
    }

    /** Strokes/round each attribute saves when raised alone from 70 to 95, under the given pressure. */
    private static Map<Attribute, Double> attributeValues(double pressure) {
        Course c = course();
        CourseSetup setup = SetupDifficulty.forEvent(Tier.STANDARD, EventPrestige.REGULAR);
        Attributes base = Attributes.uniform(LOW_RATING);
        double baseline = strokesPerRound(c, setup, base, pressure);
        Map<Attribute, Double> values = new EnumMap<>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            values.put(a, baseline - strokesPerRound(c, setup, base.with(a, HIGH_RATING), pressure));
        }
        return values;
    }

    @Test
    void everyAlwaysOnAttributeIsWorthDevelopingAndNoneDominates() {
        Map<Attribute, Double> value = attributeValues(0.0);

        for (Attribute a : Attribute.values()) {
            if (a == Attribute.COMPOSURE) {
                continue; // situational — asserted under pressure below
            }
            assertThat(value.get(a))
                    .as("%s should be worth developing (strokes/round for +25 rating)", a)
                    .isGreaterThanOrEqualTo(0.12);
            assertThat(value.get(a))
                    .as("%s should not dominate every other stat", a)
                    .isLessThanOrEqualTo(1.60);
        }

        // A real golf hierarchy is fine and expected — approach play (irons) separates golfers more than
        // wedge or driving accuracy does, as it does in real strokes-gained data. What this guards against is
        // the catastrophe that prompted it: a spread of 20x+, where a handful of stats decided everything and
        // the rest were wasted development. ~5x is the honest, defensible hierarchy after leveling.
        double max = value.entrySet().stream().filter(e -> e.getKey() != Attribute.COMPOSURE)
                .mapToDouble(Map.Entry::getValue).max().orElseThrow();
        double min = value.entrySet().stream().filter(e -> e.getKey() != Attribute.COMPOSURE)
                .mapToDouble(Map.Entry::getValue).min().orElseThrow();
        assertThat(max / min).as("spread between the most and least valuable always-on attribute").isLessThan(5.5);
    }

    @Test
    void pairedAttributesAreWorthComparableAmounts() {
        Map<Attribute, Double> value = attributeValues(0.0);

        // Accuracy and its control twin should be in the same league, not one worth a fraction of the other.
        assertThat(value.get(Attribute.IRONS_ACCURACY) / value.get(Attribute.IRONS_CONTROL))
                .as("irons accuracy vs control").isBetween(0.5, 2.0);
        assertThat(value.get(Attribute.PUTTING_ACCURACY) / value.get(Attribute.PUTTING_PROXIMITY))
                .as("putting accuracy vs proximity").isBetween(0.5, 2.0);
    }

    @Test
    void composureIsValuableUnderPressureEvenThoughItIsIdleWhenCalm() {
        assertThat(attributeValues(0.0).get(Attribute.COMPOSURE))
                .as("composure is idle in calm play, by design").isLessThan(0.10);
        assertThat(attributeValues(PRESSURE_FOR_COMPOSURE).get(Attribute.COMPOSURE))
                .as("composure earns its keep in contention").isGreaterThanOrEqualTo(0.35);
    }
}
