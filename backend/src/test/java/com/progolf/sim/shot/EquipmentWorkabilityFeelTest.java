package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import org.junit.jupiter.api.Test;

/** equipment-influence / shot-resolution: workability improves wind control, feel improves distance control. */
class EquipmentWorkabilityFeelTest {

    private static final long WORLD = 0xEEEL;
    private static final Course COURSE =
            CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.LINKS);

    private static int sumOfRounds(GolferState state, Environment env, int rounds) {
        int sum = 0;
        for (int r = 1; r <= rounds; r++) {
            for (int hole = 1; hole <= 18; hole++) {
                SeedCoordinate coord = new SeedCoordinate(WORLD, 1, r, 1, 0, hole, 0);
                sum += RoundResolver.resolveHole(COURSE.holeModel(hole, 1), Attributes.uniform(60), state,
                        env, Strategy.BALANCED, coord).totalStrokes();
            }
        }
        return sum;
    }

    private static GolferState with(double workability, double feel) {
        return new GolferState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, workability, feel);
    }

    @Test
    void feelTightensDistanceControlInCalm() {
        Environment calm = Environment.calm();
        int plain = sumOfRounds(with(0.0, 0.0), calm, 120);
        int withFeel = sumOfRounds(with(0.0, 0.4), calm, 120);
        assertThat(withFeel).isLessThanOrEqualTo(plain);
        assertThat(withFeel).isLessThan(plain); // measurably better proximity
    }

    @Test
    void workabilityHelpsInWind() {
        Environment windy = new Environment(18.0, 15.0, 1.0); // head + crosswind
        int plain = sumOfRounds(with(0.0, 0.0), windy, 120);
        int withWork = sumOfRounds(with(0.4, 0.0), windy, 120);
        assertThat(withWork).isLessThanOrEqualTo(plain);
        assertThat(withWork).isLessThan(plain);
    }

    @Test
    void workabilityHasNoEffectInCalm() {
        // No wind term to resist, so workability changes nothing — identical, seed for seed.
        Environment calm = Environment.calm();
        for (int r = 1; r <= 30; r++) {
            int a = 0;
            int b = 0;
            for (int hole = 1; hole <= 18; hole++) {
                SeedCoordinate coord = new SeedCoordinate(WORLD, 1, r, 1, 0, hole, 0);
                a += RoundResolver.resolveHole(COURSE.holeModel(hole, 1), Attributes.uniform(60),
                        with(0.0, 0.0), calm, Strategy.BALANCED, coord).totalStrokes();
                b += RoundResolver.resolveHole(COURSE.holeModel(hole, 1), Attributes.uniform(60),
                        with(0.5, 0.0), calm, Strategy.BALANCED, coord).totalStrokes();
            }
            assertThat(b).isEqualTo(a);
        }
    }
}
