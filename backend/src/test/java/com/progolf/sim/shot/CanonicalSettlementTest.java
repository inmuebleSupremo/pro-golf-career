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

/** Canonical settlement values retain contact while exposing the legal next-shot ball state. */
class CanonicalSettlementTest {

    private static final Position2d TEE = new Position2d(0, 0);
    private static final Position2d CUP = new Position2d(0, 200);

    @Test
    void normalContactSettlesAtTheContactPosition() {
        ShotOutcome outcome = resolve(geometry(square(-100, -10, 100, 240), List.of()));

        assertThat(outcome.settlement().recoveryKind()).isEqualTo(RecoveryKind.NONE);
        assertThat(outcome.settlement().recoveryPosition()).isNull();
        assertThat(outcome.settlement().ball().position()).isEqualTo(outcome.settlement().contact().position());
        assertThat(outcome.settlement().ball().lie()).isEqualTo(outcome.settlement().contact().surface());
    }

    @Test
    void waterContactKeepsTheContactButProvidesATeeWardRoughDrop() {
        CourseGeometry geometry = geometry(square(-100, -10, 100, 240),
                List.of(new TerrainRegion(Surface.WATER, square(-100, 20, 100, 190))));

        ShotOutcome outcome = resolve(geometry);

        assertThat(outcome.finalSurface()).isEqualTo(Surface.PRIMARY_ROUGH);
        assertThat(outcome.settlement().recoveryKind()).isEqualTo(RecoveryKind.WATER_DROP);
        assertThat(outcome.settlement().contact().surface()).isEqualTo(Surface.WATER);
        assertThat(outcome.settlement().ball().lie()).isEqualTo(Surface.PRIMARY_ROUGH);
        assertThat(outcome.settlement().ball().position().distanceTo(CUP))
                .isLessThanOrEqualTo(TEE.distanceTo(CUP));
    }

    @Test
    void outOfBoundsReplaysTheExactPreShotBallState() {
        ShotOutcome outcome = resolve(geometry(square(-100, -10, 100, 50), List.of()));

        assertThat(outcome.finalSurface()).isEqualTo(Surface.TEE_BOX);
        assertThat(outcome.settlement().recoveryKind()).isEqualTo(RecoveryKind.OUT_OF_BOUNDS_REPLAY);
        assertThat(outcome.settlement().recoveryPosition()).isEqualTo(TEE);
        assertThat(outcome.settlement().ball()).isEqualTo(new BallState(TEE, Surface.TEE_BOX));
    }

    private static ShotOutcome resolve(CourseGeometry geometry) {
        ShotZoneProfile profile = new ShotZoneProfile(List.of(new ZoneBand(0, 500,
                List.of(new LateralRegion(500, Surface.FAIRWAY)))));
        return ShotResolver.resolveShot(new ShotContext(Attributes.uniform(60), GolferState.fresh(), Environment.calm(),
                200, profile, ShotDecision.straight(Club.WEDGE, 120, Strategy.BALANCED),
                new SeedCoordinate(7L, 1, 1, 1, 1, 1, 0), Surface.TEE_BOX, 0,
                new BallState(TEE, Surface.TEE_BOX), geometry, CUP));
    }

    private static CourseGeometry geometry(List<Position2d> boundary, List<TerrainRegion> regions) {
        return new CourseGeometry(TEE, CUP, boundary, regions);
    }

    private static List<Position2d> square(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
    }
}
