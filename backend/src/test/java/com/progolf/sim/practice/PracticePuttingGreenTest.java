package com.progolf.sim.practice;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.shot.GolferState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticePuttingGreenTest {
    @Test
    void fiveCupsAndStartingBallShareTheAuthoritativeSurface() {
        var green = new PracticePuttingGreen(Attributes.uniform(70), GolferState.fresh(), 42);
        assertThat(PracticePuttingGreen.cups()).hasSize(5).allSatisfy(cup ->
                assertThat(PracticePuttingGreen.surfaceView().contains(cup.position())).isTrue());
        assertThat(PracticePuttingGreen.surfaceView().contains(green.state().readyPosition())).isTrue();
        assertThat(PracticePuttingGreen.surfaceView().maximumSlope()).isGreaterThan(0);
    }

    @Test
    void placementRepeatAndCollectRetainTheChosenStartWithoutMovingRestingBalls() {
        var green = new PracticePuttingGreen(Attributes.uniform(70), GolferState.fresh(), 42);
        Position2d chosen = new Position2d(-5, -3);
        var placed = green.placeBall(chosen, "0");
        assertThat(placed.startingPosition()).isEqualTo(chosen);
        assertThat(placed.readyPosition()).isEqualTo(chosen);
        assertThatThrownBy(() -> green.placeBall(new Position2d(30, 30), "1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> green.putt(3, new PhysicalPuttIntent(new Position2d(1, 0), 6), "0"))
                .isInstanceOf(IllegalArgumentException.class);

        var first = green.putt(3, new PhysicalPuttIntent(new Position2d(1, 0), 6), "1");
        assertThat(first.outcome().trajectory()).hasSizeGreaterThan(1);
        assertThat(first.outcome().trajectory().getFirst().position()).isEqualTo(chosen);
        assertThat(first.state().restingBalls()).hasSize(1);
        assertThat(first.state().restingBalls().getFirst().position()).isEqualTo(first.outcome().finalPosition());
        assertThat(first.state().startingPosition()).isEqualTo(chosen);
        var second = green.putt(1, new PhysicalPuttIntent(new Position2d(0, 1), 4), "2");
        assertThat(second.ballNumber()).isEqualTo(2);
        assertThat(second.state().restingBalls()).hasSize(2);
        assertThat(second.state().restingBalls().getFirst()).isEqualTo(first.state().restingBalls().getFirst());

        var collected = green.collectBalls("3");
        assertThat(collected.restingBalls()).isEmpty();
        assertThat(collected.startingPosition()).isEqualTo(chosen);
        assertThat(collected.readyPosition()).isEqualTo(chosen);
        assertThat(collected.puttsHit()).isEqualTo(2);
    }

    @Test
    void aCapturedBallLeavesTheSurfaceAndTheNextBallIsReady() {
        var green = new PracticePuttingGreen(Attributes.uniform(100), GolferState.fresh(), 42);
        Position2d cup = PracticePuttingGreen.cups().get(2).position();
        Position2d start = cup.plus(-0.5, 0);
        green.placeBall(start, "0");
        var result = green.putt(3, new PhysicalPuttIntent(new Position2d(1, 0), 1.8), "1");
        assertThat(result.outcome().holed()).isTrue();
        assertThat(result.holedCup()).isEqualTo(3);
        assertThat(result.state().restingBalls()).isEmpty();
        assertThat(result.state().readyPosition()).isEqualTo(start);
    }
}
