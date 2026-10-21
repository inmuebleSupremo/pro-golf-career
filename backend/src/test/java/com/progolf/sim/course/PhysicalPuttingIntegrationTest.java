package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.putt.PhysicalPuttPlanner;
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
import com.progolf.sim.world.ScheduledTournament;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tour.TourTier;
import org.junit.jupiter.api.Test;

/** Exercises physical putting through policy-bearing course models and the established shot/round result path. */
class PhysicalPuttingIntegrationTest {
    private static final Attributes PUTTER = Attributes.uniform(80)
            .with(Attribute.PUTTING_ACCURACY, 100).with(Attribute.PUTTING_PROXIMITY, 100);
    private static final SeedCoordinate COORDINATE = new SeedCoordinate(0x505859534943414cL, 2, 43, 1, 4, 7, 0);

    @Test
    void explicitPhysicalPolicyUsesTheIntegratedResolverWhileLegacyKeepsItsOriginalPath() {
        Course course = course();
        HoleModel legacy = course.holeModel(7, 1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
        HoleModel physical = course.holeModel(7, 1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN,
                PuttingPolicy.PHYSICAL_V1);

        assertThat(legacy.puttingPolicy()).isEqualTo(PuttingPolicy.LEGACY);
        assertThat(legacy.greenSurface()).isNull();
        assertThat(physical.puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1);
        assertThat(physical.greenSurface().version()).isEqualTo(GreenSurfaceVersion.V1);
        assertThat(physical.greenSurface().provenance().holeSeed()).isEqualTo(course.holes().get(6).holeSeed());

        RoundOutcome legacyRound = RoundResolver.resolveHole(legacy, PUTTER, GolferState.fresh(), Environment.calm(),
                Strategy.BALANCED, COORDINATE);
        RoundOutcome physicalRound = RoundResolver.resolveHole(physical, PUTTER, GolferState.fresh(), Environment.calm(),
                Strategy.BALANCED, COORDINATE);
        assertThat(legacyRound.shots()).noneMatch(shot -> shot.physicalPutt() != null);
        assertThat(physicalRound.shots()).anyMatch(shot -> shot.physicalPutt() != null);
        assertThat(physicalRound.shots()).filteredOn(shot -> shot.physicalPutt() != null).allSatisfy(shot -> {
            assertThat(shot.holed()).isEqualTo(shot.physicalPutt().holed());
            if (shot.settlement() != null) {
                assertThat(shot.settlement().ball().position()).isEqualTo(shot.physicalPutt().finalPosition());
            }
        });
    }

    @Test
    void submittedPhysicalIntentPreservesTheActualNearMissAndDoesNotUseLegacyHoleThreshold() {
        HoleModel hole = physicalHole();
        GreenSurface surface = hole.greenSurface();
        Position2d cup = hole.cupPosition();
        Position2d start = pointTowardCentre(surface, cup, 5.0 / 3.0);
        Position2d line = toward(start, cup);
        Position2d perpendicular = new Position2d(-line.y(), line.x());
        // Deliberately pass six inches from the centre: within the legacy 0.35-yard threshold, outside cup capture.
        Position2d missDirection = normalised(line.x() + perpendicular.x() * 0.10 / start.distanceTo(cup),
                line.y() + perpendicular.y() * 0.10 / start.distanceTo(cup));
        ShotOutcome miss = PhysicalPuttResolution.resolve(hole, PUTTER, GolferState.fresh(), COORDINATE.withShot(41),
                new BallState(start, Surface.GREEN), new PhysicalPuttIntent(missDirection, start.distanceTo(cup) * 3.0));

        assertThat(miss.physicalPutt()).isNotNull();
        assertThat(miss.holed()).isFalse();
        assertThat(miss.distanceRemaining()).isLessThan(0.35);
        assertThat(miss.strokes()).isEqualTo(1);
        assertThat(miss.settlement()).isNotNull();
        assertThat(miss.settlement().ball().position()).isEqualTo(miss.physicalPutt().finalPosition());
    }

    @Test
    void centralSubmittedCaptureCompletesThroughTheSameAuthoritativeShotOutcome() {
        HoleModel hole = physicalHole();
        GreenSurface surface = hole.greenSurface();
        Position2d cup = hole.cupPosition();
        Position2d start = pointTowardCentre(surface, cup, 5.0 / 3.0);
        ShotOutcome captured = PhysicalPuttResolution.resolve(hole, PUTTER, GolferState.fresh(), COORDINATE.withShot(52),
                new BallState(start, Surface.GREEN), new PhysicalPuttIntent(toward(start, cup), start.distanceTo(cup) * 3.0));

        assertThat(captured.physicalPutt()).isNotNull();
        assertThat(captured.holed()).isTrue();
        assertThat(captured.holed()).isEqualTo(captured.physicalPutt().holed());
        assertThat(captured.strokes()).isEqualTo(1);
        if (captured.holed()) assertThat(captured.settlement().ball().position()).isEqualTo(cup);
    }

    @Test
    void humanAndAiShareTheSameResolverAndPhysicalAiRoundsAreDeterministic() {
        HoleModel hole = physicalHole();
        GreenSurface surface = hole.greenSurface();
        Position2d start = pointTowardCentre(surface, hole.cupPosition(), 3.0);
        PhysicalPuttIntent aiIntent = PhysicalPuttPlanner.decide(surface, start, hole.cupPosition());
        ShotOutcome human = PhysicalPuttResolution.resolve(hole, PUTTER, GolferState.fresh(), COORDINATE.withShot(61),
                new BallState(start, Surface.GREEN), aiIntent);
        ShotOutcome ai = PhysicalPuttResolution.resolve(hole, PUTTER, GolferState.fresh(), COORDINATE.withShot(61),
                new BallState(start, Surface.GREEN), aiIntent);
        assertThat(ai).isEqualTo(human);

        RoundOutcome first = RoundResolver.resolveHole(hole, PUTTER, GolferState.fresh(), Environment.calm(),
                Strategy.BALANCED, COORDINATE.withShot(70));
        RoundOutcome repeat = RoundResolver.resolveHole(hole, PUTTER, GolferState.fresh(), Environment.calm(),
                Strategy.BALANCED, COORDINATE.withShot(70));
        assertThat(repeat).isEqualTo(first);
        assertThat(first.shots()).anyMatch(shot -> shot.physicalPutt() != null);
    }

    @Test
    void policyIsExplicitAndHistoricalPolicyDataDefaultsToLegacy() {
        ScheduledTournament historical = new ScheduledTournament(1, TourTier.PRO, 0, EventPrestige.REGULAR, 9L,
                PinPlacementVersion.V5_EFFECTIVE_GREEN);
        ScheduledTournament physical = ScheduledTournament.physicalV1(1, TourTier.PRO, 0, EventPrestige.REGULAR, 9L,
                PinPlacementVersion.V5_EFFECTIVE_GREEN);
        assertThat(historical.puttingPolicy()).isEqualTo(PuttingPolicy.LEGACY);
        assertThat(physical.puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1);
        assertThatThrownBy(() -> PhysicalPuttResolution.resolve(course().holeModel(7, 1), PUTTER, GolferState.fresh(),
                COORDINATE, new BallState(new Position2d(0, 0), Surface.GREEN), new PhysicalPuttIntent(new Position2d(1, 0), 5)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static HoleModel physicalHole() {
        return course().holeModel(7, 1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.PHYSICAL_V1);
    }

    private static Course course() {
        return CourseGenerator.generate(COORDINATE, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
    }

    private static Position2d pointTowardCentre(GreenSurface surface, Position2d cup, double distance) {
        Position2d centre = centre(surface.footprint());
        Position2d direction = toward(cup, centre);
        for (double candidateDistance = distance; candidateDistance >= 0.25; candidateDistance -= 0.25) {
            Position2d candidate = cup.plus(direction.x() * candidateDistance, direction.y() * candidateDistance);
            if (surface.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("could not find a valid physical-putt start");
    }

    private static Position2d centre(java.util.List<Position2d> points) {
        double x = 0.0, y = 0.0;
        for (Position2d point : points) { x += point.x(); y += point.y(); }
        return new Position2d(x / points.size(), y / points.size());
    }

    private static Position2d toward(Position2d from, Position2d to) {
        return normalised(to.x() - from.x(), to.y() - from.y());
    }

    private static Position2d normalised(double x, double y) {
        double length = StrictMath.hypot(x, y);
        return new Position2d(x / length, y / length);
    }
}
