package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoastalLinksGenerationTest {
    @Test
    void sevenRouteAdjacentHolesHaveOpenSeaWhileTheOthersRemainInland() {
        for (long seed : List.of(103L, 271L)) {
            SeedCoordinate coordinate = new SeedCoordinate(seed, 0, 0, 0, 0, 0, 0);
            Course coast = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                    CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION);
            assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                    CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION)).isEqualTo(coast);
            List<GeneratedHole> coastal = coast.holes().stream().filter(hole -> ocean(hole) != null).toList();
            assertThat(coastal).hasSize(7);
            for (GeneratedHole hole : coastal) {
                TerrainRegion ocean = ocean(hole);
                double minX = ocean.boundary().stream().mapToDouble(Position2d::x).min().orElseThrow();
                double maxX = ocean.boundary().stream().mapToDouble(Position2d::x).max().orElseThrow();
                assertThat(maxX - minX).isGreaterThan(1000.0);
                assertThat(ocean.boundary().stream().mapToDouble(Position2d::y).min().orElseThrow()).isLessThan(-80.0);
                assertThat(ocean.boundary().stream().mapToDouble(Position2d::y).max().orElseThrow())
                        .isGreaterThan(hole.geometry().greenCenter().y() + 70.0);
                assertThat(hole.geometry().surfaceAt(hole.geometry().tee())).isNotEqualTo(Surface.WATER);
                assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter())).isEqualTo(Surface.GREEN);
                double side = Math.abs(minX) > Math.abs(maxX) ? -1.0 : 1.0;
                assertThat(ocean.boundary().stream().filter(point -> Math.abs(point.x()) < 1000.0)
                        .anyMatch(point -> hole.geometry().surfaceAt(point.plus(side * 3.0, 0.0)) == Surface.WATER))
                        .as("near-shore water is an authoritative hazard on hole %s", hole.number()).isTrue();
                for (TerrainRegion region : hole.geometry().regions()) {
                    if (region.surface() == Surface.FAIRWAY || region.surface() == Surface.GREEN) {
                        assertThat(TerrainRegion.strictlyOverlaps(region.boundary(), ocean.boundary())).isFalse();
                    }
                }
            }
            assertThat(coast.holes().stream().filter(hole -> ocean(hole) == null)).hasSize(11);
            Course historical = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                    CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
            assertThat(historical.holes()).allSatisfy(hole -> assertThat(ocean(hole)).isNull());
            assertThat(coastal.stream().filter(hole -> !hole.architecturePlan().route().equals(
                    historical.holes().get(hole.number() - 1).architecturePlan().route())).count())
                    .as("coastal intent participates in candidate route selection").isGreaterThan(0);
            Course parkland = CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                    CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION);
            assertThat(parkland.holes()).allSatisfy(hole -> assertThat(ocean(hole)).isNull());
            assertThat(parkland.holes().stream().map(GeneratedHole::geometry).toList()).isEqualTo(
                    CourseGenerator.generate(coordinate, EnvironmentClassification.PARKLAND,
                            CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION)
                            .holes().stream().map(GeneratedHole::geometry).toList());
        }
    }

    private static TerrainRegion ocean(GeneratedHole hole) {
        return hole.geometry().regions().stream().filter(region -> region.surface() == Surface.WATER
                && region.boundary().stream().mapToDouble(Position2d::x).max().orElseThrow()
                - region.boundary().stream().mapToDouble(Position2d::x).min().orElseThrow() > 600).findFirst().orElse(null);
    }
}
