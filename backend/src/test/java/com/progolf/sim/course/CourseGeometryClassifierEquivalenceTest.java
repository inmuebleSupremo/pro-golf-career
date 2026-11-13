package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.world.DevelopmentTourCatalogue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Compares terrain lookup with the original stream classifier, including its boundary rules. */
class CourseGeometryClassifierEquivalenceTest {
    private static final long WORLD_SEED = 0x5445525241494e4cL;

    @Test
    void explicitPrecedenceFallbackAndBoundaryCasesMatchOriginalClassifier() {
        List<Position2d> outer = square(-20, -20, 20, 20);
        List<TerrainRegion> regions = List.of(
                new TerrainRegion(Surface.TEE_BOX, square(-18, -18, 18, 18)),
                new TerrainRegion(Surface.DEEP_ROUGH, square(-15, -15, 15, 15)),
                new TerrainRegion(Surface.FAIRWAY, square(-10, -10, 10, 10)),
                new TerrainRegion(Surface.BUNKER, square(5, 5, 9, 9)),
                new TerrainRegion(Surface.WATER, square(-3, -3, 3, 3)));
        List<TerrainRegion> reversed = new ArrayList<>(regions);
        java.util.Collections.reverse(reversed);
        for (List<TerrainRegion> ordered : List.of(regions, reversed)) {
            CourseGeometry geometry = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, ordered);
            for (Position2d point : List.of(new Position2d(0, 0), new Position2d(7, 7),
                    new Position2d(12, 0), new Position2d(17, 0), new Position2d(19, 0),
                    new Position2d(20, 0), new Position2d(21, 0))) {
                assertSameClassification(geometry, point, "precedence and fallback");
            }
            assertBoundarySamples(geometry, outer);
            for (TerrainRegion region : ordered) assertBoundarySamples(geometry, region.boundary());
        }

