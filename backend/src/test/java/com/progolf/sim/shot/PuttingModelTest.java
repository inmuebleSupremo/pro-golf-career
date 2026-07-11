package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.support.Fixtures;
import org.junit.jupiter.api.Test;

/** shot-resolution (putting): a shot from the green holes out via the make-probability model. */
class PuttingModelTest {

    private static final int N = 5000;

    /** A putt of the given length (yards) from the green, seeded at shot {@code n}. */
    private static ShotContext putt(Attributes attrs, double distanceYd, Environment env, int n) {
        return new ShotContext(attrs, GolferState.fresh(), env, distanceYd,
                Fixtures.standardProfile(),
                ShotDecision.straight(Club.PUTTER, distanceYd, Strategy.BALANCED),
                Fixtures.coordinate().withShot(n), Surface.GREEN);
    }

    private static double makeRate(Attributes attrs, double distanceYd) {
        int holed = 0;
        for (int i = 1; i <= N; i++) {
            ShotOutcome o = ShotResolver.resolveShot(putt(attrs, distanceYd, Environment.calm(), i));
            if (o.distanceRemaining() <= SimConstants.HOLED_THRESHOLD) {
                holed++;
            }
        }
        return (double) holed / N;
    }

    @Test
    void shortPuttsHoleOutNearCertainly() {
        // ~2 feet: a tap-in should fall almost every time.
        assertThat(makeRate(Attributes.uniform(60), 0.6)).isGreaterThan(0.95);
    }

    @Test
    void makeRateFallsWithDistance() {
        Attributes attrs = Attributes.uniform(60);
        double close = makeRate(attrs, 1.0);   // ~3 ft
        double mid = makeRate(attrs, 3.0);     // ~9 ft
        double far = makeRate(attrs, 6.0);     // ~18 ft
        assertThat(close).isGreaterThan(mid);
        assertThat(mid).isGreaterThan(far);
    }

    @Test
    void betterPutterHolesMoreFromTheSameDistance() {
        Attributes weak = Attributes.uniform(50).with(Attribute.PUTTING_ACCURACY, 25);
        Attributes strong = Attributes.uniform(50).with(Attribute.PUTTING_ACCURACY, 95);
        assertThat(makeRate(strong, 3.0)).isGreaterThan(makeRate(weak, 3.0));
    }

    @Test
    void puttsAreImmuneToWindAndLie() {
        // The same putt under calm and under a strong storm must resolve identically — a putt is sheltered.
        Attributes attrs = Attributes.uniform(60);
        Environment storm = new Environment(15.0, 20.0, 0.6);
        for (int i = 1; i <= 200; i++) {
            ShotOutcome calm = ShotResolver.resolveShot(putt(attrs, 4.0, Environment.calm(), i));
            ShotOutcome windy = ShotResolver.resolveShot(putt(attrs, 4.0, storm, i));
            assertThat(windy).isEqualTo(calm);
        }
    }

    @Test
    void aMissedPuttAlwaysConvergesTowardTheHole() {
        Attributes attrs = Attributes.uniform(55);
        double start = 5.0;
        for (int i = 1; i <= N; i++) {
            ShotOutcome o = ShotResolver.resolveShot(putt(attrs, start, Environment.calm(), i));
            // Whether holed (0) or missed, the ball never finishes further from the hole than it started.
            assertThat(o.distanceRemaining()).isLessThan(start);
            assertThat(o.finalSurface()).isEqualTo(Surface.GREEN);
            assertThat(o.hazardEntered()).isFalse();
            assertThat(o.strokes()).isEqualTo(1);
        }
    }

    @Test
    void aFullRoundHolesOutWithRealisticPutts() {
        Course course = CourseGenerator.generate(
                new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 1, 1, 0), EnvironmentClassification.PARKLAND);
        Attributes attrs = Attributes.uniform(62);
        int totalPutts = 0;
        for (int hole = 1; hole <= 18; hole++) {
            SeedCoordinate coord = new SeedCoordinate(0xC0FFEEL, 1, 1, 1, 3, hole, 0);
            RoundOutcome out = RoundResolver.resolveHole(course.holeModel(hole, 1), attrs,
                    GolferState.fresh(), Environment.calm(), Strategy.BALANCED, coord);
            // Every hole holes out well within the shot cap (no orbiting).
            assertThat(out.shots().size()).isLessThan(SimConstants.MAX_SHOTS_PER_HOLE);
            ShotOutcome last = out.shots().get(out.shots().size() - 1);
            assertThat(last.distanceRemaining()).isLessThanOrEqualTo(SimConstants.HOLED_THRESHOLD);
            totalPutts += HoleStats.of(out.shots(), course.holes().get(hole - 1).par()).putts();
        }
        // A believable putting round, not the pre-fix ~140+.
        assertThat(totalPutts).isBetween(22, 38);
    }
}
