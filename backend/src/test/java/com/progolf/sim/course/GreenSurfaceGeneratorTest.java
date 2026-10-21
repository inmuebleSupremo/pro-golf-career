package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

class GreenSurfaceGeneratorTest {
    private static final long WORLD = 0x475245454e544553L;

    @Test
    void identicalStableInputsReconstructAnIdenticalField() {
        GeneratedHole hole = course(41, EnvironmentClassification.PARKLAND).holes().get(4);

        GreenSurface first = GreenSurfaceGenerator.generate(hole, GreenSurfaceVersion.V1);
        GreenSurface second = GreenSurfaceGenerator.generate(hole.geometry(), hole.holeSeed(),
                hole.spatialPlan().greenComplex().approachDirection(), GreenSurfaceVersion.V1);

        assertThat(first.provenance()).isEqualTo(second.provenance());
        assertThat(first.footprint()).isEqualTo(second.footprint());
        assertThat(first.columns()).isEqualTo(second.columns());
        assertThat(first.rows()).isEqualTo(second.rows());
        for (int row = 0; row < first.rows(); row++) for (int column = 0; column < first.columns(); column++) {
            assertThat(first.gridElevation(column, row)).isEqualTo(second.gridElevation(column, row));
            assertThat(first.gradientAt(first.gridPoint(column, row))).isEqualTo(second.gradientAt(second.gridPoint(column, row)));
        }
    }

    @Test
    void seedVariationIsReproducibleAndProducesMeaningfullyDifferentFields() {
        GeneratedHole firstHole = course(42, EnvironmentClassification.LINKS).holes().get(7);
        GeneratedHole secondHole = course(43, EnvironmentClassification.LINKS).holes().get(7);
        GreenSurface first = GreenSurfaceGenerator.generate(firstHole, GreenSurfaceVersion.V1);
        GreenSurface repeat = GreenSurfaceGenerator.generate(firstHole, GreenSurfaceVersion.V1);
        GreenSurface second = GreenSurfaceGenerator.generate(secondHole, GreenSurfaceVersion.V1);

        assertThat(first.provenance()).isEqualTo(repeat.provenance());
        assertThat(first.elevationAt(firstHole.geometry().greenCenter()))
                .isEqualTo(repeat.elevationAt(firstHole.geometry().greenCenter()));
        assertThat(first.provenance().surfaceSeed()).isNotEqualTo(second.provenance().surfaceSeed());
        assertThat(first.provenance().footprintFingerprint()).isNotEqualTo(second.provenance().footprintFingerprint());
    }

    @Test
    void generatedSurfacesStayFiniteBoundedAndPlayableAtAllEffectivePinsAcrossTheGeneratorEightCorpus() {
        for (EnvironmentClassification environment : EnvironmentClassification.values()) {
            for (long courseId : new long[] {13L, 97L}) {
                Course course = course(courseId, environment);
                for (GeneratedHole hole : course.holes()) {
                    GreenSurface surface = GreenSurfaceGenerator.generate(hole, GreenSurfaceVersion.V1);
                    assertThat(surface.footprint()).isEqualTo(hole.geometry().regions().stream()
                            .filter(region -> region.surface() == Surface.GREEN).findFirst().orElseThrow().boundary());
                    assertThat(surface.maximumSlope()).isLessThanOrEqualTo(GreenSurfaceGenerator.MAXIMUM_SLOPE + 1.0e-10);
                    assertThat(surface.maximumElevation() - surface.minimumElevation()).isGreaterThanOrEqualTo(0.0);
                    for (int row = 0; row < surface.rows(); row++) for (int column = 0; column < surface.columns(); column++) {
                        assertThat(Double.isFinite(surface.gridElevation(column, row))).isTrue();
                        assertThat(Double.isFinite(surface.gradientAt(surface.gridPoint(column, row)).magnitude())).isTrue();
                    }
                    Position2d cup = hole.forRound(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN).cupPosition();
                    GreenSurfaceSample pin = surface.sampleAt(cup);
                    assertThat(pin.location()).isEqualTo(GreenSurfaceLocation.ON_GREEN);
                    assertThat(pin.gradient().magnitude()).isLessThanOrEqualTo(GreenSurfaceGenerator.MAXIMUM_SLOPE + 1.0e-10);
                }
            }
        }
    }

    @Test
    void generatingUnusedSurfaceDataDoesNotChangeExistingCourseOrRoundOutcomes() {
        SeedCoordinate coordinate = coordinate(71, EnvironmentClassification.COASTAL);
        Course before = CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        RoundOutcome beforeRound = resolve(before);

        before.holes().forEach(hole -> GreenSurfaceGenerator.generate(hole, GreenSurfaceVersion.V1));

        Course after = CourseGenerator.generate(coordinate, EnvironmentClassification.COASTAL,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        RoundOutcome afterRound = resolve(after);
        assertThat(after).isEqualTo(before);
        assertThat(afterRound).isEqualTo(beforeRound);
    }

    private static Course course(long courseId, EnvironmentClassification environment) {
        return CourseGenerator.generate(coordinate(courseId, environment), environment,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
    }

    private static SeedCoordinate coordinate(long courseId, EnvironmentClassification environment) {
        return new SeedCoordinate(WORLD, 2, courseId, environment.ordinal(), 0, 0, 0);
    }

    private static RoundOutcome resolve(Course course) {
        GeneratedHole hole = course.holes().get(0);
        return RoundResolver.resolveHole(hole.forRound(1), Attributes.uniform(65), GolferState.fresh(), Environment.calm(),
                Strategy.BALANCED, new SeedCoordinate(WORLD, 2, 71, 1, 77, hole.number(), 0));
    }
}
