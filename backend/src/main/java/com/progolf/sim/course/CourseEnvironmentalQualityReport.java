package com.progolf.sim.course;

import com.progolf.sim.spatial.Surface;

/** Canonical-geometry evidence for environmental character; it intentionally measures more than raw region totals. */
public record CourseEnvironmentalQualityReport(int treeRegionsInBounds, int treeRegionsOutsideBounds,
                                               int holesWithInBoundsTrees, int fairwayAdjacentTreeRegions,
                                               int woodedCorridorHoles, int fairwayBunkers, int greensideBunkers,
                                               int largeBunkers, int groupedBunkerHoles, int waterRegions,
                                               int shorelineAdjacentHoles, double waterAreaRatio) {
    public static CourseEnvironmentalQualityReport inspect(Course course) {
        int treesIn = 0, treesOut = 0, treeHoles = 0, adjacentTrees = 0, corridorHoles = 0;
        int fairwayBunkers = 0, greensideBunkers = 0, largeBunkers = 0, groupedBunkerHoles = 0;
        int waterRegions = 0, shorelineHoles = 0;
        double waterArea = 0.0, playableArea = 0.0;
        for (GeneratedHole hole : course.holes()) {
            CourseGeometry geometry = hole.geometry();
            playableArea += Math.abs(TerrainRegion.signedArea(geometry.playableBoundary()));
            int inBoundTrees = 0;
            int holeBunkers = 0;
            boolean shoreline = false;
            for (TerrainRegion region : geometry.regions()) {
                Position2d center = centroid(region);
                double area = Math.abs(TerrainRegion.signedArea(region.boundary()));
                if (region.surface() == Surface.TREES) {
                    // The centroid tests whether this irregular canonical region has meaningful playable-course
                    // presence; an outer fringe may legitimately extend beyond the boundary.
                    if (geometry.surfaceAt(center) != Surface.OUT_OF_BOUNDS) {
                        treesIn++;
                        inBoundTrees++;
                        if (fairwayAdjacent(hole, center)) adjacentTrees++;
                    } else treesOut++;
                } else if (region.surface() == Surface.BUNKER) {
                    holeBunkers++;
                    if (center.distanceTo(geometry.greenCenter()) < 58.0) greensideBunkers++;
                    else fairwayBunkers++;
                    if (area >= 145.0) largeBunkers++;
                } else if (region.surface() == Surface.WATER) {
                    waterRegions++;
                    waterArea += area;
                    double minY = region.boundary().stream().mapToDouble(Position2d::y).min().orElse(center.y());
                    double maxY = region.boundary().stream().mapToDouble(Position2d::y).max().orElse(center.y());
                    if (maxY - minY >= 70.0 && area >= 900.0) shoreline = true;
                }
            }
            if (inBoundTrees > 0) treeHoles++;
            if (inBoundTrees >= 3) corridorHoles++;
            if (holeBunkers >= 3) groupedBunkerHoles++;
            if (shoreline) shorelineHoles++;
        }
        return new CourseEnvironmentalQualityReport(treesIn, treesOut, treeHoles, adjacentTrees, corridorHoles,
                fairwayBunkers, greensideBunkers, largeBunkers, groupedBunkerHoles, waterRegions, shorelineHoles,
                playableArea == 0.0 ? 0.0 : waterArea / playableArea);
    }

    private static boolean fairwayAdjacent(GeneratedHole hole, Position2d point) {
        if (hole.architecturePlan() == null) return false;
        HoleArchitecturePlan plan = hole.architecturePlan();
        double distance = plan.route().projectDistance(point);
        Position2d routePoint = plan.route().pointAt(distance);
        double fairway = plan.widthStations().stream()
                .min(java.util.Comparator.comparingDouble(station -> Math.abs(station.routeDistance() - distance)))
                .map(station -> Math.max(station.leftHalfWidth(), station.rightHalfWidth())).orElse(22.0);
        return point.distanceTo(routePoint) <= fairway + 26.0;
    }

    private static Position2d centroid(TerrainRegion region) {
        double x = region.boundary().stream().mapToDouble(Position2d::x).average().orElseThrow();
        double y = region.boundary().stream().mapToDouble(Position2d::y).average().orElseThrow();
        return new Position2d(x, y);
    }
}
