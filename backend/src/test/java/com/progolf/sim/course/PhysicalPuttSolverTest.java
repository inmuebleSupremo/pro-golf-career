package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.putt.PhysicalPuttExecutionMode;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.putt.PhysicalPuttModelVersion;
import com.progolf.sim.putt.PhysicalPuttOutcome;
import com.progolf.sim.putt.PhysicalPuttRequest;
import com.progolf.sim.putt.PhysicalPuttSolver;
import com.progolf.sim.putt.PhysicalPuttTerminalCondition;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.SimConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Controlled height fields validate solver behaviour without changing generated-green policies. */
class PhysicalPuttSolverTest {
    private static final Position2d START = new Position2d(0.0, 0.0);
    private static final Position2d REMOTE_CUP = new Position2d(40.0, 20.0);

    @Test
    void idealFlatPuttsCalibrateToTheirLevelGroundFeetAndStopNaturally() {
        GreenSurface flat = plane(0.0, 0.0);
        for (double feet : new double[] {5.0, 15.0, 60.0}) {
            PhysicalPuttOutcome outcome = resolve(flat, START, REMOTE_CUP, new Position2d(1, 0), feet,
                    Attributes.uniform(50), 1L, PhysicalPuttExecutionMode.IDEAL);
            assertThat(outcome.terminalCondition()).isEqualTo(PhysicalPuttTerminalCondition.STOPPED);
            assertThat(outcome.finalPosition().distanceTo(START) * 3.0).isCloseTo(feet, offset(0.12));
            assertThat(outcome.trajectory().get(outcome.trajectory().size() - 1).speedYardsPerSecond()).isZero();
        }
    }

    @Test
    void slopesChangeTravelAndCrossSlopeBreakReversesWithItsGradient() {
        PhysicalPuttOutcome flat = resolve(plane(0, 0), START, REMOTE_CUP, new Position2d(0, 1), 15,
                Attributes.uniform(50), 2L, PhysicalPuttExecutionMode.IDEAL);
        PhysicalPuttOutcome uphill = resolve(plane(0, 0.02), START, REMOTE_CUP, new Position2d(0, 1), 15,
                Attributes.uniform(50), 2L, PhysicalPuttExecutionMode.IDEAL);
        PhysicalPuttOutcome downhill = resolve(plane(0, 0.02), new Position2d(0, 10), REMOTE_CUP, new Position2d(0, -1), 15,
                Attributes.uniform(50), 2L, PhysicalPuttExecutionMode.IDEAL);
        assertThat(uphill.finalPosition().distanceTo(START)).isLessThan(flat.finalPosition().distanceTo(START));
        assertThat(downhill.finalPosition().distanceTo(new Position2d(0, 10))).isGreaterThan(flat.finalPosition().distanceTo(START));

        PhysicalPuttOutcome breaksLeft = resolve(plane(0, 0.015), START, REMOTE_CUP, new Position2d(1, 0), 20,
                Attributes.uniform(50), 3L, PhysicalPuttExecutionMode.IDEAL);
        PhysicalPuttOutcome breaksRight = resolve(plane(0, -0.015), START, REMOTE_CUP, new Position2d(1, 0), 20,
                Attributes.uniform(50), 3L, PhysicalPuttExecutionMode.IDEAL);
        assertThat(breaksLeft.finalPosition().y()).isLessThan(-0.01);
        assertThat(breaksRight.finalPosition().y()).isGreaterThan(0.01);
    }

    @Test
    void paceChangesBreakAndPlanarInterpolationProducesNoTrajectoryJumps() {
        GreenSurface slope = plane(0, 0.012);
        PhysicalPuttOutcome slow = resolve(slope, START, REMOTE_CUP, new Position2d(1, 0), 12,
                Attributes.uniform(50), 4L, PhysicalPuttExecutionMode.IDEAL);
        PhysicalPuttOutcome fast = resolve(slope, START, REMOTE_CUP, new Position2d(1, 0), 30,
                Attributes.uniform(50), 4L, PhysicalPuttExecutionMode.IDEAL);
        // On this fixed cross-grade the longer, faster putt crosses more slope and breaks further downhill.
        assertThat(Math.abs(fast.finalPosition().y())).isGreaterThan(Math.abs(slow.finalPosition().y()));
        for (int index = 1; index < fast.trajectory().size(); index++) {
            assertThat(fast.trajectory().get(index).position().distanceTo(fast.trajectory().get(index - 1).position()))
                    .isLessThan(0.09);
        }
    }

