package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import org.junit.jupiter.api.Test;

class V6LandscapeArchitectureTest {
    private static final SeedCoordinate COORDINATE = new SeedCoordinate(0x56364c414e44L, 7, 13, 2, 0, 0, 0);

    @Test
    void v6IsDeterministicAndKeepsTheHistoricalPinsAvailable() {
        Course first = CourseGenerator.generate(COORDINATE, EnvironmentClassification.COASTAL,
                CourseGenConstants.V6_GENERATOR_VERSION);
        Course second = CourseGenerator.generate(COORDINATE, EnvironmentClassification.COASTAL,
                CourseGenConstants.V6_GENERATOR_VERSION);

        assertThat(first.generatorVersion()).isEqualTo(CourseGenConstants.V6_GENERATOR_VERSION);
        assertThat(first.landscapePlan()).isEqualTo(second.landscapePlan());
        assertThat(first.holes()).isEqualTo(second.holes());
        assertThat(CourseGenerator.supports(CourseGenConstants.V6_GENERATOR_VERSION)).isTrue();
        assertThat(CourseGenerator.generate(COORDINATE, EnvironmentClassification.COASTAL,
                CourseGenConstants.V5_GENERATOR_VERSION).landscapePlan()).isNull();
    }

    @Test
    void placementsTransformBackExactlyAndKeepNeighbourTransitionsBounded() {
        Course course = CourseGenerator.generate(COORDINATE, EnvironmentClassification.WOODLAND,
                CourseGenConstants.V6_GENERATOR_VERSION);
        CourseLandscapePlan landscape = course.landscapePlan();

        assertThat(landscape.placements()).hasSize(18);
        for (int i = 0; i < landscape.placements().size(); i++) {
            HolePlacement placement = landscape.placements().get(i);
            Position2d local = new Position2d(17.5, 243.25);
            Position2d restored = placement.transform().toLocal(placement.transform().toCourse(local));
            assertThat(restored.x()).isCloseTo(local.x(), org.assertj.core.data.Offset.offset(1e-9));
            assertThat(restored.y()).isCloseTo(local.y(), org.assertj.core.data.Offset.offset(1e-9));
            if (i < 17) assertThat(placement.transitionToNextTee()).isLessThanOrEqualTo(420.0);
        }
    }

    @Test
    void coastalAndWoodlandContextUseSharedFeaturesRatherThanPerHoleDecoration() {
        Course coast = CourseGenerator.generate(COORDINATE, EnvironmentClassification.COASTAL,
                CourseGenConstants.V6_GENERATOR_VERSION);
        Course wood = CourseGenerator.generate(COORDINATE, EnvironmentClassification.WOODLAND,
                CourseGenConstants.V6_GENERATOR_VERSION);

        assertThat(coast.landscapePlan().features()).anyMatch(feature -> feature.kind() == LandscapeFeatureKind.COAST_WATER);
        assertThat(coast.holes().stream().filter(hole -> hole.landscapeContext().relationship()
                == LandscapeRelationship.SHORELINE_RUN || hole.landscapeContext().relationship()
                == LandscapeRelationship.BAY_APPROACH)).hasSizeGreaterThanOrEqualTo(4);
        assertThat(coast.holes().stream().filter(hole -> hole.landscapeContext().relationship()
                == LandscapeRelationship.SHORELINE_RUN || hole.landscapeContext().relationship()
                == LandscapeRelationship.BAY_APPROACH).map(GeneratedHole::landscapeContext))
                .allSatisfy(context -> assertThat(context.features()).anyMatch(feature -> feature.id().equals("coast")
                        || feature.id().equals("bay")));
        assertThat(wood.landscapePlan().features()).anyMatch(feature -> feature.kind() == LandscapeFeatureKind.WOODLAND_MASS);
        assertThat(wood.holes().stream().filter(hole -> hole.landscapeContext().relationship()
                == LandscapeRelationship.WOODLAND_CORRIDOR)).hasSizeGreaterThanOrEqualTo(8);
        assertThat(CourseLandscapeQualityReport.inspect(coast).shorelineRelationships()
                + CourseLandscapeQualityReport.inspect(coast).bayRelationships()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void contextDoesNotReplaceCanonicalGameplayGeometry() {
        Course course = CourseGenerator.generate(COORDINATE, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V6_GENERATOR_VERSION);
        for (GeneratedHole hole : course.holes()) {
            assertThat(hole.landscapeContext()).isNotNull();
            assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotNull();
            assertThat(hole.geometry().regions()).isNotEmpty();
        }
    }
}
