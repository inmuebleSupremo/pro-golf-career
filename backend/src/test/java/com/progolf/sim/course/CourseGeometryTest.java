package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseGeometryTest {

    @Test
    void lookupUsesCentralPrecedenceAndBoundary() {
        List<Position2d> outer = square(-20, -20, 20, 20);
        CourseGeometry geometry = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, List.of(
                new TerrainRegion(Surface.FAIRWAY, square(-12, -12, 12, 12)),
                new TerrainRegion(Surface.WATER, square(-3, -3, 3, 3)),
                new TerrainRegion(Surface.BUNKER, square(6, 6, 10, 10))));

        assertThat(geometry.surfaceAt(new Position2d(0, 0))).isEqualTo(Surface.WATER);
        assertThat(geometry.surfaceAt(new Position2d(8, 8))).isEqualTo(Surface.BUNKER);
        assertThat(geometry.surfaceAt(new Position2d(11, 0))).isEqualTo(Surface.FAIRWAY);
        assertThat(geometry.surfaceAt(new Position2d(30, 0))).isEqualTo(Surface.OUT_OF_BOUNDS);
    }

    @Test
    void rejectsAmbiguousSamePrecedenceOverlapButResolvesGreenOverFringe() {
        List<Position2d> outer = square(-20, -20, 20, 20);
        assertThatThrownBy(() -> new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, List.of(
                new TerrainRegion(Surface.WATER, square(-8, -8, 2, 2)),
                new TerrainRegion(Surface.OUT_OF_BOUNDS, square(-2, -2, 8, 8)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ambiguous terrain overlap");

        CourseGeometry greenAndFringe = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, List.of(
                new TerrainRegion(Surface.FRINGE, square(-10, -10, 10, 10)),
                new TerrainRegion(Surface.GREEN, square(-6, -6, 6, 6))));
        assertThat(greenAndFringe.surfaceAt(new Position2d(0, 0))).isEqualTo(Surface.GREEN);
        assertThat(greenAndFringe.surfaceAt(new Position2d(8, 0))).isEqualTo(Surface.FRINGE);
    }

    @Test
    void outsidePlayableBoundaryIsOutOfBoundsEvenWhenAnArtworkRegionExtendsPastIt() {
        CourseGeometry geometry = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15),
                square(-10, -10, 10, 10), List.of(new TerrainRegion(Surface.TREES, square(-20, -20, 20, 20))));

        assertThat(geometry.surfaceAt(new Position2d(0, 0))).isEqualTo(Surface.TREES);
        assertThat(geometry.surfaceAt(new Position2d(15, 0))).isEqualTo(Surface.OUT_OF_BOUNDS);
    }

    @Test
    void generatedGeometryIsSeedStableAndNonTrivial() {
        GeneratedHole first = new GeneratedHole(1, 4, 440, 24, 14, 28, true, true, true, 0, 73L);
        GeneratedHole again = new GeneratedHole(1, 4, 440, 24, 14, 28, true, true, true, 0, 73L);

        assertThat(first.geometry()).isEqualTo(again.geometry());
        assertThat(first.geometry().regions()).anyMatch(r -> r.surface() == Surface.FAIRWAY)
                .anyMatch(r -> r.surface() == Surface.GREEN)
                .anyMatch(r -> r.surface() == Surface.BUNKER)
                .anyMatch(r -> r.surface() == Surface.WATER)
                .anyMatch(r -> r.surface() == Surface.TREES);
        assertThat(first.geometry().playableBoundary()).hasSizeGreaterThan(20);
        assertThat(first.geometry().surfaceAt(first.geometry().greenCenter())).isEqualTo(Surface.GREEN);
    }

    @Test
    void generatedGeometryKeepsAPlayableOverGreenTroubleCorridor() {
        GeneratedHole hole = new GeneratedHole(1, 4, 440, 24, 14, 28, true, true, true, 0, 73L);

        assertThat(hole.geometry().surfaceAt(new Position2d(0, 440 + 35))).isNotEqualTo(Surface.OUT_OF_BOUNDS);
        assertThat(hole.geometry().surfaceAt(new Position2d(0, 440 + 60))).isEqualTo(Surface.OUT_OF_BOUNDS);
    }

    @Test
    void fringeOnlyPointDoesNotResolveAsGreen() {
        GeneratedHole hole = new GeneratedHole(1, 4, 440, 24, 14, 28, false, false, false, 0, 73L);

        assertThat(hole.geometry().surfaceAt(new Position2d(16, 440))).isEqualTo(Surface.FRINGE);
    }

    @Test
    void lateralScaleWidensTheCanonicalTerrainWithoutChangingLength() {
        CourseGeometry geometry = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 20),
                square(-10, 0, 10, 30), List.of(new TerrainRegion(Surface.FAIRWAY, square(-5, 0, 5, 30))));

        CourseGeometry wide = geometry.withLateralScale(1.5);
        assertThat(wide.surfaceAt(new Position2d(7, 10))).isEqualTo(Surface.FAIRWAY);
        assertThat(wide.greenCenter().y()).isEqualTo(geometry.greenCenter().y());
    }

    @Test
    void everyClassificationGeneratesStableValidatedCanonicalGeometry() {
        for (EnvironmentClassification classification : EnvironmentClassification.values()) {
            Course first = CourseGenerator.generate(new SeedCoordinate(991L, 2, classification.ordinal(), 0, 0, 0, 0),
                    classification);
            Course again = CourseGenerator.generate(new SeedCoordinate(991L, 2, classification.ordinal(), 0, 0, 0, 0),
                    classification);

            assertThat(first.holes()).extracting(GeneratedHole::geometry)
                    .containsExactlyElementsOf(again.holes().stream().map(GeneratedHole::geometry).toList());
            for (GeneratedHole hole : first.holes()) {
                assertThat(hole.geometry().playableBoundary()).hasSizeGreaterThanOrEqualTo(3);
                assertThat(hole.geometry().surfaceAt(hole.geometry().tee()))
                        .as("%s hole %s tee", classification, hole.number())
                        .isNotEqualTo(Surface.OUT_OF_BOUNDS);
                assertThat(hole.geometry().surfaceAt(hole.geometry().greenCenter()))
                        .as("%s hole %s green centre", classification, hole.number())
                        .isEqualTo(Surface.GREEN);
            }
        }
    }

    private static List<Position2d> square(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
    }
}
