package com.progolf.sim.course;

import com.progolf.sim.core.Seeds;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;

/** Open sea for the versioned Links extension of Generator 8. No separate course routing is introduced. */
final class V5CoastalLinks {
    private static final long COAST_SALT = 0x4c494e4b534541L;
    private static final double OCEAN_REACH = 1200.0;

    private V5CoastalLinks() { }

    /** A seven-hole coastal stretch gives 39% exposure and one shared land/sea orientation. */
    static List<Integer> holeIndices(long courseSeed, int holeCount) {
        int start = Math.floorMod(Seeds.deriveSeed(courseSeed, COAST_SALT), holeCount);
        List<Integer> result = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) result.add((start + offset) % holeCount);
        return List.copyOf(result);
    }

    static CourseGeometry addOcean(CourseGeometry geometry, HoleArchitecturePlan plan, long courseSeed,
                                   int holeNumber) {
        double side = side(courseSeed);
        int shape = shape(courseSeed, holeNumber); // long parallel shore, green headland, and a broad bay
        double length = plan.route().length();
        double phase = Math.floorMod(Seeds.deriveSeed(courseSeed, COAST_SALT + 2), 10_000) / 10_000.0 * 6.283185307179586;
        List<Position2d> shore = new ArrayList<>();
        for (int index = 0; index <= 80; index++) {
            double y = -320.0 + (length + 640.0) * index / 80.0;
            double routeDistance = Math.clamp(y, 0.0, length);
            Position2d route = plan.route().pointAt(routeDistance);
            double progress = routeDistance / length;
            double width = widthAt(plan.widthStations(), routeDistance, side);
            double sweep = 7.0 * StrictMath.sin(y / 53.0 + phase)
                    + 3.5 * StrictMath.sin(y / 21.0 + phase * .63);
            double feature = switch (shape) {
                case 1 -> -17.0 * bell(progress, .88, .19); // green on a headland
                case 2 -> 27.0 * bell(progress, .53, .23) - 9.0 * bell(progress, .79, .13); // bay
                default -> 0.0;
            };
            double coastX = route.x() + side * (width + 31.0 + sweep + feature);
            // The accepted route and green remain dry; only the sea-side recovery margin can be lost.
            for (TerrainRegion region : geometry.regions()) {
                if (region.surface() != Surface.FAIRWAY && region.surface() != Surface.FIRST_CUT
                        && region.surface() != Surface.GREEN && region.surface() != Surface.FRINGE) continue;
                for (Position2d vertex : region.boundary()) {
                    if (Math.abs(vertex.y() - y) < 16.0 && side * vertex.x() + 13.0 > side * coastX) {
                        coastX = vertex.x() + side * 13.0;
                    }
                }
            }
            shore.add(new Position2d(coastX, y));
        }
        List<Position2d> sea = new ArrayList<>(shore);
        sea.add(new Position2d(side * OCEAN_REACH, shore.getLast().y()));
        sea.add(new Position2d(side * OCEAN_REACH, shore.getFirst().y()));
        if (TerrainRegion.signedArea(sea) < 0.0) java.util.Collections.reverse(sea);
        List<TerrainRegion> regions = new ArrayList<>(geometry.regions());
        regions.add(new TerrainRegion(Surface.WATER, sea));
        return new CourseGeometry(geometry.tee(), geometry.greenCenter(), geometry.playableBoundary(), regions);
    }

    static double side(long courseSeed) {
        return (Seeds.deriveSeed(courseSeed, COAST_SALT + 1) & 1L) == 0 ? -1.0 : 1.0;
    }

    static int shape(long courseSeed, int holeNumber) {
        return Math.floorMod(holeNumber - 1 - holeIndices(courseSeed, 18).getFirst(), 18) % 3;
    }

    private static double bell(double x, double center, double radius) {
        double t = Math.max(0.0, 1.0 - Math.abs(x - center) / radius);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double widthAt(List<FairwayWidthStation> stations, double distance, double side) {
        for (int i = 1; i < stations.size(); i++) {
            FairwayWidthStation next = stations.get(i);
            if (distance > next.routeDistance()) continue;
            FairwayWidthStation previous = stations.get(i - 1);
            double span = next.routeDistance() - previous.routeDistance();
            double t = span == 0.0 ? 0.0 : (distance - previous.routeDistance()) / span;
            double from = side < 0 ? previous.leftHalfWidth() : previous.rightHalfWidth();
            double to = side < 0 ? next.leftHalfWidth() : next.rightHalfWidth();
            return from + (to - from) * t;
        }
        FairwayWidthStation last = stations.getLast();
        return side < 0 ? last.leftHalfWidth() : last.rightHalfWidth();
    }
}
