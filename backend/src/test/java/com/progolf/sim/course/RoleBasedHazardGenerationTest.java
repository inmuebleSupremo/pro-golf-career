package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotAim;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Fixed V4 corpus: semantic provenance, feasibility, and retained historical version isolation. */
class RoleBasedHazardGenerationTest {
    private static final long WORLD = 0x563448415a415244L;

    @Test
    void v4IsExplicitDeterministicAndLeavesHistoricalVersionsWithoutHazardPlans() {
        SeedCoordinate coordinate = coordinate(7, EnvironmentClassification.COASTAL);
        Course v1 = CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V1_GENERATOR_VERSION);
        Course v2 = CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V2_GENERATOR_VERSION);
        Course v3 = CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V3_GENERATOR_VERSION);
        Course v4 = CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V4_GENERATOR_VERSION);

        assertThat(v1.holes()).allSatisfy(hole -> assertThat(hole.hazardPlan()).isNull());
        assertThat(v2.holes()).allSatisfy(hole -> assertThat(hole.hazardPlan()).isNull());
        assertThat(v3.holes()).allSatisfy(hole -> assertThat(hole.hazardPlan()).isNull());
        assertThat(v4.generatorVersion()).isEqualTo(CourseGenConstants.V4_GENERATOR_VERSION);
        assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V4_GENERATOR_VERSION)).isEqualTo(v4);
        assertThat(v4.holes()).allSatisfy(hole -> assertThat(hole.hazardPlan()).isNotNull());
        assertThatThrownBy(() -> CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL, 99))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported");
    }

    @Test
    void fixedV4CorpusRetainsHazardProvenanceAndSafeCoresAcrossProfiles() {
        long courses = 0;
        long water = 0;
        long recovery = 0;
        long greenGuards = 0;
        long bailoutBoundaries = 0;
        for (StrategicEmphasis strategic : StrategicEmphasis.values()) {
            for (WidthTendency width : WidthTendency.values()) {
                for (RecoverySeverity recoverySeverity : RecoverySeverity.values()) {
                    CourseDesignProfile profile = new CourseDesignProfile(strategic, width, recoverySeverity);
                    for (EnvironmentClassification classification : EnvironmentClassification.values()) {
                        Course course = CourseGenerator.generateV4(coordinate(courses, classification), classification, profile);
                        assertThat(course.holes()).allSatisfy(hole -> {
                            HoleSpatialPlan spatial = hole.spatialPlan();
                            HazardPlan plan = hole.hazardPlan();
                            assertThat(plan.features()).isNotEmpty().hasSizeLessThanOrEqualTo(4);
                            assertThat(plan.features()).allSatisfy(feature -> {
                                assertThat(feature.surface()).isIn(Surface.BUNKER, Surface.WATER, Surface.TREES,
                                        Surface.RECOVERY_AREA);
                                assertThat(feature.role()).isIn(HazardRole.values());
                                assertThat(feature.envelope().longitudinalRadius()).isPositive();
                            });
                            assertThat(plan.features().stream().filter(feature -> feature.role() == HazardRole.GREEN_GUARD))
                                    .allSatisfy(feature -> assertThat(feature.anchor().greenSide())
                                            .isEqualTo(spatial.greenComplex().protectedSide()));
                            for (LandingZone zone : spatial.landingZones()) {
                                if (zone.role() == LandingZoneRole.PRIMARY || zone.role() == LandingZoneRole.SAFE) {
                                    assertThat(hole.geometry().surfaceAt(zone.center(spatial.route())))
                                            .isIn(Surface.FAIRWAY, Surface.FIRST_CUT, Surface.PRIMARY_ROUGH);
                                }
                            }
                            assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
                            assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter())).isEqualTo(Surface.GREEN);
                        });
                        water += course.holes().stream().flatMap(hole -> hole.hazardPlan().features().stream())
                                .filter(feature -> feature.surface() == Surface.WATER).count();
                        recovery += course.holes().stream().flatMap(hole -> hole.hazardPlan().features().stream())
                                .filter(feature -> feature.surface() == Surface.TREES || feature.surface() == Surface.RECOVERY_AREA)
                                .count();
                        greenGuards += course.holes().stream().flatMap(hole -> hole.hazardPlan().features().stream())
                                .filter(feature -> feature.role() == HazardRole.GREEN_GUARD).count();
                        bailoutBoundaries += course.holes().stream().flatMap(hole -> hole.hazardPlan().features().stream())
                                .filter(feature -> feature.role() == HazardRole.BAILOUT_BOUNDARY).count();
                        courses++;
                    }
                }
            }
        }
        assertThat(courses).isEqualTo(162);
        assertThat(water).isPositive();
        assertThat(recovery).isPositive();
        assertThat(greenGuards).isPositive().isLessThan(courses * 18L);
        assertThat(bailoutBoundaries).isPositive();
    }

    @Test
    void riskRewardFeaturesGuardTheAggressiveZoneWhileSafeZoneRemainsClear() {
        GeneratedHole hole = Arrays.stream(EnvironmentClassification.values())
                .flatMap(classification -> CourseGenerator.generate(coordinate(classification.ordinal() + 90, classification),
                        classification, CourseGenConstants.V4_GENERATOR_VERSION).holes().stream())
                .filter(candidate -> candidate.spatialPlan().landingZones().stream()
                        .anyMatch(zone -> zone.role() == LandingZoneRole.AGGRESSIVE)).findFirst().orElseThrow();
        HoleSpatialPlan plan = hole.spatialPlan();
        assertThat(hole.hazardPlan().features()).anyMatch(feature -> feature.role() == HazardRole.LANDING_GUARD
                && feature.anchor().landingZoneRole() == LandingZoneRole.AGGRESSIVE);
        LandingZone safe = plan.landingZones().stream().filter(zone -> zone.role() == LandingZoneRole.SAFE).findFirst().orElseThrow();
        assertThat(hole.geometry().surfaceAt(safe.center(plan.route())))
                .isIn(Surface.FAIRWAY, Surface.FIRST_CUT, Surface.PRIMARY_ROUGH);
    }

    @Test
    void currentProgressionTargetsRemainOutsideV4Hazards() {
        GeneratedHole dogleg = Arrays.stream(EnvironmentClassification.values())
                .flatMap(classification -> CourseGenerator.generate(coordinate(classification.ordinal() + 180, classification),
                        classification, CourseGenConstants.V4_GENERATOR_VERSION).holes().stream())
                .filter(hole -> !hole.spatialPlan().route().intermediateAnchors().isEmpty()).findFirst().orElseThrow();
        HoleModel model = dogleg.forRound(1);
        ShotAim.Reference aim = ShotAim.forBall(model, new BallState(model.geometry().tee(), Surface.TEE_BOX), Strategy.BALANCED);
        assertThat(aim.progressionTarget()).isTrue();
        assertThat(model.geometry().surfaceAt(aim.target()))
                .isNotIn(Surface.BUNKER, Surface.WATER, Surface.TREES, Surface.RECOVERY_AREA);
    }

    @Test
    void turnGuardsAndWaterReliefOccurOnlyInFeasibleV4Layouts() {
        long turns = 0;
        long waters = 0;
        for (EnvironmentClassification classification : EnvironmentClassification.values()) {
            for (int id = 0; id < 80; id++) {
                Course course = CourseGenerator.generate(coordinate(300 + id, classification), classification,
                        CourseGenConstants.V4_GENERATOR_VERSION);
                for (GeneratedHole hole : course.holes()) {
                    turns += hole.hazardPlan().features().stream().filter(feature -> feature.role() == HazardRole.TURN_GUARD).count();
                    for (TerrainRegion region : hole.geometry().regions()) {
                        if (region.surface() == Surface.WATER) {
                            waters++;
                            assertThat(hasTeeWardPrimaryRoughRelief(region, hole.geometry())).isTrue();
                        }
                    }
                }
            }
        }
        assertThat(turns).isPositive();
        assertThat(waters).isPositive();
    }

    @Test
    void v4SetupGeometryRemainsCanonicalAndPlayable() {
        Course course = CourseGenerator.generate(coordinate(501, EnvironmentClassification.WOODLAND),
                EnvironmentClassification.WOODLAND, CourseGenConstants.V4_GENERATOR_VERSION);
        CourseSetup tight = new CourseSetup(0.72, 1.0, 1.4);
        for (GeneratedHole hole : course.holes()) {
            HoleModel model = hole.forRound(1, tight);
            assertThat(model.geometry().surfaceAt(model.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
            assertThat(model.geometry().surfaceAt(model.geometry().greenCenter())).isEqualTo(Surface.GREEN);
        }
    }

    private static boolean hasTeeWardPrimaryRoughRelief(TerrainRegion water, CourseGeometry geometry) {
        double x = water.boundary().stream().mapToDouble(Position2d::x).average().orElseThrow();
        double y = water.boundary().stream().mapToDouble(Position2d::y).average().orElseThrow();
        Position2d contact = new Position2d(x, y);
        Position2d tee = geometry.tee();
        double dx = tee.x() - contact.x();
        double dy = tee.y() - contact.y();
        double distance = StrictMath.hypot(dx, dy);
        for (double setback = 15.0; setback <= distance; setback += 1.0) {
            if (geometry.surfaceAt(contact.plus(dx / distance * setback, dy / distance * setback)) == Surface.PRIMARY_ROUGH) {
                return true;
            }
        }
        return false;
    }

    private static SeedCoordinate coordinate(long id, EnvironmentClassification classification) {
        return new SeedCoordinate(WORLD, 4, id, classification.ordinal(), 0, 0, 0);
    }
}
