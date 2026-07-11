package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import org.junit.jupiter.api.Test;

/** staff-influence / shot-resolution: caddie (strategic) and psychologist (mental) support shape the shot. */
class StaffSupportShotTest {

    private static final long WORLD = 0x57AFF;
    private static final Course COURSE =
            CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);

    /** Total strokes for an 18-hole round for a golfer in the given state, tagged by a round seed. */
    private static int roundTotal(GolferState state, int roundTag) {
        int total = 0;
        for (int hole = 1; hole <= 18; hole++) {
            SeedCoordinate coord = new SeedCoordinate(WORLD, 1, roundTag, 1, 0, hole, 0);
            total += RoundResolver.resolveHole(COURSE.holeModel(hole, 1), Attributes.uniform(60), state,
                    Environment.calm(), Strategy.BALANCED, coord).totalStrokes();
        }
        return total;
    }

    private static int sumOfRounds(GolferState state, int rounds) {
        int sum = 0;
        for (int r = 1; r <= rounds; r++) {
            sum += roundTotal(state, r);
        }
        return sum;
    }

    @Test
    void mentalSupportHasNoEffectWithoutFatigue() {
        // effectiveFatigue = fatigue * (1 - mentalSupport); with no fatigue, mental support changes nothing.
        GolferState fresh = new GolferState(0.0, 0.0);
        GolferState mental = new GolferState(0.0, 0.0, 0.0, 0.0, 0.6, 0.0);
        for (int r = 1; r <= 40; r++) {
            assertThat(roundTotal(mental, r)).isEqualTo(roundTotal(fresh, r));
        }
    }

    @Test
    void mentalSupportSoftensFatigueInAggregate() {
        GolferState tired = new GolferState(0.9, 0.0);
        GolferState tiredWithPsych = new GolferState(0.9, 0.0, 0.0, 0.0, 0.6, 0.0);
        int base = sumOfRounds(tired, 120);
        int supported = sumOfRounds(tiredWithPsych, 120);
        assertThat(supported).isLessThanOrEqualTo(base); // resilience never hurts a tired golfer
        assertThat(supported).isLessThan(base);          // and it measurably helps
    }

    @Test
    void strategicSupportReducesMishitsInAggregate() {
        GolferState plain = GolferState.fresh();
        GolferState withCaddie = new GolferState(0.0, 0.0, 0.0, 0.0, 0.0, 0.6);
        int base = sumOfRounds(plain, 120);
        int supported = sumOfRounds(withCaddie, 120);
        assertThat(supported).isLessThanOrEqualTo(base); // fewer blow-ups
        assertThat(supported).isLessThan(base);
    }

    @Test
    void supportIsRangeChecked() {
        assertThatThrownBy(() -> new GolferState(0.0, 0.0, 0.0, 0.0, 1.5, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GolferState(0.0, 0.0, 0.0, 0.0, 0.0, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
