package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import org.junit.jupiter.api.Test;

/** shot-resolution pressure: pressure worsens shots, Composure resists it, a psychologist relieves it. */
class PressureShotEffectTest {

    private static final Course COURSE = CourseGenerator.generate(
            new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 1, 1, 0), EnvironmentClassification.PARKLAND);

    /** Mean vs-par over N rounds for a golfer at the given pressure / mental support. */
    private static double mean(Attributes attrs, double pressure, double mentalSupport) {
        int n = 50;
        double sum = 0;
        for (int s = 1; s <= n; s++) {
            GolferState state = new GolferState(0.0, pressure, 0, 0, mentalSupport, 0, 0, 0);
            int strokes = 0, par = 0;
            for (int hole = 1; hole <= 18; hole++) {
                SeedCoordinate coord = new SeedCoordinate(0xC0FFEEL, 1, 1, 1, s, hole, 0);
                RoundOutcome out = RoundResolver.resolveHole(COURSE.holeModel(hole, 1), attrs, state,
                        Environment.calm(), Strategy.BALANCED, coord);
                strokes += out.totalStrokes();
                par += COURSE.holes().get(hole - 1).par();
            }
            sum += strokes - par;
        }
        return sum / n;
    }

    @Test
    void pressureWorsensScoresForANervyGolfer() {
        Attributes nervy = Attributes.uniform(62).with(Attribute.COMPOSURE, 25);
        assertThat(mean(nervy, 1.0, 0.0)).isGreaterThan(mean(nervy, 0.0, 0.0));
    }

    @Test
    void composureResistsPressure() {
        Attributes nervy = Attributes.uniform(62).with(Attribute.COMPOSURE, 25);
        Attributes composed = Attributes.uniform(62).with(Attribute.COMPOSURE, 95);
        double nervyChoke = mean(nervy, 1.0, 0.0) - mean(nervy, 0.0, 0.0);
        double composedChoke = mean(composed, 1.0, 0.0) - mean(composed, 0.0, 0.0);
        assertThat(composedChoke).isLessThan(nervyChoke); // the composed golfer is far less affected
    }

    @Test
    void aPsychologistRelievesPressure() {
        Attributes nervy = Attributes.uniform(62).with(Attribute.COMPOSURE, 25);
        assertThat(mean(nervy, 1.0, 0.6)).isLessThan(mean(nervy, 1.0, 0.0)); // support softens the choke
    }

    @Test
    void zeroPressureIsNeutralRegardlessOfComposure() {
        // With no pressure, Composure is irrelevant: the two golfers play byte-identically (neutrality).
        Attributes nervy = Attributes.uniform(62).with(Attribute.COMPOSURE, 25);
        Attributes composed = Attributes.uniform(62).with(Attribute.COMPOSURE, 95);
        assertThat(mean(nervy, 0.0, 0.0)).isEqualTo(mean(composed, 0.0, 0.0));
    }
}
