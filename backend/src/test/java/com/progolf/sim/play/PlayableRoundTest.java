package com.progolf.sim.play;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** playable-round spec: an interactive shot-by-shot round, skippable, faithful to automatic resolution. */
class PlayableRoundTest {

    private static final long WORLD = 0xA11CE5F00DL;
    private static final int SEASON = 1;
    private static final long TOURN = 1;
    private static final int ROUND = 1;
    private static final int FIELD_INDEX = 0;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
    }

    private static Course v3Course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 101, 0, 0, 0, 0), EnvironmentClassification.PARKLAND,
                CourseGenConstants.V3_GENERATOR_VERSION);
    }

    private static List<HoleToPlay> holesOf(Course course) {
        List<HoleToPlay> holes = new ArrayList<>();
        for (int hole = 1; hole <= 18; hole++) {
            holes.add(new HoleToPlay(course.holeModel(hole, ROUND), course.holes().get(hole - 1).par(),
                    Environment.calm()));
        }
        return holes;
    }

    private static PlayableRound round(Course course) {
        return new PlayableRound(Attributes.uniform(55), GolferState.fresh(), holesOf(course),
                new SeedCoordinate(WORLD, SEASON, TOURN, ROUND, FIELD_INDEX, 0, 0), Strategy.BALANCED);
    }

    @Test
    void aSimmedRoundCompletesWithEighteenHoleScores() {
        PlayableRound pr = round(course());
        pr.simRound();
        assertThat(pr.isComplete()).isTrue();
        assertThat(pr.holeScores()).hasSize(18);
        assertThat(pr.totalStrokes()).isEqualTo(pr.holeScores().stream().mapToInt(Integer::intValue).sum());
        assertThat(pr.scoreVsPar()).isBetween(-30, 40); // a plausible round
    }

    @Test
    void aSimmedRoundExactlyMatchesAutomaticResolution() {
        Course course = course();
        int aiTotal = 0;
        for (int hole = 1; hole <= 18; hole++) {
            aiTotal += RoundResolver.resolveHole(
                    course.holeModel(hole, ROUND), Attributes.uniform(55), GolferState.fresh(), Environment.calm(),
                    Strategy.BALANCED, new SeedCoordinate(WORLD, SEASON, TOURN, ROUND, FIELD_INDEX, hole, 0))
                    .totalStrokes();
        }

        PlayableRound pr = round(course);
        pr.simRound();
        assertThat(pr.totalStrokes()).as("simmed round is faithful to the AI round").isEqualTo(aiTotal);
    }

    @Test
    void theHumanPlaysAShotFromTheSituation() {
        PlayableRound pr = round(course());
        ShotSituation s = pr.situation();
        assertThat(s.holeNumber()).isEqualTo(1);
        assertThat(s.shotNumber()).isEqualTo(1);
        assertThat(s.lie()).isEqualTo(Surface.TEE_BOX);
        assertThat(s.par()).isBetween(3, 5);
        assertThat(s.distanceToPin()).isGreaterThan(0.0);

        ShotOutcome first = pr.playShot(
                ShotDecision.straight(Club.DRIVER, Math.min(s.distanceToPin(), Club.DRIVER.baseDistance()), Strategy.BALANCED));
        assertThat(first.strokes()).isGreaterThanOrEqualTo(1);
        assertThat(pr.totalStrokes()).isEqualTo(first.strokes());

        pr.simRound(); // finish the rest
        assertThat(pr.isComplete()).isTrue();
        assertThat(pr.holeScores()).hasSize(18);
    }

    @Test
    void visibleHumanAndAiShotsMaterializeTracesWhileBulkSimulationDoesNot() {
        PlayableRound human = round(course());
        var cup = human.currentHoleModel().cupPosition();
        ShotOutcome humanOutcome = human.playShot(new BallStrikeIntent(ClubId.DRIVER, new AimPoint(cup.x(), cup.y())));
        assertThat(humanOutcome.trace()).isNotNull();
        assertThat(humanOutcome.trace().intendedAimPoint()).isEqualTo(new AimPoint(cup.x(), cup.y()));

        PlayableRound visibleAi = round(course());
        assertThat(visibleAi.simShot().trace()).isNotNull();

        PlayableRound background = round(course());
        background.simHole();
        assertThat(background.playedHoles().getFirst().shots()).allSatisfy(shot -> assertThat(shot.trace()).isNull());
    }

    @Test
    void simmingIsDeterministicForTheSameInputs() {
        PlayableRound a = round(course());
        PlayableRound b = round(course());
        a.simRound();
        b.simRound();
        assertThat(a.totalStrokes()).isEqualTo(b.totalStrokes());
        assertThat(a.holeScores()).isEqualTo(b.holeScores());
    }

    @Test
    void v3SimmedRoundMatchesAutomaticRouteProgressionAndManualShotRemainsPlayable() {
        Course course = v3Course();
        int automaticTotal = 0;
        for (int hole = 1; hole <= 18; hole++) {
            automaticTotal += RoundResolver.resolveHole(course.holeModel(hole, ROUND), Attributes.uniform(55),
                    GolferState.fresh(), Environment.calm(), Strategy.BALANCED,
                    new SeedCoordinate(WORLD, SEASON, TOURN, ROUND, FIELD_INDEX, hole, 0)).totalStrokes();
        }
        PlayableRound simulated = round(course);
        simulated.simRound();
        assertThat(simulated.totalStrokes()).isEqualTo(automaticTotal);

        PlayableRound manual = round(course);
        ShotSituation situation = manual.situation();
        ShotOutcome outcome = manual.playShot(ShotDecision.straight(Club.DRIVER,
                Math.min(situation.distanceToPin(), Club.DRIVER.baseDistance()), Strategy.BALANCED));
        assertThat(outcome.settlement()).isNotNull();
        assertThat(manual.ballState().lie()).isNotEqualTo(Surface.OUT_OF_BOUNDS);
    }

    @Test
    void skippingWorksAtHoleAndRoundGranularity() {
        PlayableRound pr = round(course());
        pr.simHole();
        assertThat(pr.currentHole()).isEqualTo(2); // advanced exactly one hole
        assertThat(pr.holeScores()).hasSize(1);

        pr.simRound();
        assertThat(pr.isComplete()).isTrue();
        assertThat(pr.holeScores()).hasSize(18);
    }
}