    @Test
    void changingGradientsRemainStableAcrossHeightFieldCellBoundaries() {
        PhysicalPuttOutcome outcome = resolve(curvedSurface(), new Position2d(-8, 0), REMOTE_CUP, new Position2d(1, 0), 36,
                Attributes.uniform(50), 41L, PhysicalPuttExecutionMode.IDEAL);
        assertThat(outcome.terminalCondition()).isEqualTo(PhysicalPuttTerminalCondition.STOPPED);
        assertThat(outcome.finalPosition().y()).isLessThan(-0.05);
        for (int index = 1; index < outcome.trajectory().size(); index++) {
            assertThat(outcome.trajectory().get(index).position().distanceTo(outcome.trajectory().get(index - 1).position()))
                    .isLessThan(0.10);
            assertThat(Math.abs(outcome.trajectory().get(index).speedYardsPerSecond()
                    - outcome.trajectory().get(index - 1).speedYardsPerSecond())).isLessThan(0.035);
        }
    }

    @Test
    void physicalCupCaptureIsGeometricAndNeverUsesTheLegacyThreshold() {
        GreenSurface flat = plane(0, 0);
        Position2d cup = new Position2d(0, 0);
        PhysicalPuttOutcome captured = resolve(flat, new Position2d(-5.0 / 3.0, 0), cup, new Position2d(1, 0), 5,
                Attributes.uniform(50), 5L, PhysicalPuttExecutionMode.IDEAL);
        PhysicalPuttOutcome fast = resolve(flat, new Position2d(-5.0 / 3.0, 0), cup, new Position2d(1, 0), 20,
                Attributes.uniform(50), 5L, PhysicalPuttExecutionMode.IDEAL);
        PhysicalPuttOutcome nearMiss = resolve(flat, new Position2d(-5.0 / 3.0, 0.10), cup, new Position2d(1, 0), 5,
                Attributes.uniform(50), 5L, PhysicalPuttExecutionMode.IDEAL);
        assertThat(captured.holed()).isTrue();
        assertThat(captured.terminalCondition()).isEqualTo(PhysicalPuttTerminalCondition.CAPTURED);
        assertThat(captured.trajectory().get(captured.trajectory().size() - 1).position()).isEqualTo(cup);
        assertThat(fast.holed()).isFalse();
        assertThat(nearMiss.holed()).isFalse();
        assertThat(nearMiss.finalPosition().distanceTo(cup)).isLessThan(SimConstants.HOLED_THRESHOLD);
        // The visible opening is 4.25 inches. This is the smaller *ball-centre* clearance inside that opening,
        // not a separate target: a 1.68-inch ball's centre must remain 1.285 inches from the cup centre.
        assertThat(PhysicalPuttSolver.CUP_CAPTURE_RADIUS_YARDS).isEqualTo((4.25 - 1.68) / 72.0);
        assertThat(PhysicalPuttSolver.CUP_CAPTURE_RADIUS_YARDS).isLessThan(SimConstants.HOLED_THRESHOLD);
        assertThat(SimConstants.HOLED_THRESHOLD).isEqualTo(0.35);
    }

    @Test
    void seededExecutionIsDeterministicSkillSensitiveAndSourceIndependent() {
        GreenSurface flat = plane(0, 0);
        Attributes weak = Attributes.uniform(50).with(Attribute.PUTTING_ACCURACY, 20).with(Attribute.PUTTING_PROXIMITY, 20);
        Attributes strong = Attributes.uniform(50).with(Attribute.PUTTING_ACCURACY, 95).with(Attribute.PUTTING_PROXIMITY, 95);
        PhysicalPuttOutcome repeatOne = resolve(flat, START, REMOTE_CUP, new Position2d(1, 0), 20, weak, 99L,
                PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN);
        PhysicalPuttOutcome repeatTwo = resolve(flat, START, REMOTE_CUP, new Position2d(1, 0), 20, weak, 99L,
                PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN);
        PhysicalPuttOutcome strongExecution = resolve(flat, START, REMOTE_CUP, new Position2d(1, 0), 20, strong, 99L,
                PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN);
        assertThat(repeatTwo).isEqualTo(repeatOne);
        assertThat(Math.abs(strongExecution.execution().directionErrorDegrees()))
                .isLessThan(Math.abs(repeatOne.execution().directionErrorDegrees()));
        assertThat(Math.abs(strongExecution.execution().paceMultiplier() - 1.0))
                .isLessThan(Math.abs(repeatOne.execution().paceMultiplier() - 1.0));

        // There is no human/AI branch or origin field: the same stable physical request resolves identically.
        PhysicalPuttOutcome human = resolve(flat, START, REMOTE_CUP, new Position2d(1, 0), 20, strong, 711L,
                PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN);
        PhysicalPuttOutcome ai = resolve(flat, START, REMOTE_CUP, new Position2d(1, 0), 20, strong, 711L,
                PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN);
        assertThat(ai).isEqualTo(human);
        assertThat(human.trajectory().size()).isLessThan(3_005);
    }

