package com.progolf.sim.play;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import org.junit.jupiter.api.Test;

/** playable-event spec: an interactive single playoff hole, faithful to automatic hole resolution. */
class PlayableHoleTest {

    private static final long WORLD = 0xC0FFEEL;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.LINKS);
    }

    @Test
    void aSimmedPlayoffHoleMatchesAutomaticResolution() {
        Course course = course();
        int holeNumber = 1;
        int playoffRound = 91; // matches the automatic sudden-death round id (90 + playoff hole)
        int fieldIndex = 4;
        SeedCoordinate coord = new SeedCoordinate(WORLD, 1, 7, playoffRound, fieldIndex, holeNumber, 0);

        int automatic = RoundResolver.resolveHole(
                course.holeModel(holeNumber, playoffRound), Attributes.uniform(60), GolferState.fresh(),
                Environment.calm(), Strategy.BALANCED, coord).totalStrokes();

        PlayableHole hole = new PlayableHole(holeNumber, course.holes().get(holeNumber - 1).par(),
                Attributes.uniform(60), GolferState.fresh(), course.holeModel(holeNumber, playoffRound),
                Environment.calm(), coord, Strategy.BALANCED);
        hole.simHole();

        assertThat(hole.isComplete()).isTrue();
        assertThat(hole.strokes()).isEqualTo(automatic);
    }

    @Test
    void theSituationExposesTheHoleAndAdvances() {
        Course course = course();
        SeedCoordinate coord = new SeedCoordinate(WORLD, 1, 7, 91, 0, 1, 0);
        PlayableHole hole = new PlayableHole(1, 4, Attributes.uniform(60), GolferState.fresh(),
                course.holeModel(1, 91), Environment.calm(), coord, Strategy.BALANCED);

        ShotSituation situation = hole.situation();
        assertThat(situation.holeNumber()).isEqualTo(1);
        assertThat(situation.shotNumber()).isEqualTo(1);
        assertThat(situation.distanceToPin()).isGreaterThan(0.0);

        hole.simShot();
        if (!hole.isComplete()) {
            assertThat(hole.situation().shotNumber()).isEqualTo(2);
        }
    }
}
