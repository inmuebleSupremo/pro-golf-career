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

/** Focused acceptance coverage for deterministic, firmness-sensitive post-contact release. */
class GroundResponseTest {
    private static final Position2d TEE = new Position2d(0, 0);
    private static final Position2d CUP = new Position2d(0, 400);
    private static final SeedCoordinate COORDINATE = new SeedCoordinate(0x600D5EEDL, 1, 1, 1, 1, 1, 1);
    private static final List<Position2d> OPEN = square(-1_000, -10, 1_000, 700);
    private static final ShotZoneProfile ZONES = new ShotZoneProfile(List.of(
            new ZoneBand(0, 800, List.of(new LateralRegion(800, Surface.FAIRWAY)))));
    private static final CourseGeometry OPEN_FAIRWAY = new CourseGeometry(TEE, CUP, OPEN,
            List.of(new TerrainRegion(Surface.FAIRWAY, OPEN)));

    @Test
    void firmDriverReleasesMateriallyFartherThanIdenticalSoftDriver() {
        ShotOutcome soft = resolve(OPEN_FAIRWAY, ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL, 260, 0.10);
        ShotOutcome firm = resolve(OPEN_FAIRWAY, ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL, 260, 0.95);

        assertThat(firm.settlement().contact()).isEqualTo(soft.settlement().contact());
        assertThat(firm.trace().roll()).isNotNull();
        assertThat(soft.trace().roll()).isNotNull();
        double firmRelease = rollDistance(firm);
        double softRelease = rollDistance(soft);
        assertThat(firmRelease).isGreaterThan(softRelease + 7.0);
        assertThat(firm.finalSurface()).isEqualTo(Surface.FAIRWAY);
        assertThat(firm.settlement().ball()).isEqualTo(resolve(OPEN_FAIRWAY, ClubSpec.of(ClubId.DRIVER),
                ShotFamily.FULL, 260, 0.95).settlement().ball());
    }

