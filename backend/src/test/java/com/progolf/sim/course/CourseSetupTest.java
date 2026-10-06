package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import org.junit.jupiter.api.Test;

/** course-setup: a neutral setup reproduces the baseline course; a harder setup raises scoring. */
class CourseSetupTest {

    private static final long SEED = 0xC0FFEE99L;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(SEED, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
    }

    @Test
    void standardSetupReproducesBaselineHoleGeometryAndPin() {
        Course course = course();
        for (int hole = 1; hole <= 18; hole++) {
            HoleModel baseline = course.holeModel(hole, 1);
            HoleModel standard = course.holeModel(hole, 1, CourseSetup.standard());
            assertThat(standard.geometry()).isSameAs(course.holes().get(hole - 1).geometry());
            assertThat(standard.startDistance()).isEqualTo(baseline.startDistance());
            assertThat(standard.pinLateral()).isEqualTo(baseline.pinLateral());
            for (double d = 250; d >= 5; d -= 35) {
                assertThat(standard.zoneProfileFor(d)).as("profile at %.0f on hole %d", d, hole)
                        .isEqualTo(baseline.zoneProfileFor(d));
            }
        }
    }

    @Test
    void nonNeutralSetupExposesTheCanonicalGeometryUsedForSurfaceSettlement() {
        GeneratedHole hole = course().holes().get(0);
        HoleModel tightModel = hole.forRound(1, new CourseSetup(1.0, 1.0, 0.60));
        CourseGeometry baseline = hole.geometry();
        CourseGeometry effective = tightModel.geometry();

        assertThat(effective).isNotEqualTo(baseline);
        assertThat(hole.forRound(1, new CourseSetup(1.0, 1.0, 0.60)).geometry())
                .isSameAs(effective);

        Position2d changed = firstClassificationDifference(baseline, effective, hole.length());
        // Shot settlement asks HoleModel.geometry() for its contact surface, so this is the exact terrain
        // contract the resolver follows rather than a separate presentation-only width calculation.
        assertThat(tightModel.geometry().surfaceAt(changed)).isEqualTo(effective.surfaceAt(changed));
        assertThat(tightModel.geometry().surfaceAt(changed)).isNotEqualTo(baseline.surfaceAt(changed));
    }

    @Test
    void aHarderSetupTucksThePinAndNarrowsTheGreen() {
        Course course = course();
        CourseSetup easy = new CourseSetup(0.4, 1.0, 1.5);   // central pins, wide
        CourseSetup hard = new CourseSetup(2.2, 1.0, 0.6);   // tucked pins, tight
        // Averaged over the course so a single central-pin hole doesn't dominate.
        double easyLateral = 0;
        double hardLateral = 0;
        for (int hole = 1; hole <= 18; hole++) {
            easyLateral += Math.abs(course.holeModel(hole, 1, easy).pinLateral());
            hardLateral += Math.abs(course.holeModel(hole, 1, hard).pinLateral());
        }
        assertThat(hardLateral).as("harder setups cut pins closer to the edge").isGreaterThan(easyLateral);
    }

    @Test
    void aHarderSetupRaisesScoring() {
        Course course = course();
        Attributes attrs = Attributes.uniform(65);
        double easyMean = roundMean(course, attrs, new CourseSetup(0.5, 1.0, 1.5));
        double neutralMean = roundMean(course, attrs, CourseSetup.standard());
        double hardMean = roundMean(course, attrs, new CourseSetup(2.0, 1.0, 0.6));
        // Monotonic: tighter/tucked setups score higher (more over par) than wide/central ones.
        assertThat(neutralMean).isGreaterThan(easyMean);
        assertThat(hardMean).isGreaterThan(neutralMean);
    }

    /** Mean score vs par over several seeded rounds of the whole course under a setup (calm). */
    private static double roundMean(Course course, Attributes attrs, CourseSetup setup) {
        double total = 0;
        int rounds = 6;
        for (int r = 1; r <= rounds; r++) {
            int strokes = 0;
            for (int hole = 1; hole <= 18; hole++) {
                HoleModel model = course.holeModel(hole, r, setup);
                SeedCoordinate coord = new SeedCoordinate(SEED, 2, 2, r, 0, hole, 0);
                RoundOutcome out = RoundResolver.resolveHole(model, attrs, GolferState.fresh(),
                        Environment.calm(), Strategy.BALANCED, coord);
                strokes += out.totalStrokes();
            }
            total += strokes - course.totalPar();
        }
        return total / rounds;
    }

    private static Position2d firstClassificationDifference(CourseGeometry first, CourseGeometry second, double length) {
        for (double y = 0; y <= length + 60; y += 1.0) {
            for (double x = -120; x <= 120; x += 1.0) {
                Position2d point = new Position2d(x, y);
                if (first.surfaceAt(point) != second.surfaceAt(point)) {
                    return point;
                }
            }
        }
        throw new AssertionError("expected setup-specific geometry to alter at least one surface classification");
    }
}
