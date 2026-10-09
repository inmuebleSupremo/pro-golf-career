package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotAim;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class V5OrganicHoleArchitectureTest {
    private static final long WORLD = 0x5635_4152_4348L;

    @Test
    void v5IsDeterministicAndRetainsPriorVersions() {
        SeedCoordinate coordinate = coordinate(11, EnvironmentClassification.PARKLAND);
        Course v5 = CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V5_GENERATOR_VERSION);

        assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V5_GENERATOR_VERSION)).isEqualTo(v5);
        assertThat(v5.generatorVersion()).isEqualTo(CourseGenConstants.V5_GENERATOR_VERSION);
        assertThat(v5.architecturePlan()).isNotNull();
        assertThat(v5.architecturePlan().holes()).hasSize(18);
        assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V4_GENERATOR_VERSION).architecturePlan()).isNull();
    }

    @Test
    void completeV5CoursesContainConnectedVariedCanonicalArchitecture() {
        long oneTurn = 0;
        long twoTurns = 0;
        long asymmetricStations = 0;
        long distinctSignatures = 0;
        long bunkerRegions = 0;
        long treeRegions = 0;
        for (EnvironmentClassification classification : EnvironmentClassification.values()) {
            for (int id = 0; id < 6; id++) {
                Course course = CourseGenerator.generate(coordinate(id, classification), classification,
                        CourseGenConstants.V5_GENERATOR_VERSION);
                assertThat(course.totalPar()).isEqualTo(72);
                assertThat(course.holes()).allSatisfy(hole -> {
                    assertThat(hole.architecturePlan()).isNotNull();
                    assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
                    assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter())).isEqualTo(Surface.GREEN);
                    assertThat(hole.geometry().playableBoundary()).hasSizeGreaterThan(5);
                    assertThat(hole.architecturePlan().greenFootprint()).hasSizeGreaterThanOrEqualTo(10);
                });
                oneTurn += course.architecturePlan().holes().stream()
                        .filter(plan -> plan.route().intermediateAnchors().size() == 1).count();
                twoTurns += course.architecturePlan().holes().stream()
                        .filter(plan -> plan.route().intermediateAnchors().size() == 2).count();
                asymmetricStations += course.architecturePlan().holes().stream().flatMap(plan -> plan.widthStations().stream())
                        .filter(station -> Math.abs(station.leftHalfWidth() - station.rightHalfWidth()) > 0.5).count();
                distinctSignatures += course.architecturePlan().holes().stream().map(HoleArchitecturePlan::structuralSignature)
                        .distinct().count();
                bunkerRegions += CourseQualityReport.inspect(course).bunkerRegions();
                treeRegions += CourseQualityReport.inspect(course).treeRegions();
            }
        }
        assertThat(oneTurn).isPositive();
        assertThat(twoTurns).isPositive();
        assertThat(asymmetricStations).isPositive();
        assertThat(distinctSignatures).isGreaterThan(80);
        assertThat(bunkerRegions).isGreaterThan(120);
        assertThat(treeRegions).isPositive();
    }

    @Test
    void v5ProgressionTargetsAndLegacyGuidanceRemainCanonicalAndPlayable() {
        Course course = Arrays.stream(EnvironmentClassification.values()).map(classification ->
                        CourseGenerator.generate(coordinate(71 + classification.ordinal(), classification), classification,
                                CourseGenConstants.V5_GENERATOR_VERSION))
                .findFirst().orElseThrow();
        GeneratedHole turning = course.holes().stream().filter(hole -> hole.architecturePlan().route()
                .intermediateAnchors().size() == 2).findFirst().orElseThrow();
        HoleModel model = turning.forRound(1);
        BallState ball = new BallState(model.geometry().tee(), Surface.TEE_BOX);
        ShotAim.Reference aim = ShotAim.forBall(model, ball, Strategy.BALANCED);

        assertThat(aim.progressionTarget()).isTrue();
        assertThat(model.geometry().surfaceAt(aim.target())).isIn(Surface.FAIRWAY, Surface.FIRST_CUT,
                Surface.PRIMARY_ROUGH, Surface.DEEP_ROUGH);
        assertThat(model.strategicLandingPlan()).isNotNull();
    }

    @Test
    void qualityReportShowsWholeCourseStructuralEvidenceAgainstV4() {
        SeedCoordinate coordinate = coordinate(123, EnvironmentClassification.COASTAL);
        CourseQualityReport v4 = CourseQualityReport.inspect(CourseGenerator.generate(coordinate,
                EnvironmentClassification.COASTAL, CourseGenConstants.V4_GENERATOR_VERSION));
        CourseQualityReport v5 = CourseQualityReport.inspect(CourseGenerator.generate(coordinate,
                EnvironmentClassification.COASTAL, CourseGenConstants.V5_GENERATOR_VERSION));

        assertThat(v5.holes()).isEqualTo(18);
        assertThat(v5.twoTurnHoles()).isPositive();
        assertThat(v5.twoTurnHoles()).isLessThanOrEqualTo(2);
        assertThat(v5.straightHoles() + v5.gentleHoles() + v5.doglegHoles() + v5.twoTurnHoles()).isEqualTo(18);
        assertThat(v5.asymmetricWidthStations()).isPositive();
        assertThat(v5.bunkerRegions()).isPositive();
        assertThat(v5.distinctSignatureBuckets()).isGreaterThanOrEqualTo(14);
        assertThat(v5.meanRouteChordRatio()).isGreaterThan(v4.meanRouteChordRatio());
    }

    @Test
    void architectureIdentityChangesMeasuredCourseTendenciesWithoutTemplates() {
        SeedCoordinate coordinate = coordinate(212, EnvironmentClassification.LINKS);
        CourseDesignProfile design = new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.BALANCED,
                RecoverySeverity.BALANCED);
        Course restrained = CourseGenerator.generateV5(coordinate, EnvironmentClassification.LINKS, design,
                new CourseArchitectureProfile(.05, .10, .12, .85, .10));
        Course moving = CourseGenerator.generateV5(coordinate, EnvironmentClassification.LINKS, design,
                new CourseArchitectureProfile(.95, .85, .88, .20, .92));

        assertThat(restrained.architecturePlan().profile()).isNotEqualTo(moving.architecturePlan().profile());
        assertThat(CourseQualityReport.inspect(moving).meanRouteChordRatio())
                .isGreaterThan(CourseQualityReport.inspect(restrained).meanRouteChordRatio());
        assertThat(moving.architecturePlan().holes()).extracting(HoleArchitecturePlan::structuralSignature)
                .isNotEqualTo(restrained.architecturePlan().holes().stream()
                        .map(HoleArchitecturePlan::structuralSignature).toList());
    }

    @Test
    void environmentCharacterIsMeasuredFromCanonicalPlayableGeometry() {
        Course parkland = CourseGenerator.generate(coordinate(310, EnvironmentClassification.PARKLAND),
                EnvironmentClassification.PARKLAND, CourseGenConstants.V5_GENERATOR_VERSION);
        Course woodland = CourseGenerator.generate(coordinate(311, EnvironmentClassification.WOODLAND),
                EnvironmentClassification.WOODLAND, CourseGenConstants.V5_GENERATOR_VERSION);
        Course links = CourseGenerator.generate(coordinate(312, EnvironmentClassification.LINKS),
                EnvironmentClassification.LINKS, CourseGenConstants.V5_GENERATOR_VERSION);
        Course coastal = CourseGenerator.generate(coordinate(313, EnvironmentClassification.COASTAL),
                EnvironmentClassification.COASTAL, CourseGenConstants.V5_GENERATOR_VERSION);
        CourseEnvironmentalQualityReport parklandReport = CourseEnvironmentalQualityReport.inspect(parkland);
        CourseEnvironmentalQualityReport woodlandReport = CourseEnvironmentalQualityReport.inspect(woodland);
        CourseEnvironmentalQualityReport linksReport = CourseEnvironmentalQualityReport.inspect(links);
        CourseEnvironmentalQualityReport coastalReport = CourseEnvironmentalQualityReport.inspect(coastal);

        assertThat(parklandReport.holesWithInBoundsTrees()).isEqualTo(18);
        assertThat(parklandReport.fairwayAdjacentTreeRegions()).isPositive();
        assertThat(parklandReport.fairwayBunkers()).isGreaterThanOrEqualTo(16);
        assertThat(woodlandReport.treeRegionsInBounds()).isGreaterThan(parklandReport.treeRegionsInBounds());
        assertThat(woodlandReport.woodedCorridorHoles()).isGreaterThanOrEqualTo(12);
        assertThat(linksReport.treeRegionsInBounds()).isLessThanOrEqualTo(3);
        assertThat(linksReport.shorelineAdjacentHoles()).isBetween(4, 5);
        assertThat(coastalReport.shorelineAdjacentHoles()).isBetween(4, 5);
        assertThat(coastalReport.waterAreaRatio()).isGreaterThan(0.03);
    }

    private static SeedCoordinate coordinate(long id, EnvironmentClassification classification) {
        return new SeedCoordinate(WORLD, 5, id, classification.ordinal(), 0, 0, 0);
    }
}
