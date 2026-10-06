package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Locked, deterministic V2 scoring smoke calibration using the existing human/AI resolution seam. */
class CourseDesignScoringTest {
    private static final long WORLD = 0xC0DEC0FFEE42L;

    @Test
    void nineProfileAnchorsResolveWithoutScoringOrSettlementPathologies() {
        List<CourseDesignProfile> anchors = List.of(
                profile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED, RecoverySeverity.BALANCED),
                profile(StrategicEmphasis.POSITIONAL, WidthTendency.EXACTING, RecoverySeverity.PENAL),
                profile(StrategicEmphasis.POSITIONAL, WidthTendency.GENEROUS, RecoverySeverity.FORGIVING),
                profile(StrategicEmphasis.RISK_REWARD, WidthTendency.EXACTING, RecoverySeverity.PENAL),
                profile(StrategicEmphasis.RISK_REWARD, WidthTendency.GENEROUS, RecoverySeverity.FORGIVING),
                profile(StrategicEmphasis.POSITIONAL, WidthTendency.BALANCED, RecoverySeverity.BALANCED),
                profile(StrategicEmphasis.RISK_REWARD, WidthTendency.BALANCED, RecoverySeverity.BALANCED),
                profile(StrategicEmphasis.BALANCED, WidthTendency.EXACTING, RecoverySeverity.PENAL),
                profile(StrategicEmphasis.BALANCED, WidthTendency.GENEROUS, RecoverySeverity.FORGIVING));
        int rounds = 0;
        int totalToPar = 0;
        int best = Integer.MAX_VALUE;
        int worst = Integer.MIN_VALUE;
        for (int anchor = 0; anchor < anchors.size(); anchor++) {
            for (int courseId = 0; courseId < 12; courseId++) {
                EnvironmentClassification classification = EnvironmentClassification.values()[courseId
                        % EnvironmentClassification.values().length];
                Course course = CourseGenerator.generateV2(new SeedCoordinate(WORLD, 1, courseId, 0, 0, 0, 0),
                        classification, anchors.get(anchor));
                for (int round = 1; round <= 12; round++) {
                    int strokes = 0;
                    for (int hole = 1; hole <= 18; hole++) {
                        RoundOutcome outcome = RoundResolver.resolveHole(course.holeModel(hole, round), Attributes.uniform(72),
                                com.progolf.sim.shot.GolferState.fresh(), Environment.calm(),
                                anchor % 2 == 0 ? Strategy.BALANCED : Strategy.AGGRESSIVE,
                                new SeedCoordinate(WORLD, 1, courseId, round, anchor, hole, 0));
                        assertThat(outcome.shots()).isNotEmpty();
                        strokes += outcome.totalStrokes();
                    }
                    int toPar = strokes - course.totalPar();
                    totalToPar += toPar;
                    best = Math.min(best, toPar);
                    worst = Math.max(worst, toPar);
                    rounds++;
                }
            }
        }
        double mean = (double) totalToPar / rounds;
        System.out.printf("[course-design-calibration] rounds=%d mean=%.3f best=%d worst=%d%n", rounds, mean, best, worst);
        assertThat(rounds).isEqualTo(1_296);
        assertThat(mean).isBetween(-8.0, 12.0);
        assertThat(best).isBetween(-20, 5);
        assertThat(worst).isBetween(5, 45);
    }

    private static CourseDesignProfile profile(StrategicEmphasis strategic, WidthTendency width,
                                               RecoverySeverity recovery) {
        return new CourseDesignProfile(strategic, width, recovery);
    }
}