        CourseGeometry noRegions = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, List.of());
        assertThat(noRegions.surfaceAt(new Position2d(0, 0))).isEqualTo(Surface.PRIMARY_ROUGH);
        assertSameClassification(noRegions, new Position2d(0, 0), "no matching region");

        // Retain the first matching surface even when equal-priority regions have identical boundaries.
        // The existing strict-overlap validator does not reject coincident polygon boundaries.
        for (Surface first : List.of(Surface.WATER, Surface.OUT_OF_BOUNDS)) {
            Surface second = first == Surface.WATER ? Surface.OUT_OF_BOUNDS : Surface.WATER;
            CourseGeometry tied = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, List.of(
                    new TerrainRegion(first, square(-4, -4, 4, 4)),
                    new TerrainRegion(second, square(-4, -4, 4, 4))));
            assertThat(tied.surfaceAt(new Position2d(0, 0))).isEqualTo(first);
            assertSameClassification(tied, new Position2d(0, 0), "equal-precedence first match");
        }

        // Different surfaces with equal precedence meet at x=0 without strictly overlapping.
        CourseGeometry contact = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 15), outer, List.of(
                new TerrainRegion(Surface.WATER, square(-10, -10, 0, 10)),
                new TerrainRegion(Surface.OUT_OF_BOUNDS, square(0, -10, 10, 10))));
        for (double x : new double[] {-1.0e-8, -1.0e-10, 0.0, 1.0e-10, 1.0e-8}) {
            assertSameClassification(contact, new Position2d(x, 0), "equal-precedence contact");
        }
        assertBoundarySamples(contact, contact.regions().get(0).boundary());
        assertBoundarySamples(contact, contact.regions().get(1).boundary());
    }

    @Test
    void generatedCourseFamiliesAndVersionsMatchOriginalClassifier() {
        int[] versions = {CourseGenConstants.V5_REFINED_GENERATOR_VERSION,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION,
                CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION};
        EnvironmentClassification[] families = {EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS,
                EnvironmentClassification.HEATHLAND, EnvironmentClassification.WOODLAND, EnvironmentClassification.DESERT};
        for (int version : versions) for (EnvironmentClassification family : families) {
            SeedCoordinate coordinate = new SeedCoordinate(WORLD_SEED, version, family.ordinal(), 0, 0, 0, 0);
            Course course = CourseGenerator.generate(coordinate, family, version);
            for (int holeNumber : new int[] {1, 9, 18}) {
                CourseGeometry geometry = course.holes().get(holeNumber - 1).geometry();
                String label = "generator " + version + " " + family + " hole " + holeNumber;
                assertGeneratedSamples(geometry, label, WORLD_SEED + 31L * version + 7L * family.ordinal() + holeNumber);
                if (holeNumber == 9) {
                    assertGeneratedSamples(geometry.withLateralScale(0.85), label + " narrow setup",
                            WORLD_SEED + 31L * version + 7L * family.ordinal() + holeNumber);
                }
            }
        }
    }

    @Test
    void developmentTourVenueRecipesMatchOriginalClassifier() {
        for (String eventId : List.of("lowcountry-coastal", "sandhills-classic", "coachella-valley")) {
            var event = DevelopmentTourCatalogue.forId(eventId);
            int venueIndex = DevelopmentTourCatalogue.selectedVenueIndex(event, WORLD_SEED, 2);
            var venue = DevelopmentTourCatalogue.venue(venueIndex);
            SeedCoordinate coordinate = new SeedCoordinate(WORLD_SEED, 0x44455656454e5545L, venueIndex, 0, 0, 0, 0);
            Course course = CourseGenerator.generate(coordinate, venue.classification(),
                    CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION);
            for (int holeNumber : new int[] {1, 18}) {
                assertGeneratedSamples(course.holes().get(holeNumber - 1).geometry(), venue.id() + " hole " + holeNumber,
                        WORLD_SEED + venueIndex * 17L + holeNumber);
            }
        }
    }

    private static void assertGeneratedSamples(CourseGeometry geometry, String label, long seed) {
        assertSameClassification(geometry, geometry.tee(), label + " tee");
        assertSameClassification(geometry, geometry.greenCenter(), label + " green");
        assertSelectedBoundarySamples(geometry, geometry.playableBoundary());
        for (TerrainRegion region : geometry.regions()) assertSelectedBoundarySamples(geometry, region.boundary());
        double minX = geometry.playableBoundary().stream().mapToDouble(Position2d::x).min().orElseThrow();
        double maxX = geometry.playableBoundary().stream().mapToDouble(Position2d::x).max().orElseThrow();
        double minY = geometry.playableBoundary().stream().mapToDouble(Position2d::y).min().orElseThrow();
        double maxY = geometry.playableBoundary().stream().mapToDouble(Position2d::y).max().orElseThrow();
        SplittableRandom random = new SplittableRandom(seed);
        for (int i = 0; i < 32; i++) {
            Position2d point = new Position2d(random.nextDouble(minX - 10, maxX + 10),
                    random.nextDouble(minY - 10, maxY + 10));
            assertSameClassification(geometry, point, label + " sample " + i);
        }
    }

    private static void assertSelectedBoundarySamples(CourseGeometry geometry, List<Position2d> polygon) {
        for (int index : new int[] {0, polygon.size() / 2, polygon.size() - 1}) {
            assertBoundaryPoint(geometry, polygon.get(index));
            Position2d next = polygon.get((index + 1) % polygon.size());
            assertBoundaryPoint(geometry, new Position2d((polygon.get(index).x() + next.x()) / 2.0,
                    (polygon.get(index).y() + next.y()) / 2.0));
        }
    }

    private static void assertBoundarySamples(CourseGeometry geometry, List<Position2d> polygon) {
        for (int index = 0; index < polygon.size(); index++) {
            assertBoundaryPoint(geometry, polygon.get(index));
            Position2d next = polygon.get((index + 1) % polygon.size());
            assertBoundaryPoint(geometry, new Position2d((polygon.get(index).x() + next.x()) / 2.0,
                    (polygon.get(index).y() + next.y()) / 2.0));
        }
    }

    private static void assertBoundaryPoint(CourseGeometry geometry, Position2d point) {
        assertSameClassification(geometry, point, "boundary");
        for (double delta : new double[] {1.0e-10, 1.0e-8}) {
            assertSameClassification(geometry, point.plus(delta, 0), "boundary +x");
            assertSameClassification(geometry, point.plus(-delta, 0), "boundary -x");
            assertSameClassification(geometry, point.plus(0, delta), "boundary +y");
            assertSameClassification(geometry, point.plus(0, -delta), "boundary -y");
        }
    }

    private static void assertSameClassification(CourseGeometry geometry, Position2d point, String label) {
        assertThat(geometry.surfaceAt(point)).as("%s at %s", label, point)
                .isEqualTo(originalSurfaceAt(geometry, point));
    }

    /** Frozen pre-optimisation implementation; deliberately retains its stream and polygon tests. */
    private static Surface originalSurfaceAt(CourseGeometry geometry, Position2d position) {
        Objects.requireNonNull(position, "position");
        if (!originalPlayableContains(geometry.playableBoundary(), position)) return Surface.OUT_OF_BOUNDS;
        return geometry.regions().stream()
                .filter(region -> region.contains(position))
                .min(Comparator.comparingInt(region -> originalPrecedence(region.surface())))
                .map(TerrainRegion::surface)
                .orElse(Surface.PRIMARY_ROUGH);
    }

    private static int originalPrecedence(Surface surface) {
        return switch (surface) {
            case WATER, OUT_OF_BOUNDS -> 0;
            case BUNKER, WASTE_AREA -> 1;
            case GREEN -> 2;
            case FRINGE -> 3;
            case TREES, RECOVERY_AREA -> 4;
            case FAIRWAY -> 5;
            case FIRST_CUT -> 6;
            case PRIMARY_ROUGH -> 7;
            case DEEP_ROUGH -> 8;
            case TEE_BOX -> 9;
        };
    }

    private static boolean originalPlayableContains(List<Position2d> polygon, Position2d point) {
        for (int i = 0; i < polygon.size(); i++) {
            if (originalOnSegment(polygon.get(i), polygon.get((i + 1) % polygon.size()), point)) return true;
        }
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Position2d a = polygon.get(i);
            Position2d b = polygon.get(j);
            if ((a.y() > point.y()) != (b.y() > point.y())
                    && point.x() < (b.x() - a.x()) * (point.y() - a.y()) / (b.y() - a.y()) + a.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean originalOnSegment(Position2d a, Position2d b, Position2d point) {
        double cross = (b.x() - a.x()) * (point.y() - a.y())
                - (b.y() - a.y()) * (point.x() - a.x());
        return Math.abs(cross) < 1.0e-9
                && point.x() >= Math.min(a.x(), b.x()) - 1.0e-9
                && point.x() <= Math.max(a.x(), b.x()) + 1.0e-9
                && point.y() >= Math.min(a.y(), b.y()) - 1.0e-9
                && point.y() <= Math.max(a.y(), b.y()) + 1.0e-9;
    }

    private static List<Position2d> square(double minX, double minY, double maxX, double maxY) {
        return List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY));
    }
}