    @Test
    void shortPuttSkillMatrixIsDeterministicAndShowsTheExistingAttributeGradient() {
        Attributes weak = puttingAttributes(35);
        Attributes medium = puttingAttributes(65);
        Attributes strong = puttingAttributes(90);
        int weakMakes = 0;
        int mediumMakes = 0;
        int strongMakes = 0;
        StringBuilder evidence = new StringBuilder("short-putt capture matrix (60 fixed seeds per cell):");

        // Level and a modest 0.4% cross-slope are controlled, representative GreenSurface conditions. No solver
        // inputs, capture criteria, or player attributes are changed by this diagnostic.
        for (double crossSlope : new double[] {0.0, 0.004}) {
            for (double feet : new double[] {1, 2, 3, 5, 10}) {
                int[] makes = new int[3];
                Position2d start = new Position2d(-feet / 3.0, 0);
                for (long seed = 1; seed <= 60; seed++) {
                    if (resolve(plane(0, crossSlope), start, new Position2d(0, 0), new Position2d(1, 0), feet,
                            weak, seed, PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN).holed()) makes[0]++;
                    if (resolve(plane(0, crossSlope), start, new Position2d(0, 0), new Position2d(1, 0), feet,
                            medium, seed, PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN).holed()) makes[1]++;
                    if (resolve(plane(0, crossSlope), start, new Position2d(0, 0), new Position2d(1, 0), feet,
                            strong, seed, PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN).holed()) makes[2]++;
                }
                weakMakes += makes[0];
                mediumMakes += makes[1];
                strongMakes += makes[2];
                evidence.append(String.format("%n  slope=%+.1f%% distance=%.0fft: weak=%d medium=%d strong=%d",
                        crossSlope * 100, feet, makes[0], makes[1], makes[2]));
            }
        }
        System.out.println(evidence);
        assertThat(mediumMakes).isGreaterThanOrEqualTo(weakMakes);
        assertThat(strongMakes).isGreaterThanOrEqualTo(mediumMakes);
    }

    @Test
    void aBallCanLeaveTheSupportedGreenAndCollarWithoutAFabricatedRestingPosition() {
        PhysicalPuttOutcome outcome = resolve(plane(0, 0), new Position2d(49, 0), new Position2d(0, 0),
                new Position2d(1, 0), 30, Attributes.uniform(50), 6L, PhysicalPuttExecutionMode.IDEAL);
        assertThat(outcome.terminalCondition()).isEqualTo(PhysicalPuttTerminalCondition.LEFT_SUPPORTED_SURFACE);
        assertThat(outcome.holed()).isFalse();
    }

    private static PhysicalPuttOutcome resolve(GreenSurface surface, Position2d start, Position2d cup, Position2d direction,
                                                double feet, Attributes attributes, long seed, PhysicalPuttExecutionMode mode) {
        return PhysicalPuttSolver.resolve(new PhysicalPuttRequest(PhysicalPuttModelVersion.V1, surface, start, cup,
                new PhysicalPuttIntent(direction, feet), attributes, GolferState.fresh(), seed, mode));
    }

    private static Attributes puttingAttributes(int rating) {
        return Attributes.uniform(50).with(Attribute.PUTTING_ACCURACY, rating).with(Attribute.PUTTING_PROXIMITY, rating);
    }

    private static GreenSurface plane(double gradientX, double gradientY) {
        int columns = 81;
        int rows = 49;
        double minX = -50;
        double maxX = 50;
        double minY = -30;
        double maxY = 30;
        double[] elevations = new double[columns * rows];
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            double x = minX + (maxX - minX) * column / (columns - 1.0);
            double y = minY + (maxY - minY) * row / (rows - 1.0);
            elevations[row * columns + column] = gradientX * x + gradientY * y;
        }
        List<Position2d> footprint = List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
        return new GreenSurface(new GreenSurfaceProvenance(GreenSurfaceVersion.V1, 8L, 9L, 10L,
                new Position2d(0, 1)), footprint, minX, maxX, minY, maxY, columns, rows, elevations);
    }

    private static GreenSurface curvedSurface() {
        int columns = 81;
        int rows = 49;
        double minX = -50;
        double maxX = 50;
        double minY = -30;
        double maxY = 30;
        double[] elevations = new double[columns * rows];
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            double x = minX + (maxX - minX) * column / (columns - 1.0);
            double y = minY + (maxY - minY) * row / (rows - 1.0);
            elevations[row * columns + column] = 0.0002 * x * x + 0.01 * y;
        }
        List<Position2d> footprint = List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
        return new GreenSurface(new GreenSurfaceProvenance(GreenSurfaceVersion.V1, 12L, 13L, 14L,
                new Position2d(0, 1)), footprint, minX, maxX, minY, maxY, columns, rows, elevations);
    }
}
