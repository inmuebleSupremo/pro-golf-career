package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.Position2d;
import org.junit.jupiter.api.Test;

/** Coordinate invariants for the canonical projection of the legacy carry/lateral sampler. */
class ShotFrameTest {

    private static final Position2d GREEN_CENTRE = new Position2d(0, 400);
    private static final Position2d TUCKED_CUP = new Position2d(8, 405);

    @Test
    void centredShotUsesGreenCentreReferenceWithoutDoubleCountingPinLateral() {
        ShotFrame frame = ShotFrame.towardGreenCentreReference(new Position2d(0, 0), GREEN_CENTRE, TUCKED_CUP);

        assertThat(frame.project(405, 0)).isEqualTo(new Position2d(0, 405));
        assertThat(frame.project(405, 8)).isEqualTo(TUCKED_CUP);
    }

    @Test
    void equalAndOppositeLateralErrorsRemainSymmetricAroundLocalForwardAxis() {
        ShotFrame frame = ShotFrame.towardGreenCentreReference(new Position2d(20, 100), GREEN_CENTRE, TUCKED_CUP);
        Position2d centre = frame.project(120, 0);
        Position2d right = frame.project(120, 15);
        Position2d left = frame.project(120, -15);

        assertThat(right.x() + left.x()).isCloseTo(2 * centre.x(), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(right.y() + left.y()).isCloseTo(2 * centre.y(), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(right.distanceTo(left)).isCloseTo(30, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void offCentreBallStartsItsNextFrameAtItsActualPersistentPosition() {
        Position2d ball = new Position2d(-30, 220);
        ShotFrame frame = ShotFrame.towardGreenCentreReference(ball, GREEN_CENTRE, TUCKED_CUP);

        assertThat(frame.project(0, 0)).isEqualTo(ball);
        assertThat(frame.project(200, 0).distanceTo(new Position2d(0, 405)))
                .isLessThan(ball.distanceTo(new Position2d(0, 405)));
        Position2d projectedCup = frame.project(frame.forwardTo(TUCKED_CUP), frame.lateralTo(TUCKED_CUP));
        assertThat(projectedCup.x()).isCloseTo(TUCKED_CUP.x(), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(projectedCup.y()).isCloseTo(TUCKED_CUP.y(), org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void overGreenBallFallsBackToACupFacingFrame() {
        Position2d ball = new Position2d(4, 405);
        ShotFrame frame = ShotFrame.forFullShot(ball, GREEN_CENTRE, TUCKED_CUP);

        assertThat(frame.forwardTo(TUCKED_CUP)).isGreaterThan(0);
        assertThat(frame.lateralTo(TUCKED_CUP)).isCloseTo(0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void directCupFrameKeepsPuttingDistanceAnchoredToThePhysicalCup() {
        Position2d ball = new Position2d(-2, 401);
        ShotFrame frame = ShotFrame.toward(ball, TUCKED_CUP);

        assertThat(frame.project(ball.distanceTo(TUCKED_CUP), 0)).isEqualTo(TUCKED_CUP);
    }

    @Test
    void doglegApproachUsesCurrentBallToGreenReferenceRatherThanGlobalCourseAxis() {
        Position2d ball = new Position2d(36, 255);
        Position2d green = new Position2d(0, 430);
        Position2d cup = new Position2d(-6, 434);
        ShotFrame frame = ShotFrame.towardGreenCentreReference(ball, green, cup);

        Position2d contact = frame.project(100, 0);
        assertThat(contact.x()).isLessThan(ball.x());
        assertThat(contact.y()).isGreaterThan(ball.y());
    }
}
