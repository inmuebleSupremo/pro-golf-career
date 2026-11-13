package com.progolf.sim.putt;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.core.SeasonProfile;
import com.progolf.sim.course.GreenSurfaceGradient;
import com.progolf.sim.course.GreenSurfaceLocation;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.shot.GolferState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fixed-step V1 rolling solver. It is intentionally separate from the legacy probability resolver until
 * an event explicitly opts into this model. Coordinates, positions, and velocities use yards/seconds.
 */
public final class PhysicalPuttSolver {
    /** Fixed 10 ms semi-implicit Euler step keeps V1 replay deterministic and inexpensive. */
    public static final double TIME_STEP_SECONDS = 0.01;
    public static final double ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED = 1.25;
    /** The fringe is modeled as level, longer grass until the authoritative green boundary is crossed. */
    public static final double FRINGE_ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED = 2.50;
    public static final double GRAVITY_YARDS_PER_SECOND_SQUARED = 10.725;
    public static final double STOP_SPEED_YARDS_PER_SECOND = 0.025;
    public static final double MAXIMUM_DURATION_SECONDS = 30.0;
    /** 4.25-inch cup diameter and 1.68-inch ball diameter, reduced to a centre-path capture radius. */
    public static final double CUP_CAPTURE_RADIUS_YARDS = (4.25 - 1.68) / 72.0;
    /** Simplified deterministic capture limit: fast centre-path putts pass across the cup. */
    public static final double MAXIMUM_CAPTURE_SPEED_YARDS_PER_SECOND = 1.20;

    private PhysicalPuttSolver() {
    }

    public static PhysicalPuttOutcome resolve(PhysicalPuttRequest request) {
        return resolve(request, List.of(request.cup()));
    }

    /** The same rolling path can be captured by any supplied cup; tournament play supplies its single cup. */
    public static PhysicalPuttOutcome resolve(PhysicalPuttRequest request, List<Position2d> cups) {
        SeasonProfile profile = SeasonProfile.current();
        long started = profile == null ? 0L : System.nanoTime();
        try {
        Objects.requireNonNull(request, "request");
        cups = List.copyOf(Objects.requireNonNull(cups, "cups"));
        if (cups.isEmpty() || cups.stream().anyMatch(cup -> !request.surface().contains(cup))) {
            throw new IllegalArgumentException("physical putt cups must be on the supplied green surface");
        }
        if (request.modelVersion() != PhysicalPuttModelVersion.V1) {
            throw new IllegalArgumentException("unsupported physical putting model: " + request.modelVersion());
        }
        PhysicalPuttExecution execution = execute(request);
        List<PhysicalPuttTrajectoryPoint> trajectory = new ArrayList<>();
        Position2d position = request.start();
        boolean fringeStart = request.fringeGeometry() != null && !request.surface().contains(position);
        double velocityX = execution.direction().x() * execution.initialSpeedYardsPerSecond();
        double velocityY = execution.direction().y() * execution.initialSpeedYardsPerSecond();
        trajectory.add(new PhysicalPuttTrajectoryPoint(0.0, position, execution.initialSpeedYardsPerSecond()));

        int maximumSteps = (int) StrictMath.ceil(MAXIMUM_DURATION_SECONDS / TIME_STEP_SECONDS);
        for (int step = 1; step <= maximumSteps; step++) {
            double speed = StrictMath.hypot(velocityX, velocityY);
            Surface terrain = fringeStart ? request.fringeGeometry().surfaceAt(position) : Surface.GREEN;
            double rollingDeceleration = terrain == Surface.FRINGE
                    ? FRINGE_ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED
                    : ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED;
            GreenSurfaceGradient gradient = terrain == Surface.FRINGE
                    ? new GreenSurfaceGradient(0.0, 0.0) : request.surface().gradientAt(position);
            double slopeAccelerationX = -GRAVITY_YARDS_PER_SECOND_SQUARED * gradient.x();
            double slopeAccelerationY = -GRAVITY_YARDS_PER_SECOND_SQUARED * gradient.y();
            double slopeAcceleration = StrictMath.hypot(slopeAccelerationX, slopeAccelerationY);
            if (speed < STOP_SPEED_YARDS_PER_SECOND && slopeAcceleration <= rollingDeceleration) {
                return stopped(request, execution, trajectory, position, step - 1);
            }

            double accelerationX = slopeAccelerationX;
            double accelerationY = slopeAccelerationY;
            if (speed >= STOP_SPEED_YARDS_PER_SECOND) {
                accelerationX -= rollingDeceleration * velocityX / speed;
                accelerationY -= rollingDeceleration * velocityY / speed;
            }
            double nextVelocityX = velocityX + accelerationX * TIME_STEP_SECONDS;
            double nextVelocityY = velocityY + accelerationY * TIME_STEP_SECONDS;
            if (speed >= STOP_SPEED_YARDS_PER_SECOND
                    && nextVelocityX * velocityX + nextVelocityY * velocityY <= 0.0
                    && slopeAcceleration <= rollingDeceleration) {
                return stopped(request, execution, trajectory, position, step - 1);
            }
            Position2d next = position.plus(nextVelocityX * TIME_STEP_SECONDS, nextVelocityY * TIME_STEP_SECONDS);
            double nextSpeed = StrictMath.hypot(nextVelocityX, nextVelocityY);
            double time = step * TIME_STEP_SECONDS;
            if (nextSpeed <= MAXIMUM_CAPTURE_SPEED_YARDS_PER_SECOND) {
                for (Position2d cup : cups) {
                    if (segmentDistance(position, next, cup) <= CUP_CAPTURE_RADIUS_YARDS) {
                        trajectory.add(new PhysicalPuttTrajectoryPoint(time, cup, nextSpeed));
                        return new PhysicalPuttOutcome(request.modelVersion(), execution, trajectory, cup, true,
                                PhysicalPuttTerminalCondition.CAPTURED);
                    }
                }
            }
            Surface nextTerrain = fringeStart ? request.fringeGeometry().surfaceAt(next) : Surface.GREEN;
            if (fringeStart ? nextTerrain != Surface.GREEN && nextTerrain != Surface.FRINGE
                    : request.surface().sampleAt(next).location() == GreenSurfaceLocation.OUTSIDE) {
                trajectory.add(new PhysicalPuttTrajectoryPoint(time, next, nextSpeed));
                return new PhysicalPuttOutcome(request.modelVersion(), execution, trajectory, next, false,
                        PhysicalPuttTerminalCondition.LEFT_SUPPORTED_SURFACE);
            }
            trajectory.add(new PhysicalPuttTrajectoryPoint(time, next, nextSpeed));
            position = next;
            velocityX = nextVelocityX;
            velocityY = nextVelocityY;
        }
        return new PhysicalPuttOutcome(request.modelVersion(), execution, trajectory, position, false,
                PhysicalPuttTerminalCondition.MAXIMUM_DURATION);
        } finally {
            if (profile != null) profile.recordCall(SeasonProfile.Metric.PHYSICAL_PUTT_SOLVER, System.nanoTime() - started);
        }
    }

