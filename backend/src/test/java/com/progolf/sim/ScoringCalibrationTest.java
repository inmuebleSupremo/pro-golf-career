package com.progolf.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Calibration guard: a generated field on a generated course should shoot believable golf scores
 * (roughly par, not dozens under). This locks in the shot/course tuning so a regression is caught.
 */
class ScoringCalibrationTest {

    private static final long WORLD = 0xCA11B4A7EL;

    private static int roundScore(ProfessionalGolfer g, Course course, int round, int golferId) {
        int strokes = 0;
        for (int hole = 1; hole <= 18; hole++) {
            HoleModel model = course.holeModel(hole, round);
            SeedCoordinate coord = new SeedCoordinate(WORLD, 1, 1, round, golferId, hole, 0);
            RoundOutcome out = RoundResolver.resolveHole(model, g.player().attributes(),
                    g.player().toGolferState(0.0), Environment.calm(), Strategy.BALANCED, coord);
            strokes += out.totalStrokes();
        }
        return strokes - course.totalPar();
    }

    @Test
    void generatedFieldShootsBelievableScores() {
        Course course = CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
        List<ProfessionalGolfer> field = PopulationGenerator.generate(new SeedCoordinate(WORLD, 2, 1, 0, 0, 0, 0), 80);

        List<Integer> scores = new ArrayList<>();
        for (int i = 0; i < field.size(); i++) {
            scores.add(roundScore(field.get(i), course, 1, i));
        }
        double mean = scores.stream().mapToInt(Integer::intValue).average().orElseThrow();
        int best = scores.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int worst = scores.stream().mapToInt(Integer::intValue).max().orElseThrow();

        System.out.printf("[calibration] field=%d mean=%.2f best=%d worst=%d%n", scores.size(), mean, best, worst);

        // Believable golf: the field averages near par (not dozens under), the best round is under par
        // but not absurd, weaker golfers post over-par rounds, and there is real spread. This guards
        // against a regression to the pre-calibration model (which averaged ~-16 to -27 per round).
        assertThat(mean).isBetween(-7.0, 8.0);
        assertThat(best).isBetween(-18, -2);
        assertThat(worst).isGreaterThanOrEqualTo(4).isLessThanOrEqualTo(40);
        assertThat(worst - best).isGreaterThan(8);

        // Fixed-seed calm corpus baseline: the deterministic, club-aware release model is calibrated
        // independently from the broad realism guard above.
        assertThat(mean).isBetween(-0.43, -0.42);
        assertThat(best).isEqualTo(-8);
        assertThat(worst).isEqualTo(17);
    }
}
