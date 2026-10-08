package com.progolf.sim.shot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.core.Handedness;
import org.junit.jupiter.api.Test;

class FlightSolutionTest {
    @Test
    void handednessShapesBulgeOnTheSpecifiedSideAcrossArbitraryAimAxesAndReturnToAimPoint() {
        for (Position2d aim : java.util.List.of(new Position2d(80, 120), new Position2d(-95, 70))) {
            ShotFrame frame = ShotFrame.toward(new Position2d(0, 0), aim);
            double carry = aim.distanceTo(new Position2d(0, 0));
            assertShape(frame, aim, carry, Handedness.RIGHT, ShotShape.DRAW, 1.0);
            assertShape(frame, aim, carry, Handedness.RIGHT, ShotShape.FADE, -1.0);
            assertShape(frame, aim, carry, Handedness.LEFT, ShotShape.DRAW, -1.0);
            assertShape(frame, aim, carry, Handedness.LEFT, ShotShape.FADE, 1.0);

            FlightSolution straight = new FlightSolution(frame, carry, 0, FlightSolution.shapeCurveSign(Handedness.RIGHT, ShotShape.STRAIGHT), 12);
            assertEquals(0.0, frame.lateralTo(straight.positionAt(.5)));
            assertEquals(aim, straight.firstContact());
        }
    }

    private static void assertShape(ShotFrame frame, Position2d aim, double carry, Handedness handedness,
                                    ShotShape shape, double expectedSide) {
        FlightSolution solution = new FlightSolution(frame, carry, 0,
                FlightSolution.shapeCurveSign(handedness, shape) * 8, 12);
        assertTrue(frame.lateralTo(solution.positionAt(.5)) * expectedSide > 0,
                () -> handedness + " " + shape + " must bulge on the specified aim-relative side");
        assertEquals(aim, solution.firstContact());
        var samples = solution.samples();
        assertEquals(0.0, samples.getFirst().progress());
        assertEquals(0.0, samples.getFirst().height());
        assertEquals(1.0, samples.getLast().progress());
        assertEquals(0.0, samples.getLast().height());
        assertEquals(solution.firstContact(), samples.getLast().position());
        for (int index = 1; index < samples.size(); index++) {
            assertTrue(Double.isFinite(samples.get(index).height()));
            assertTrue(samples.get(index).progress() > samples.get(index - 1).progress());
        }
    }
}
