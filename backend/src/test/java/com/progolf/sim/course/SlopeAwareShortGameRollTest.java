package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ClubSpec;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.GroundMotionPoint;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotFamily;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.WindVector;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exact planar and changing fields exercise the shared shot resolver, rather than a visual offset. */
class SlopeAwareShortGameRollTest {
    private static final Position2d BALL = new Position2d(0, -10);
    private static final Position2d AIM = new Position2d(0, 0);
    private static final Position2d CUP = new Position2d(0, 10);
    private static final List<Position2d> PLAYABLE = rectangle(-30, -20, 30, 35);
    private static final List<Position2d> GREEN = rectangle(-20, -2, 20, 25);
    private static final CourseGeometry GEOMETRY = geometry(GREEN, false);
    private static final ShotZoneProfile ZONES = new ShotZoneProfile(List.of(
            new ZoneBand(0, 100, List.of(new LateralRegion(100, Surface.FAIRWAY)))));
    private static final SeedCoordinate SEED = new SeedCoordinate(0x5A0E1234L, 2, 5, 1, 3, 1, 0);

    @Test
    void flatPhysicalGreenKeepsTheExistingChipAndPitchLandingAndRelease() {
        for (ShotFamily family : List.of(ShotFamily.CHIP, ShotFamily.PITCH)) {
            ShotOutcome old = resolve(GEOMETRY, null, family, 10);
            ShotOutcome flat = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0), family, 10);
            assertThat(flat.carry()).isEqualTo(old.carry());
            assertThat(flat.lateral()).isEqualTo(old.lateral());
            assertThat(flat.settlement().contact()).isEqualTo(old.settlement().contact());
            assertThat(flat.trace().backspinRpm()).isEqualTo(old.trace().backspinRpm());
            assertThat(flat.settlement().ball().position().distanceTo(old.settlement().ball().position()))
                    .isLessThan(0.15);
            assertThat(flat.trace().groundPath().getLast().position()).isEqualTo(flat.settlement().ball().position());
        }
    }

    @Test
    void uphillDownhillAndOppositeCrossSlopesChangeTheActualRoll() {
        for (ShotFamily family : List.of(ShotFamily.CHIP, ShotFamily.PITCH)) {
            ShotOutcome flat = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0), family, 10);
            ShotOutcome uphill = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0.035 * y), family, 10);
            ShotOutcome downhill = resolve(GEOMETRY, surface(GREEN, (x, y) -> -0.035 * y), family, 10);
            assertThat(uphill.settlement().ball().position().y()).isLessThan(flat.settlement().ball().position().y());
            assertThat(downhill.settlement().ball().position().y()).isGreaterThan(flat.settlement().ball().position().y());
            assertThat(downhill.settlement().ball().position().y() - flat.settlement().ball().position().y())
                    .isLessThan(2.0);

            ShotOutcome rightDownhill = resolve(GEOMETRY, surface(GREEN, (x, y) -> -0.035 * x), family, 10);
            ShotOutcome leftDownhill = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0.035 * x), family, 10);
            assertThat(rightDownhill.settlement().ball().position().x()).isGreaterThan(flat.settlement().ball().position().x());
            assertThat(leftDownhill.settlement().ball().position().x()).isLessThan(flat.settlement().ball().position().x());
        }
    }

    @Test
    void changingGradientIsResampledAndAuthoredIntoAValidCurvedTrace() {
        GreenSurface changing = surface(GREEN, (x, y) -> 0.0015 * x * y);
        ShotOutcome curved = resolve(GEOMETRY, changing, ShotFamily.CHIP, 10);
        Position2d contact = curved.settlement().contact().position();
        GreenSurfaceGradient initial = changing.gradientAt(contact);
        ShotOutcome fixed = resolve(GEOMETRY, surface(GREEN,
                (x, y) -> initial.x() * x + initial.y() * y), ShotFamily.CHIP, 10);
        assertThat(curved.settlement().ball().position().distanceTo(fixed.settlement().ball().position()))
                .isGreaterThan(0.005);
        assertThat(curved.trace().groundPath()).extracting(GroundMotionPoint::kind)
                .contains(GroundMotionPoint.Kind.ROLL_SAMPLE, GroundMotionPoint.Kind.ROLL_STOP);
        assertThat(curved.trace().roll().from()).isEqualTo(contact);
        assertThat(curved.trace().roll().to()).isEqualTo(curved.settlement().ball().position());
        assertThat(curved.trace().groundPath().getLast().position()).isEqualTo(curved.trace().finalPoint());
        assertThat(ShotResolver.resolveShot(context(GEOMETRY, changing, ShotFamily.CHIP, 10)).settlement())
                .isEqualTo(curved.settlement());
        assertThat(resolve(GEOMETRY, changing, ShotFamily.CHIP, 10).trace()).isEqualTo(curved.trace());
    }

    @Test
    void lowEnergyAndGreenExitSettleWithinAuthoritativePlayableTerrain() {
        ShotOutcome shortChip = ShotResolver.resolveShotWithTrace(contextAt(GEOMETRY,
                surface(GREEN, (x, y) -> -0.035 * y), ShotFamily.CHIP, 2,
                new Position2d(0, -3.5), Surface.FRINGE, CUP));
        assertThat(shortChip.settlement().contact().surface()).isEqualTo(Surface.GREEN);
        assertThat(shortChip.settlement().ball().position().distanceTo(shortChip.settlement().contact().position()))
                .isLessThan(4.0);
        assertThat(shortChip.trace().groundPath()).isNotEmpty();
        assertThat(shortChip.trace().groundPath().size()).isLessThan(20);

        List<Position2d> smallGreen = rectangle(-20, -2, 20, 2);
        CourseGeometry narrow = geometry(smallGreen, true);
        ShotOutcome crossing = ShotResolver.resolveShotWithTrace(contextAt(narrow,
                surface(smallGreen, (x, y) -> -0.025 * y), ShotFamily.CHIP, 10,
                BALL, Surface.FIRST_CUT, new Position2d(0, 0)));
        assertThat(crossing.settlement().contact().surface()).isEqualTo(Surface.GREEN);
        assertThat(crossing.finalSurface()).isEqualTo(narrow.surfaceAt(crossing.settlement().ball().position()));
        assertThat(crossing.settlement().ball().position().y()).isGreaterThan(2.0);
        assertThat(crossing.settlement().ball().position().y()).isLessThan(6.0);
        assertThat(crossing.trace().groundPath().getLast().position()).isEqualTo(crossing.trace().finalPoint());

        double barrier = 2 + (crossing.settlement().ball().position().y() - 2) / 2;
        CourseGeometry thinHazard = new CourseGeometry(BALL, new Position2d(0, 0), PLAYABLE, List.of(
                new TerrainRegion(Surface.FIRST_CUT, PLAYABLE),
                new TerrainRegion(Surface.FRINGE, rectangle(-22, -4, 22, 6)),
                new TerrainRegion(Surface.GREEN, smallGreen),
                new TerrainRegion(Surface.WATER, rectangle(-20, barrier, 20, barrier + 0.02))));
        ShotOutcome stopped = ShotResolver.resolveShotWithTrace(contextAt(thinHazard,
                surface(smallGreen, (x, y) -> -0.025 * y), ShotFamily.CHIP, 10,
                BALL, Surface.FIRST_CUT, new Position2d(0, 0)));
        assertThat(stopped.trace().groundPath().getLast().kind()).isEqualTo(GroundMotionPoint.Kind.BOUNDARY_STOP);
        assertThat(stopped.settlement().ball().position().y()).isLessThan(barrier);
        assertThat(stopped.finalSurface()).isEqualTo(Surface.FRINGE);
    }

    @Test
    void aFringeLandingCanRollIntoTheGreenAndThenBreak() {
        ShotOutcome flat = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0), ShotFamily.CHIP, 6.5);
        ShotOutcome cross = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0.035 * x), ShotFamily.CHIP, 6.5);
        assertThat(flat.settlement().contact().surface()).isEqualTo(Surface.FRINGE);
        assertThat(cross.settlement().contact()).isEqualTo(flat.settlement().contact());
        assertThat(cross.finalSurface()).isEqualTo(Surface.GREEN);
        assertThat(cross.settlement().ball().position().x()).isLessThan(flat.settlement().ball().position().x());
        assertThat(cross.trace().groundPath()).extracting(GroundMotionPoint::kind)
                .contains(GroundMotionPoint.Kind.ROLL_SAMPLE, GroundMotionPoint.Kind.ROLL_STOP);
    }

    @Test
    void legacyAndMismatchedSurfaceRetainTheOriginalSettlement() {
        ShotOutcome legacy = resolve(GEOMETRY, null, ShotFamily.CHIP, 10);
        ShotOutcome invalid = resolve(GEOMETRY, surface(rectangle(-19, -2, 20, 25), (x, y) -> 0.035 * x),
                ShotFamily.CHIP, 10);
        assertThat(invalid.settlement()).isEqualTo(legacy.settlement());
        assertThat(invalid.trace()).isEqualTo(legacy.trace());
        ShotOutcome ordinary = resolve(GEOMETRY, null, ShotFamily.FULL, 10);
        ShotOutcome suppliedSurface = resolve(GEOMETRY, surface(GREEN, (x, y) -> 0.035 * x),
                ShotFamily.FULL, 10);
        assertThat(suppliedSurface).isEqualTo(ordinary);
    }

    private static ShotOutcome resolve(CourseGeometry geometry, GreenSurface surface, ShotFamily family, double carry) {
        return ShotResolver.resolveShotWithTrace(context(geometry, surface, family, carry));
    }

    private static ShotContext context(CourseGeometry geometry, GreenSurface surface, ShotFamily family, double carry) {
        return contextAt(geometry, surface, family, carry, BALL, Surface.FIRST_CUT, CUP);
    }

    private static ShotContext contextAt(CourseGeometry geometry, GreenSurface surface, ShotFamily family, double carry,
                                         Position2d start, Surface lie, Position2d cup) {
        Position2d aim = start.plus(0, carry);
        ClubSpec club = ClubSpec.of(ClubId.GAP_WEDGE);
        ShotDecision decision = new ShotDecision(club.family(), carry, 0, Strategy.BALANCED, club, family);
        return new ShotContext(Attributes.uniform(85), GolferState.fresh(),
                new Environment(new WindVector(0, 0), 1, 0.95), start.distanceTo(cup), ZONES,
                decision, SEED, lie, 0, new BallState(start, lie),
                geometry, cup, aim, Handedness.RIGHT, surface);
    }

    private static CourseGeometry geometry(List<Position2d> green, boolean water) {
        return new CourseGeometry(BALL, new Position2d(0, 5), PLAYABLE, List.of(
                new TerrainRegion(Surface.FIRST_CUT, PLAYABLE),
                new TerrainRegion(Surface.FRINGE, rectangle(-22, -4, 22, water ? 6 : 27)),
                new TerrainRegion(Surface.GREEN, green),
                new TerrainRegion(Surface.WATER, water ? rectangle(-20, 6, 20, 20)
                        : rectangle(25, 25, 29, 30))));
    }

    private interface Elevation { double at(double x, double y); }

    private static GreenSurface surface(List<Position2d> footprint, Elevation elevation) {
        double minX = footprint.stream().mapToDouble(Position2d::x).min().orElseThrow();
        double maxX = footprint.stream().mapToDouble(Position2d::x).max().orElseThrow();
        double minY = footprint.stream().mapToDouble(Position2d::y).min().orElseThrow();
        double maxY = footprint.stream().mapToDouble(Position2d::y).max().orElseThrow();
        int columns = 41, rows = 31;
        double[] heights = new double[columns * rows];
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            double x = minX + (maxX - minX) * column / (columns - 1.0);
            double y = minY + (maxY - minY) * row / (rows - 1.0);
            heights[row * columns + column] = elevation.at(x, y);
        }
        return new GreenSurface(new GreenSurfaceProvenance(GreenSurfaceVersion.V1, 11L, 13L, 17L,
                new Position2d(0, 1)), footprint, minX, maxX, minY, maxY, columns, rows, heights);
    }

    private static List<Position2d> rectangle(double left, double bottom, double right, double top) {
        return List.of(new Position2d(left, bottom), new Position2d(right, bottom),
                new Position2d(right, top), new Position2d(left, top));
    }
}
