package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.List;

/** Bounded deterministic course-scale geography and placement selection for retained V6. */
final class V6LandscapePlanner {
    private static final long LANDSCAPE_SALT = 0x56364c414e4453L;
    private static final double MIN_LANE_GAP = 360.0;
    private static final double MIN_ROW_GAP = 790.0;

    private V6LandscapePlanner() { }

    static CourseLandscapePlan plan(long courseSeed, EnvironmentClassification environment, List<Double> lengths) {
        if (lengths.size() != 18) throw new IllegalArgumentException("V6 requires 18 lengths");
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(courseSeed, LANDSCAPE_SALT));
        double laneGap = MIN_LANE_GAP + rng.nextDouble() * 40.0;
        double rowGap = MIN_ROW_GAP + rng.nextDouble() * 85.0;
        List<HolePlacement> placements = serpentinePlacements(lengths, laneGap, rowGap, environment, rng);
        boolean valid = valid(placements);
        if (!valid) placements = fallbackPlacements(lengths, environment);
        List<LandscapeFeature> features = features(environment, laneGap, rowGap, rng);
        return new CourseLandscapePlan("v6-" + Long.toUnsignedString(courseSeed, 36), environment, features,
                placements, !valid);
    }

    static LandscapeHoleContext context(CourseLandscapePlan plan, int holeNumber) {
        HolePlacement placement = plan.placement(holeNumber);
        double extent = placement.envelopeLength();
        List<LandscapeContextFeature> local = plan.features().stream().map(feature -> {
            List<Position2d> projected = feature.boundary().stream().map(placement.transform()::toLocal).toList();
            return new LandscapeContextFeature(feature.id(), feature.kind(), clip(projected, -260.0, 260.0, -140.0, extent + 180.0));
        }).filter(feature -> feature.boundary().size() >= 3).toList();
        return new LandscapeHoleContext(plan.identity(), placement.relationship(), local);
    }

    static LandscapeHoleCondition condition(CourseLandscapePlan plan, int holeNumber) {
        return LandscapeHoleCondition.from(plan.placement(holeNumber).relationship());
    }

    private static List<HolePlacement> serpentinePlacements(List<Double> lengths, double laneGap, double rowGap,
                                                              EnvironmentClassification environment, Rng rng) {
        List<HolePlacement> result = new ArrayList<>(18);
        Position2d laneStart = new Position2d(0.0, 0.0);
        for (int lane = 0; lane < 3; lane++) {
            boolean north = lane % 2 == 0;
            Position2d origin = laneStart;
            for (int row = 0; row < 6; row++) {
                int number = lane * 6 + row + 1;
                int index = number - 1;
                double heading = (north ? 0.0 : StrictMath.PI) + (rng.nextDouble() - .5) * .12;
                LandscapeRelationship relationship = relationship(environment, lane, row);
                HolePlacement placement = new HolePlacement(number, new CourseCoordinateTransform(origin, heading), 104.0,
                        lengths.get(index) + 118.0, 0.0, relationship);
                result.add(placement);
                Position2d next = placement.transform().toCourse(new Position2d((rng.nextDouble() - .5) * 26.0,
                        placement.envelopeLength() - 70.0 + 150.0 + rng.nextDouble() * 24.0));
                origin = next;
            }
            HolePlacement last = result.getLast();
            double transfer = laneGap;
            laneStart = last.greenEstimate().plus(transfer, (rng.nextDouble() - .5) * 45.0);
        }
        return withTransitions(result);
    }

    /** Explicit bounded fallback; it remains one shared serpentine course plan, never isolated hole diagrams. */
    private static List<HolePlacement> fallbackPlacements(List<Double> lengths, EnvironmentClassification environment) {
        return serpentinePlacements(lengths, 400.0, 900.0, environment,
                new SplitMix64Rng(Seeds.deriveSeed(lengths.getFirst().longValue(), LANDSCAPE_SALT)));
    }

    private static List<HolePlacement> withTransitions(List<HolePlacement> input) {
        List<HolePlacement> result = new ArrayList<>(input.size());
        for (int i = 0; i < input.size(); i++) {
            HolePlacement placement = input.get(i);
            double transition = i == input.size() - 1 ? 0.0
                    : placement.greenEstimate().distanceTo(input.get(i + 1).transform().origin());
            result.add(new HolePlacement(placement.holeNumber(), placement.transform(), placement.envelopeHalfWidth(),
                    placement.envelopeLength(), transition, placement.relationship()));
        }
        return List.copyOf(result);
    }

    private static boolean valid(List<HolePlacement> placements) {
        for (int i = 0; i < placements.size(); i++) {
            HolePlacement first = placements.get(i);
            if (first.transitionToNextTee() > 420.0) return false;
            for (int j = i + 1; j < placements.size(); j++) {
                if (overlaps(first, placements.get(j))) return false;
            }
        }
        return true;
    }

    private static boolean overlaps(HolePlacement a, HolePlacement b) {
        double[] aBounds = bounds(a);
        double[] bBounds = bounds(b);
        double aMinX = aBounds[0], aMaxX = aBounds[1], aMinY = aBounds[2], aMaxY = aBounds[3];
        double bMinX = bBounds[0], bMaxX = bBounds[1], bMinY = bBounds[2], bMaxY = bBounds[3];
        return aMinX < bMaxX && aMaxX > bMinX && aMinY < bMaxY && aMaxY > bMinY;
    }

    private static double[] bounds(HolePlacement placement) {
        List<Position2d> corners = List.of(new Position2d(-placement.envelopeHalfWidth(), -50.0),
                new Position2d(placement.envelopeHalfWidth(), -50.0),
                new Position2d(placement.envelopeHalfWidth(), placement.envelopeLength() - 20.0),
                new Position2d(-placement.envelopeHalfWidth(), placement.envelopeLength() - 20.0));
        List<Position2d> points = corners.stream().map(placement.transform()::toCourse).toList();
        return new double[] {points.stream().mapToDouble(Position2d::x).min().orElseThrow(),
                points.stream().mapToDouble(Position2d::x).max().orElseThrow(),
                points.stream().mapToDouble(Position2d::y).min().orElseThrow(),
                points.stream().mapToDouble(Position2d::y).max().orElseThrow()};
    }

    private static LandscapeRelationship relationship(EnvironmentClassification environment, int lane, int row) {
        return switch (environment) {
            case COASTAL -> lane == 0 ? (row % 3 == 1 ? LandscapeRelationship.BAY_APPROACH
                    : LandscapeRelationship.SHORELINE_RUN) : lane == 1 ? LandscapeRelationship.INLAND_TURN
                    : LandscapeRelationship.OPEN_GROUND;
            case LINKS -> lane == 0 ? LandscapeRelationship.SHORELINE_RUN : row % 2 == 0
                    ? LandscapeRelationship.DUNE_EDGE : LandscapeRelationship.LINKS_EXPOSED;
            case WOODLAND -> row % 3 == 1 ? LandscapeRelationship.CLEARING_LANDING
                    : LandscapeRelationship.WOODLAND_CORRIDOR;
            case PARKLAND -> row % 3 == 1 ? LandscapeRelationship.COPSE_EDGE : LandscapeRelationship.PARKLAND_FIELD;
            default -> LandscapeRelationship.OPEN_GROUND;
        };
    }

    private static List<LandscapeFeature> features(EnvironmentClassification environment, double laneGap, double rowGap,
                                                    Rng rng) {
        double height = rowGap * 5.0 + 700.0;
        List<LandscapeFeature> result = new ArrayList<>();
        switch (environment) {
            case COASTAL -> {
                result.add(rectangle("coast", LandscapeFeatureKind.COAST_WATER, -860.0, -126.0, -300.0, height));
                result.add(rectangle("bay", LandscapeFeatureKind.BAY_WATER, -300.0, -126.0, rowGap * 1.45, rowGap * 3.1));
            }
            case LINKS -> {
                result.add(rectangle("shore", LandscapeFeatureKind.COAST_WATER, -820.0, -128.0, -250.0, height));
                result.add(rectangle("dunes", LandscapeFeatureKind.DUNE_BAND, -120.0, 145.0, -120.0, height));
                result.add(rectangle("heath", LandscapeFeatureKind.LINKS_HEATH, laneGap * 2.0 + 130.0,
                        laneGap * 2.0 + 480.0, -120.0, height));
            }
            case WOODLAND -> {
                result.add(rectangle("west-wood", LandscapeFeatureKind.WOODLAND_MASS, -610.0, -128.0, -180.0, height));
                result.add(rectangle("east-wood", LandscapeFeatureKind.WOODLAND_MASS, laneGap * 2.0 + 128.0,
                        laneGap * 2.0 + 620.0, -180.0, height));
                result.add(rectangle("clearing", LandscapeFeatureKind.CLEARING, laneGap - 115.0, laneGap + 115.0,
                        rowGap * 1.55, rowGap * 2.45));
            }
            case PARKLAND -> {
                result.add(rectangle("north-field", LandscapeFeatureKind.PARKLAND_FIELD, -260.0, laneGap * 2.0 + 280.0,
                        rowGap * 3.0, height));
                result.add(rectangle("west-copse", LandscapeFeatureKind.COPSE, -350.0, -130.0, rowGap * .55, rowGap * 1.7));
                result.add(rectangle("east-copse", LandscapeFeatureKind.COPSE, laneGap * 2.0 + 126.0,
                        laneGap * 2.0 + 330.0, rowGap * 2.5, rowGap * 4.25));
            }
            default -> result.add(rectangle("open-ground", LandscapeFeatureKind.PARKLAND_FIELD, -250.0,
                    laneGap * 2.0 + 260.0, -160.0, height));
        }
        if (environment != EnvironmentClassification.COASTAL && environment != EnvironmentClassification.LINKS
                && rng.nextDouble() < .34) {
            result.add(rectangle("lake", LandscapeFeatureKind.LAKE, laneGap * 2.0 + 145.0,
                    laneGap * 2.0 + 330.0, rowGap * 4.1, rowGap * 4.7));
        }
        return List.copyOf(result);
    }

    private static LandscapeFeature rectangle(String id, LandscapeFeatureKind kind, double minX, double maxX,
                                              double minY, double maxY) {
        return new LandscapeFeature(id, kind, List.of(new Position2d(minX, minY), new Position2d(maxX, minY),
                new Position2d(maxX, maxY), new Position2d(minX, maxY)));
    }

    /** Small deterministic viewport clip for context projection, not a gameplay polygon operation. */
    private static List<Position2d> clip(List<Position2d> source, double minX, double maxX, double minY, double maxY) {
        List<Position2d> result = source;
        result = clipEdge(result, point -> point.x() >= minX, (a, b) -> intersectX(a, b, minX));
        result = clipEdge(result, point -> point.x() <= maxX, (a, b) -> intersectX(a, b, maxX));
        result = clipEdge(result, point -> point.y() >= minY, (a, b) -> intersectY(a, b, minY));
        return List.copyOf(clipEdge(result, point -> point.y() <= maxY, (a, b) -> intersectY(a, b, maxY)));
    }

    private static List<Position2d> clipEdge(List<Position2d> input, java.util.function.Predicate<Position2d> inside,
                                              Intersection intersection) {
        if (input.isEmpty()) return List.of();
        List<Position2d> result = new ArrayList<>();
        Position2d previous = input.getLast();
        boolean previousInside = inside.test(previous);
        for (Position2d current : input) {
            boolean currentInside = inside.test(current);
            if (currentInside != previousInside) result.add(intersection.at(previous, current));
            if (currentInside) result.add(current);
            previous = current;
            previousInside = currentInside;
        }
        return result;
    }

    private static Position2d intersectX(Position2d a, Position2d b, double x) {
        double t = (x - a.x()) / (b.x() - a.x());
        return new Position2d(x, a.y() + (b.y() - a.y()) * t);
    }

    private static Position2d intersectY(Position2d a, Position2d b, double y) {
        double t = (y - a.y()) / (b.y() - a.y());
        return new Position2d(a.x() + (b.x() - a.x()) * t, y);
    }

    @FunctionalInterface
    private interface Intersection {
        Position2d at(Position2d a, Position2d b);
    }
}
