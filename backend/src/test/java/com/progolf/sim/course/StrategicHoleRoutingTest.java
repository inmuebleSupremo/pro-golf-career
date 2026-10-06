package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.ShotAim;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixed V3 geometry/progression corpus; V1/V2 exact fixtures remain in {@link CourseDesignTest}. */
class StrategicHoleRoutingTest {
    private static final long WORLD = 0x563352434f525055L;

    @Test
    void v3IsExplicitDeterministicAndRetainsSpatialPlansWithoutChangingOlderVersions() {
        SeedCoordinate coordinate = coordinate(7, EnvironmentClassification.LINKS);
        Course v1 = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V1_GENERATOR_VERSION);
        Course v2 = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V2_GENERATOR_VERSION);
        Course v3 = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V3_GENERATOR_VERSION);

        assertThat(v1.holes()).allSatisfy(hole -> assertThat(hole.spatialPlan()).isNull());
        assertThat(v2.holes()).allSatisfy(hole -> assertThat(hole.spatialPlan()).isNull());
        assertThat(v3.generatorVersion()).isEqualTo(CourseGenConstants.V3_GENERATOR_VERSION);
        assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V3_GENERATOR_VERSION)).isEqualTo(v3);
        assertThat(v3.holes()).allSatisfy(hole -> {
            assertThat(hole.spatialPlan()).isNotNull();
            assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
            assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter())).isEqualTo(Surface.GREEN);
        });
    }

    @Test
    void routeModelRejectsBackwardDisplacedAndOverComplexCandidatesBeforeCompilation() {
        assertThatThrownBy(() -> new HoleRoute(new Position2d(0, 0), List.of(new Position2d(12, 80)),
                new Position2d(10, 45))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("progress");
        assertThatThrownBy(() -> new HoleRoute(new Position2d(0, 0), List.of(new Position2d(80, 100)),
                new Position2d(0, 260))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displacement");
        assertThatThrownBy(() -> new HoleRoute(new Position2d(0, 0),
                List.of(new Position2d(10, 80), new Position2d(0, 160)), new Position2d(0, 250)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at most one");
    }

    @Test
    void fixedV3CorpusCoversStraightLeftAndRightRoutesWithReachableFairwayZones() {
        long straight = 0;
        long left = 0;
        long right = 0;
        long zones = 0;
        for (EnvironmentClassification classification : EnvironmentClassification.values()) {
            for (int id = 0; id < 8; id++) {
                Course course = CourseGenerator.generate(coordinate(classification.ordinal() * 8L + id, classification),
                        classification, CourseGenConstants.V3_GENERATOR_VERSION);
                for (GeneratedHole hole : course.holes()) {
                    HoleSpatialPlan plan = hole.spatialPlan();
                    if (plan.route().intermediateAnchors().isEmpty()) straight++;
                    else if (plan.route().intermediateAnchors().getFirst().x() < 0.0) left++;
                    else right++;
                    for (LandingZone zone : plan.landingZones()) {
                        assertThat(zone.routeDistance()).isBetween(1.0, plan.route().length() - 10.0);
                        assertThat(hole.geometry().surfaceAt(zone.center(plan.route())))
                                .as("zone %s on hole %s", zone.role(), hole.number())
                                .isIn(Surface.FAIRWAY, Surface.FIRST_CUT, Surface.PRIMARY_ROUGH);
                        zones++;
                    }
                }
            }
        }
        assertThat(straight).isGreaterThan(0);
        assertThat(left).isGreaterThan(0);
        assertThat(right).isGreaterThan(0);
        assertThat(zones).isGreaterThan(48L * 18L);
    }

    @Test
    void allProfileCombinationsPassTheFixedV3GeometryAndFeasibilityCorpus() {
        long courses = 0;
        for (StrategicEmphasis strategic : StrategicEmphasis.values()) {
            for (WidthTendency width : WidthTendency.values()) {
                for (RecoverySeverity recovery : RecoverySeverity.values()) {
                    CourseDesignProfile profile = new CourseDesignProfile(strategic, width, recovery);
                    for (EnvironmentClassification classification : EnvironmentClassification.values()) {
                        for (int id = 0; id < 8; id++) {
                            Course course = CourseGenerator.generateV3(coordinate(classification.ordinal() * 8L + id,
                                    classification), classification, profile);
                            assertThat(course.designProfile()).isEqualTo(profile);
                            assertThat(course.holes()).allSatisfy(hole -> {
                                HoleSpatialPlan plan = hole.spatialPlan();
                                assertThat(plan.route().length()).isGreaterThan(80.0);
                                assertThat(plan.landingZones()).anyMatch(zone -> zone.role() == LandingZoneRole.PRIMARY);
                                assertThat(plan.route().finalUnit().x() * plan.greenComplex().approachDirection().x()
                                        + plan.route().finalUnit().y() * plan.greenComplex().approachDirection().y())
                                        .isGreaterThan(0.90);
                                assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
                                assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter())).isEqualTo(Surface.GREEN);
                            });
                            courses++;
                        }
                    }
                }
            }
        }
        assertThat(courses).isEqualTo(1_296);
    }

    @Test
    void riskRewardZonesAreSpatiallySeparatedAndAggressiveProgressesFurther() {
        Course course = CourseGenerator.generate(coordinate(19, EnvironmentClassification.PARKLAND),
                EnvironmentClassification.PARKLAND, CourseGenConstants.V3_GENERATOR_VERSION);
        HoleBrief brief = course.coursePlan().briefs().stream()
                .filter(candidate -> candidate.archetype() == StrategicArchetype.RISK_REWARD && candidate.par() >= 4)
                .findFirst().orElseThrow();
        HoleSpatialPlan plan = course.holes().get(brief.number() - 1).spatialPlan();
        LandingZone safe = plan.landingZones().stream().filter(zone -> zone.role() == LandingZoneRole.SAFE)
                .findFirst().orElseThrow();
        LandingZone aggressive = plan.landingZones().stream().filter(zone -> zone.role() == LandingZoneRole.AGGRESSIVE)
                .findFirst().orElseThrow();
        assertThat(aggressive.routeDistance()).isGreaterThan(safe.routeDistance());
        assertThat(aggressive.center(plan.route()).distanceTo(plan.greenCenter()))
                .isLessThan(safe.center(plan.route()).distanceTo(plan.greenCenter()));
    }

    @Test
    void doglegProgressionUsesSemanticTargetAndLegacyHolesKeepGreenCentreFallback() {
        GeneratedHole dogleg = Arrays.stream(EnvironmentClassification.values())
                .flatMap(classification -> CourseGenerator.generate(coordinate(classification.ordinal() + 90, classification),
                        classification, CourseGenConstants.V3_GENERATOR_VERSION).holes().stream())
                .filter(hole -> !hole.spatialPlan().route().intermediateAnchors().isEmpty()).findFirst().orElseThrow();
        HoleModel model = dogleg.forRound(1);
        BallState tee = new BallState(model.geometry().tee(), Surface.TEE_BOX);
        ShotAim.Reference v3Aim = ShotAim.forBall(model, tee, Strategy.BALANCED);
        assertThat(v3Aim.progressionTarget()).isTrue();
        assertThat(v3Aim.target()).isNotEqualTo(model.geometry().greenCenter());

        GeneratedHole straight = CourseGenerator.generate(coordinate(29, EnvironmentClassification.PARKLAND),
                EnvironmentClassification.PARKLAND, CourseGenConstants.V3_GENERATOR_VERSION).holes().stream()
                .filter(hole -> hole.spatialPlan().route().intermediateAnchors().isEmpty()).findFirst().orElseThrow();
        HoleModel straightModel = straight.forRound(1);
        ShotAim.Reference straightAim = ShotAim.forBall(straightModel,
                new BallState(straightModel.geometry().tee(), Surface.TEE_BOX), Strategy.BALANCED);
        assertThat(straightAim.progressionTarget()).isFalse();
        assertThat(straightAim.target().x()).isEqualTo(straightModel.geometry().greenCenter().x());

        GeneratedHole legacy = CourseGenerator.generate(coordinate(3, EnvironmentClassification.COASTAL),
                EnvironmentClassification.COASTAL, CourseGenConstants.V2_GENERATOR_VERSION).holes().getFirst();
        HoleModel legacyModel = legacy.forRound(1);
        ShotAim.Reference legacyAim = ShotAim.forBall(legacyModel,
                new BallState(legacyModel.geometry().tee(), Surface.TEE_BOX), Strategy.BALANCED);
        assertThat(legacyAim.progressionTarget()).isFalse();
        assertThat(legacyAim.target().x()).isEqualTo(legacyModel.geometry().greenCenter().x());
    }

    @Test
    void v3SetupGeometryAndAutomaticRoundsRemainPlayable() {
        Course course = CourseGenerator.generate(coordinate(23, EnvironmentClassification.MOUNTAIN),
                EnvironmentClassification.MOUNTAIN, CourseGenConstants.V3_GENERATOR_VERSION);
        CourseSetup tight = new CourseSetup(1.6, 1.0, 0.65);
        for (GeneratedHole hole : course.holes()) {
            HoleModel model = hole.forRound(1, tight);
            assertThat(model.geometry().surfaceAt(model.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
            assertThat(model.geometry().surfaceAt(model.geometry().greenCenter())).isEqualTo(Surface.GREEN);
        }
        int strokes = 0;
        for (int hole = 1; hole <= 18; hole++) {
            RoundOutcome outcome = RoundResolver.resolveHole(course.holeModel(hole, 1, tight), Attributes.uniform(68),
                    GolferState.fresh(), Environment.calm(), Strategy.BALANCED,
                    new SeedCoordinate(WORLD, 3, 23, 1, 0, hole, 0));
            assertThat(outcome.shots()).hasSizeLessThan(20);
            strokes += outcome.totalStrokes();
        }
        assertThat(strokes).isBetween(55, 180);
    }

    private static SeedCoordinate coordinate(long id, EnvironmentClassification classification) {
        return new SeedCoordinate(WORLD, 2, id, classification.ordinal(), 0, 0, 0);
    }
}
