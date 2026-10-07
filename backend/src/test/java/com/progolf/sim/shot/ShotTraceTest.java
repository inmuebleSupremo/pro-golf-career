package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Trace materialization is a pure projection of canonical resolution and settlement facts. */
class ShotTraceTest {

    private static final Position2d TEE = new Position2d(0, 0);
    private static final Position2d CUP = new Position2d(0, 200);
    private static final Position2d AIM = new Position2d(15, 180);

    @Test
    void normalTracePreservesOriginAimContactAndFinalBall() {
        ShotOutcome outcome = traced(geometry(square(-100, -10, 100, 240), List.of()), Surface.TEE_BOX);

        assertThat(outcome.trace()).isNotNull();
        assertThat(outcome.trace().clubId()).isEqualTo(ClubId.GAP_WEDGE);
        assertThat(outcome.trace().origin()).isEqualTo(TEE);
        assertThat(outcome.trace().intendedAimPoint()).isEqualTo(new AimPoint(AIM.x(), AIM.y()));
        assertThat(outcome.trace().contact()).isEqualTo(outcome.settlement().contact());
        assertThat(outcome.trace().transition()).isNull();
        assertThat(outcome.trace().finalPoint()).isEqualTo(outcome.settlement().ball().position());
        assertThat(outcome.trace().finalPoint()).isEqualTo(outcome.trace().contact().position());
    }

    @Test
    void traceKeepsTheCanonicalContactAcrossOrdinaryLandingSurfaces() {
        assertThat(traced(geometry(square(-100, -10, 100, 240),
                List.of(new TerrainRegion(Surface.FAIRWAY, square(-100, -10, 100, 240)))), Surface.TEE_BOX)
                .trace().contact().surface()).isEqualTo(Surface.FAIRWAY);

        assertThat(traced(geometry(square(-100, -10, 100, 240),
                List.of(new TerrainRegion(Surface.PRIMARY_ROUGH, square(-100, -10, 100, 240)))), Surface.TEE_BOX)
                .trace().contact().surface()).isEqualTo(Surface.PRIMARY_ROUGH);

        assertThat(traced(geometry(square(-100, -10, 100, 240),
                List.of(new TerrainRegion(Surface.BUNKER, square(-100, -10, 100, 240)))), Surface.TEE_BOX)
                .trace().contact().surface()).isEqualTo(Surface.BUNKER);
    }

    @Test
    void waterDropAndReplayTracesKeepContactSeparateFromLegalBall() {
        ShotOutcome dropped = traced(geometry(square(-100, -10, 100, 240),
                List.of(new TerrainRegion(Surface.WATER, square(-100, 20, 100, 190)))), Surface.TEE_BOX);
        assertThat(dropped.trace().contact().surface()).isEqualTo(Surface.WATER);
        assertThat(dropped.trace().transition().kind()).isEqualTo(RecoveryKind.WATER_DROP);
        assertThat(dropped.trace().transition().from()).isEqualTo(dropped.trace().contact().position());
        assertThat(dropped.trace().transition().to()).isEqualTo(dropped.settlement().recoveryPosition());
        assertThat(dropped.trace().finalPoint()).isEqualTo(dropped.settlement().ball().position());

        ShotOutcome fallback = traced(geometry(square(-100, -10, 100, 240),
                List.of(new TerrainRegion(Surface.WATER, square(-100, -10, 100, 240)))), Surface.TEE_BOX);
        assertThat(fallback.trace().transition().kind()).isEqualTo(RecoveryKind.STROKE_AND_DISTANCE_FALLBACK);
        assertThat(fallback.trace().finalPoint()).isEqualTo(TEE);

        ShotOutcome outOfBounds = traced(geometry(square(-100, -10, 100, 50), List.of()), Surface.TEE_BOX);
        assertThat(outOfBounds.trace().contact().surface()).isEqualTo(Surface.OUT_OF_BOUNDS);
        assertThat(outOfBounds.trace().transition().kind()).isEqualTo(RecoveryKind.OUT_OF_BOUNDS_REPLAY);
        assertThat(outOfBounds.trace().finalPoint()).isEqualTo(TEE);
    }

    @Test
    void traceMaterializationDoesNotChangeResolutionOrPutting() {
        CourseGeometry geometry = geometry(square(-100, -10, 100, 240), List.of());
        ShotContext fullShot = context(geometry, Surface.TEE_BOX, new BallState(TEE, Surface.TEE_BOX), AIM);
        ShotOutcome summary = ShotResolver.resolveShot(fullShot);
        ShotOutcome traced = ShotResolver.resolveShotWithTrace(fullShot);

        assertThat(summary.trace()).isNull();
        assertThat(traced.trace()).isNotNull();
        assertThat(traced.carry()).isEqualTo(summary.carry());
        assertThat(traced.lateral()).isEqualTo(summary.lateral());
        assertThat(traced.finalSurface()).isEqualTo(summary.finalSurface());
        assertThat(traced.penaltyStrokes()).isEqualTo(summary.penaltyStrokes());
        assertThat(traced.strokes()).isEqualTo(summary.strokes());
        assertThat(traced.settlement()).isEqualTo(summary.settlement());

        // A ball already at the cup exercises the canonical hole-out settlement path without
        // depending on a probabilistic putt make roll.
        BallState puttingBall = new BallState(CUP, Surface.GREEN);
        ShotContext putt = context(geometry, Surface.GREEN, puttingBall, CUP);
        ShotOutcome summaryPutt = ShotResolver.resolveShot(putt);
        ShotOutcome tracedPutt = ShotResolver.resolveShotWithTrace(putt);
        assertThat(tracedPutt.putt()).isTrue();
        assertThat(tracedPutt.carry()).isEqualTo(summaryPutt.carry());
        assertThat(tracedPutt.settlement()).isEqualTo(summaryPutt.settlement());
        assertThat(tracedPutt.distanceRemaining()).isZero();
        assertThat(tracedPutt.trace().contact().position()).isEqualTo(CUP);
        assertThat(tracedPutt.trace().finalPoint()).isEqualTo(tracedPutt.settlement().ball().position());
    }

    private static ShotOutcome traced(CourseGeometry geometry, Surface lie) {
        return ShotResolver.resolveShotWithTrace(context(geometry, lie, new BallState(TEE, lie), AIM));
    }

    private static ShotContext context(CourseGeometry geometry, Surface lie, BallState ball, Position2d aim) {
        ShotZoneProfile profile = new ShotZoneProfile(List.of(new ZoneBand(0, 500,
                List.of(new LateralRegion(500, Surface.FAIRWAY)))));
        return new ShotContext(Attributes.uniform(60), GolferState.fresh(), Environment.calm(),
                ball.position().distanceTo(CUP), profile, ShotDecision.straight(Club.WEDGE, 120, Strategy.BALANCED),
                new SeedCoordinate(7L, 1, 1, 1, 1, 1, 0), lie, 0, ball, geometry, CUP, aim);
    }

    private static CourseGeometry geometry(List<Position2d> boundary, List<TerrainRegion> regions) {
        return new CourseGeometry(TEE, CUP, boundary, regions);
    }

    private static List<Position2d> square(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
    }
}