    @Test
    void clubLoftAndShortGameFamiliesRemainDistinctUnderFirmness() {
        Environment firm = environment(0.95);
        Environment soft = environment(0.10);
        Attributes attributes = Attributes.uniform(65);
        ShotExecutionProfile driver = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.DRIVER),
                ShotFamily.FULL, attributes, firm);
        ShotExecutionProfile sixIron = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.SIX_IRON),
                ShotFamily.FULL, attributes, firm);
        ShotExecutionProfile gapWedge = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE),
                ShotFamily.FULL, attributes, firm);
        ShotExecutionProfile pitchFirm = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE),
                ShotFamily.PITCH, attributes, firm);
        ShotExecutionProfile pitchSoft = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE),
                ShotFamily.PITCH, attributes, soft);
        ShotExecutionProfile chipFirm = ShotExecutionProfile.derive(Surface.FAIRWAY, ClubSpec.of(ClubId.GAP_WEDGE),
                ShotFamily.CHIP, attributes, firm);

        assertThat(driver.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(sixIron.rollYardsOn(Surface.FAIRWAY));
        assertThat(sixIron.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(gapWedge.rollYardsOn(Surface.FAIRWAY));
        assertThat(sixIron.rollYardsOn(Surface.GREEN)).isGreaterThan(gapWedge.rollYardsOn(Surface.GREEN));
        assertThat(pitchFirm.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(pitchSoft.rollYardsOn(Surface.FAIRWAY));
        assertThat(pitchFirm.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(pitchFirm.rollYardsOn(Surface.GREEN));
        assertThat(chipFirm.rollYardsOn(Surface.FAIRWAY)).isGreaterThan(pitchFirm.rollYardsOn(Surface.FAIRWAY));
        assertThat(chipFirm.rollYardsOn(Surface.GREEN)).isGreaterThan(pitchFirm.rollYardsOn(Surface.GREEN));
    }

    @Test
    void releaseMayCrossOneOrdinaryBoundaryButClampsBeforeWater() {
        ShotOutcome open = resolve(OPEN_FAIRWAY, ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL, 260, 0.95);
        Position2d contact = open.settlement().contact().position();
        double firstBoundary = contact.y() + 4.0;
        CourseGeometry oneOrdinaryBoundary = geometryWithFairwayUntil(firstBoundary, null);
        ShotOutcome ordinary = resolve(oneOrdinaryBoundary, ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL, 260, 0.95);

        assertThat(ordinary.settlement().contact().surface()).isEqualTo(Surface.FAIRWAY);
        assertThat(ordinary.finalSurface()).isEqualTo(Surface.PRIMARY_ROUGH);
        assertThat(ordinary.settlement().ball().position().y()).isGreaterThan(firstBoundary);

        double waterBoundary = firstBoundary + 2.0;
        ShotOutcome clamped = resolve(geometryWithFairwayUntil(firstBoundary, waterBoundary), ClubSpec.of(ClubId.DRIVER),
                ShotFamily.FULL, 260, 0.95);
        assertThat(clamped.finalSurface()).isEqualTo(Surface.PRIMARY_ROUGH);
        assertThat(clamped.hazardEntered()).isFalse();
        assertThat(clamped.settlement().ball().position().y()).isLessThan(waterBoundary);
        assertThat(clamped.trace().transition()).isNull();
    }

    @Test
    void releaseClampsBeforeASecondOrdinarySurfaceTransition() {
        ShotOutcome open = resolve(OPEN_FAIRWAY, ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL, 260, 0.95);
        double fairwayEnd = open.settlement().contact().position().y() + 2.0;
        double roughEnd = fairwayEnd + 2.0;

        ShotOutcome clamped = resolve(geometryWithTwoOrdinaryBoundaries(fairwayEnd, roughEnd),
                ClubSpec.of(ClubId.DRIVER), ShotFamily.FULL, 260, 0.95);

        assertThat(clamped.finalSurface()).isEqualTo(Surface.PRIMARY_ROUGH);
        assertThat(clamped.settlement().ball().position().y()).isGreaterThan(fairwayEnd);
        assertThat(clamped.settlement().ball().position().y()).isLessThan(roughEnd);
        assertThat(clamped.hazardEntered()).isFalse();
    }

    @Test
    void traceAndSummaryUseIdenticalFirmGroundSettlement() {
        ShotContext context = context(OPEN_FAIRWAY, ClubSpec.of(ClubId.FOUR_IRON), ShotFamily.FULL, 190, 0.80);
        ShotOutcome summary = ShotResolver.resolveShot(context);
        ShotOutcome traced = ShotResolver.resolveShotWithTrace(context);

        assertThat(summary.trace()).isNull();
        assertThat(traced.trace()).isNotNull();
        assertThat(traced.settlement()).isEqualTo(summary.settlement());
        assertThat(traced.finalSurface()).isEqualTo(summary.finalSurface());
        assertThat(traced.strokes()).isEqualTo(summary.strokes());
    }

    private static ShotOutcome resolve(CourseGeometry geometry, ClubSpec club, ShotFamily family,
                                       double aimY, double firmness) {
        return ShotResolver.resolveShotWithTrace(context(geometry, club, family, aimY, firmness));
    }

    private static ShotContext context(CourseGeometry geometry, ClubSpec club, ShotFamily family,
                                       double aimY, double firmness) {
        Position2d aim = new Position2d(0, aimY);
        ShotDecision decision = new ShotDecision(club.family(), TEE.distanceTo(aim), 0, Strategy.BALANCED, club, family);
        return new ShotContext(Attributes.uniform(65), GolferState.fresh(), environment(firmness), TEE.distanceTo(CUP),
                ZONES, decision, COORDINATE, Surface.FAIRWAY, 0, new BallState(TEE, Surface.TEE_BOX),
                geometry, CUP, aim);
    }

    private static Environment environment(double firmness) {
        return new Environment(new WindVector(0, 0), 1.0, firmness);
    }

    private static CourseGeometry geometryWithFairwayUntil(double fairwayEnd, Double waterStart) {
        List<TerrainRegion> regions = new java.util.ArrayList<>();
        regions.add(new TerrainRegion(Surface.FAIRWAY, square(-1_000, -10, 1_000, fairwayEnd)));
        if (waterStart != null) regions.add(new TerrainRegion(Surface.WATER, square(-1_000, waterStart, 1_000, 700)));
        return new CourseGeometry(TEE, CUP, OPEN, regions);
    }

    private static CourseGeometry geometryWithTwoOrdinaryBoundaries(double fairwayEnd, double roughEnd) {
        return new CourseGeometry(TEE, CUP, OPEN, List.of(
                new TerrainRegion(Surface.FAIRWAY, square(-1_000, -10, 1_000, fairwayEnd)),
                new TerrainRegion(Surface.PRIMARY_ROUGH, square(-1_000, fairwayEnd, 1_000, roughEnd)),
                new TerrainRegion(Surface.FRINGE, square(-1_000, roughEnd, 1_000, 700))));
    }

    private static double rollDistance(ShotOutcome outcome) {
        return outcome.trace().roll().to().distanceTo(outcome.trace().roll().from());
    }

    private static List<Position2d> square(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
    }
}
