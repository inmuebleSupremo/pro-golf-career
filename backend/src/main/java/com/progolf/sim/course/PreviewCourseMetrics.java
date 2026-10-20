package com.progolf.sim.course;

import com.progolf.sim.spatial.Surface;
import java.util.HashMap;
import java.util.Map;

/** Honest, development-only complete-course measures for the principles-driven architecture review. */
record PreviewCourseMetrics(int parFourFiveHoles, int straightforwardParFourFive, int meaningfulTurnHoles,
                            int doubleMeaningfulTurnHoles, int bunkerRegions, int groupedBunkerHoles,
                            int waterRegions, int treeRegions, int repeatedStructureHoles,
                            double minimumFairwayWidth, double meanFairwayWidth) {
    private static final double MEANINGFUL_TURN_DEGREES = 18.0;
    private static final double MINIMUM_CHORD_OFFSET = 24.0;

    static PreviewCourseMetrics inspect(Course course) {
        int parFourFive = 0, straightforward = 0, meaningful = 0, doubleTurns = 0;
        int bunkers = 0, groupedBunkers = 0, water = 0, trees = 0;
        double minWidth = Double.POSITIVE_INFINITY, totalWidth = 0.0;
        int stations = 0;
        Map<String, Integer> structures = new HashMap<>();
        for (GeneratedHole hole : course.holes()) {
            HoleArchitecturePlan plan = hole.architecturePlan();
            int turns = plan == null ? 0 : meaningfulTurns(plan.route(), hole.par());
            if (hole.par() >= 4) {
                parFourFive++;
                if (turns == 0) straightforward++;
                if (turns > 0) meaningful++;
                if (turns > 1) doubleTurns++;
            }
            int holeBunkers = 0;
            for (TerrainRegion region : hole.geometry().regions()) {
                if (region.surface() == Surface.BUNKER) {
                    bunkers++;
                    holeBunkers++;
                } else if (region.surface() == Surface.WATER) water++;
                else if (region.surface() == Surface.TREES) trees++;
            }
            if (holeBunkers >= 3) groupedBunkers++;
            if (plan != null) {
                double averageWidth = plan.widthStations().stream()
                        .mapToDouble(station -> station.leftHalfWidth() + station.rightHalfWidth()).average().orElse(0.0);
                structures.merge(hole.par() + ":" + turns + ":" + Math.round(routeChord(plan.route()) * 20.0)
                        + ":" + Math.round(averageWidth / 4.0), 1, Integer::sum);
                for (FairwayWidthStation station : plan.widthStations()) {
                    double width = station.leftHalfWidth() + station.rightHalfWidth();
                    minWidth = Math.min(minWidth, width);
                    totalWidth += width;
                    stations++;
                }
            }
        }
        int repeated = structures.values().stream().mapToInt(count -> Math.max(0, count - 1)).sum();
        return new PreviewCourseMetrics(parFourFive, straightforward, meaningful, doubleTurns, bunkers, groupedBunkers,
                water, trees, repeated, stations == 0 ? 0.0 : minWidth, stations == 0 ? 0.0 : totalWidth / stations);
    }

    static int meaningfulTurns(ArchitectureRoute route, int par) {
        if (par < 4) return 0;
        int result = 0;
        double travelled = 0.0;
        var points = route.points();
        double chordLength = route.teeOrigin().distanceTo(route.greenCenter());
        for (int index = 1; index < points.size() - 1; index++) {
            travelled += points.get(index - 1).distanceTo(points.get(index));
            if (meaningfulTurn(route, par, index, travelled, chordLength)) result++;
        }
        return result;
    }

    static boolean meaningfulTurn(ArchitectureRoute route, int par, int pointIndex) {
        if (par < 4 || pointIndex <= 0 || pointIndex >= route.points().size() - 1) return false;
        double travelled = 0.0;
        var points = route.points();
        for (int index = 1; index <= pointIndex; index++) travelled += points.get(index - 1).distanceTo(points.get(index));
        return meaningfulTurn(route, par, pointIndex, travelled, route.teeOrigin().distanceTo(route.greenCenter()));
    }

    private static boolean meaningfulTurn(ArchitectureRoute route, int par, int pointIndex, double travelled,
                                          double chordLength) {
        if (par < 4) return false;
        var points = route.points();
        Position2d before = points.get(pointIndex - 1);
        Position2d turn = points.get(pointIndex);
        Position2d after = points.get(pointIndex + 1);
        double remaining = route.length() - travelled;
        return travelled >= 130.0 && remaining >= 95.0
                && turnAngleDegrees(before, turn, after) >= MEANINGFUL_TURN_DEGREES
                && distanceFromChord(turn, route.teeOrigin(), route.greenCenter(), chordLength) >= MINIMUM_CHORD_OFFSET;
    }

    private static double turnAngleDegrees(Position2d before, Position2d turn, Position2d after) {
        double ax = turn.x() - before.x(), ay = turn.y() - before.y();
        double bx = after.x() - turn.x(), by = after.y() - turn.y();
        double dot = (ax * bx + ay * by) / (StrictMath.hypot(ax, ay) * StrictMath.hypot(bx, by));
        return StrictMath.toDegrees(StrictMath.acos(Math.clamp(dot, -1.0, 1.0)));
    }

    private static double distanceFromChord(Position2d point, Position2d tee, Position2d green, double chordLength) {
        return StrictMath.abs((green.x() - tee.x()) * (tee.y() - point.y())
                - (tee.x() - point.x()) * (green.y() - tee.y())) / chordLength;
    }

    private static double routeChord(ArchitectureRoute route) {
        return route.length() / route.teeOrigin().distanceTo(route.greenCenter());
    }
}
