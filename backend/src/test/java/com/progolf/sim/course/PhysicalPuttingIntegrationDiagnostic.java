package com.progolf.sim.course;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.putt.PhysicalPuttResolution;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import java.util.Locale;

/** Reproducible console evidence for the event-policy integration; it does not call PhysicalPuttSolver directly. */
public final class PhysicalPuttingIntegrationDiagnostic {
    private PhysicalPuttingIntegrationDiagnostic() {
    }

    public static void main(String[] args) {
        SeedCoordinate coordinate = new SeedCoordinate(0x505859534943414cL, 2, 43, 1, 4, 7, 0);
        Course course = CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        HoleModel physical = course.holeModel(7, 1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN,
                PuttingPolicy.PHYSICAL_V1);
        HoleModel legacy = course.holeModel(7, 1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
        GreenSurface surface = physical.greenSurface();
        Position2d cup = physical.cupPosition();
        Position2d centre = centre(surface);
        Position2d startDirection = direction(cup, centre);
        Position2d start = cup.plus(startDirection.x() * (5.0 / 3.0), startDirection.y() * (5.0 / 3.0));
        PhysicalPuttIntent intent = new PhysicalPuttIntent(direction(start, cup), start.distanceTo(cup) * 3.0);
        Attributes attributes = Attributes.uniform(80).with(Attribute.PUTTING_ACCURACY, 100)
                .with(Attribute.PUTTING_PROXIMITY, 100);
        ShotOutcome outcome = PhysicalPuttResolution.resolve(physical, attributes, GolferState.fresh(), coordinate.withShot(52),
                new BallState(start, Surface.GREEN), intent);
        RoundOutcome legacyRound = RoundResolver.resolveHole(legacy, attributes, GolferState.fresh(), Environment.calm(),
                Strategy.BALANCED, coordinate);

        System.out.printf(Locale.ROOT, "policy=%s surface=%s physicalModel=%s%n", physical.puttingPolicy(), surface.version(),
                physical.puttingPolicy().physicalModelVersion());
        System.out.printf(Locale.ROOT, "intent direction=(%.6f,%.6f) paceFeet=%.3f%n", intent.direction().x(), intent.direction().y(),
                intent.intendedRollFeet());
        System.out.printf(Locale.ROOT, "physical final=(%.6f,%.6f) holed=%s strokes=%d terminal=%s trajectoryPoints=%d%n",
                outcome.physicalPutt().finalPosition().x(), outcome.physicalPutt().finalPosition().y(), outcome.holed(),
                outcome.strokes(), outcome.physicalPutt().terminalCondition(), outcome.physicalPutt().trajectory().size());
        System.out.printf(Locale.ROOT, "legacy policy=%s physicalTrajectoryPresent=%s shots=%d%n", legacy.puttingPolicy(),
                legacyRound.shots().stream().anyMatch(shot -> shot.physicalPutt() != null), legacyRound.shots().size());
    }

    private static Position2d centre(GreenSurface surface) {
        double x = 0.0, y = 0.0;
        for (Position2d point : surface.footprint()) { x += point.x(); y += point.y(); }
        return new Position2d(x / surface.footprint().size(), y / surface.footprint().size());
    }

    private static Position2d direction(Position2d from, Position2d to) {
        double x = to.x() - from.x();
        double y = to.y() - from.y();
        double length = StrictMath.hypot(x, y);
        return new Position2d(x / length, y / length);
    }
}
