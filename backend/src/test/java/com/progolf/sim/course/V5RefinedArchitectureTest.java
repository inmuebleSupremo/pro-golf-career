package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class V5RefinedArchitectureTest {
    private static final long WORLD = 0x5635524546494e45L;

    @Test
    void experimentalRefinementIsDeterministicAndKeepsLegalPinsAcrossBothBiomes() {
        for (EnvironmentClassification environment : new EnvironmentClassification[] {
                EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS}) {
            for (long seed : new long[] {4242, 99127, 812347, 137997, 21183, 615733, 45819, 746122}) {
                SeedCoordinate coordinate = new SeedCoordinate(WORLD, 5, seed, environment.ordinal(), 0, 0, 0);
                Course course = CourseGenerator.generate(coordinate, environment,
                        CourseGenConstants.V5_REFINED_GENERATOR_VERSION);
                assertThat(CourseGenerator.generate(coordinate, environment,
                        CourseGenConstants.V5_REFINED_GENERATOR_VERSION)).isEqualTo(course);
                assertThat(course.generatorVersion()).isEqualTo(CourseGenConstants.V5_REFINED_GENERATOR_VERSION);
                for (GeneratedHole hole : course.holes()) {
                    PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
                    assertThat(hole.geometry().surfaceAt(pin.cup())).isEqualTo(Surface.GREEN);
                    assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.OUT_OF_BOUNDS);
                    List<TerrainRegion> hazards = new ArrayList<>();
                    for (TerrainRegion region : hole.geometry().regions()) {
                        if (region.surface() != Surface.WATER && region.surface() != Surface.TREES
                                && region.surface() != Surface.BUNKER && region.surface() != Surface.WASTE_AREA) continue;
                        assertThat(V5GeometryGenerator.hazardsHaveRoom(region, hazards))
                                .as("hazard composition: %s seed %s hole %s", environment, seed, hole.number())
                                .isTrue();
                        if (region.surface() == Surface.BUNKER) {
                            assertThat(region.boundary()).allSatisfy(point ->
                                    assertThat(hole.geometry().surfaceAt(point))
                                            .as("bunker within playable terrain: %s seed %s hole %s", environment, seed, hole.number())
                                            .isNotEqualTo(Surface.OUT_OF_BOUNDS));
                        }
                        hazards.add(region);
                    }
                }
            }
        }
    }

    @Test
    void retainedV5RemainsTheDefaultAndItsOwnVersionPath() {
        assertThat(CourseGenConstants.CURRENT_GENERATOR_VERSION).isEqualTo(CourseGenConstants.V5_GENERATOR_VERSION);
        SeedCoordinate coordinate = new SeedCoordinate(WORLD, 5, 4242, EnvironmentClassification.PARKLAND.ordinal(), 0, 0, 0);
        Course original = CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V5_GENERATOR_VERSION);
        Course refined = CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V5_REFINED_GENERATOR_VERSION);
        assertThat(original.generatorVersion()).isEqualTo(5);
        assertThat(original.holes().get(1).geometry()).isNotEqualTo(refined.holes().get(1).geometry());
    }
}