    private static PhysicalPuttOutcome stopped(PhysicalPuttRequest request, PhysicalPuttExecution execution,
                                                List<PhysicalPuttTrajectoryPoint> trajectory, Position2d position,
                                                int completedSteps) {
        double time = completedSteps * TIME_STEP_SECONDS;
        PhysicalPuttTrajectoryPoint last = trajectory.get(trajectory.size() - 1);
        if (last.timeSeconds() != time || last.speedYardsPerSecond() != 0.0) {
            trajectory.add(new PhysicalPuttTrajectoryPoint(time, position, 0.0));
        }
        return new PhysicalPuttOutcome(request.modelVersion(), execution, trajectory, position, false,
                PhysicalPuttTerminalCondition.STOPPED);
    }

    private static PhysicalPuttExecution execute(PhysicalPuttRequest request) {
        double directionErrorRadians = 0.0;
        double paceMultiplier = 1.0;
        if (request.executionMode() == PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN) {
            Rng rng = new SplitMix64Rng(request.executionSeed());
            GolferState state = request.state();
            double conditions = conditionMultiplier(state);
            double accuracy = request.attributes().norm(Attribute.PUTTING_ACCURACY);
            double proximity = request.attributes().norm(Attribute.PUTTING_PROXIMITY);
            double directionSigmaRadians = StrictMath.toRadians(0.30 + (1.0 - accuracy) * 2.10) * conditions;
            double paceSigma = (0.008 + (1.0 - proximity) * 0.080) * conditions;
            directionErrorRadians = rng.nextGaussian() * directionSigmaRadians;
            paceMultiplier = Math.max(0.15, 1.0 + rng.nextGaussian() * paceSigma);
        }
        Position2d intended = request.intent().direction();
        double cosine = StrictMath.cos(directionErrorRadians);
        double sine = StrictMath.sin(directionErrorRadians);
        Position2d direction = new Position2d(intended.x() * cosine - intended.y() * sine,
                intended.x() * sine + intended.y() * cosine);
        double initialSpeed = StrictMath.sqrt(2.0 * ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED
                * request.intent().intendedRollYards() * paceMultiplier);
        return new PhysicalPuttExecution(direction, initialSpeed, StrictMath.toDegrees(directionErrorRadians), paceMultiplier);
    }

    private static double conditionMultiplier(GolferState state) {
        double effectiveFatigue = state.fatigue() * (1.0 - state.mentalSupport());
        double effectivePressure = state.pressure() * (1.0 - state.mentalSupport());
        return 1.0 + effectiveFatigue * 0.45 + effectivePressure * 0.35 + state.injuryImpairment() * 0.60;
    }

    private static double segmentDistance(Position2d start, Position2d end, Position2d point) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double lengthSquared = dx * dx + dy * dy;
        double fraction = lengthSquared < 1.0e-15 ? 0.0
                : Math.clamp(((point.x() - start.x()) * dx + (point.y() - start.y()) * dy) / lengthSquared, 0.0, 1.0);
        return StrictMath.hypot(point.x() - (start.x() + dx * fraction), point.y() - (start.y() + dy * fraction));
    }
}
