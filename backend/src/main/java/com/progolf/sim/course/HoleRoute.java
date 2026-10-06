package com.progolf.sim.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A deliberately small V3 route: tee, approach anchor, and at most one dogleg anchor. Distances and offsets
 * are measured along this polyline; it is planning data and never replaces canonical terrain polygons.
 */
public record HoleRoute(Position2d teeOrigin, List<Position2d> intermediateAnchors, Position2d approachAnchor) {
    private static final double MAX_TURN_RADIANS = StrictMath.toRadians(62.0);

    public HoleRoute {
        Objects.requireNonNull(teeOrigin, "teeOrigin");
        intermediateAnchors = List.copyOf(Objects.requireNonNull(intermediateAnchors, "intermediateAnchors"));
        Objects.requireNonNull(approachAnchor, "approachAnchor");
        if (intermediateAnchors.size() > 1) {
            throw new IllegalArgumentException("V3 supports at most one intermediate route anchor");
        }
        List<Position2d> points = points(teeOrigin, intermediateAnchors, approachAnchor);
        for (int i = 1; i < points.size(); i++) {
            double dx = points.get(i).x() - points.get(i - 1).x();
            double dy = points.get(i).y() - points.get(i - 1).y();
            if (points.get(i - 1).distanceTo(points.get(i)) < 20.0) {
                throw new IllegalArgumentException("route segments must be at least 20 yards");
            }
            if (dy <= 0.0) {
                throw new IllegalArgumentException("route anchors must progress toward the green");
            }
            if (Math.abs(dx) > dy * 0.65) {
                throw new IllegalArgumentException("route displacement exceeds V3 feasibility bound");
            }
        }
        if (intermediateAnchors.size() == 1 && turnRadians(points.get(0), points.get(1), points.get(2)) > MAX_TURN_RADIANS) {
            throw new IllegalArgumentException("route turn exceeds V3 severity bound");
        }
    }

    public List<Position2d> points() {
        return points(teeOrigin, intermediateAnchors, approachAnchor);
    }

    public double length() {
        List<Position2d> points = points();
        double result = 0.0;
        for (int i = 1; i < points.size(); i++) result += points.get(i - 1).distanceTo(points.get(i));
        return result;
    }

    /** Centreline point at clamped route distance, plus a signed golfer-right route offset. */
    public Position2d pointAt(double distance, double offset) {
        if (!Double.isFinite(distance) || !Double.isFinite(offset)) {
            throw new IllegalArgumentException("route coordinates must be finite");
        }
        Segment segment = segmentAt(distance);
        double local = Math.clamp(distance - segment.startDistance(), 0.0, segment.length());
        double progress = local / segment.length();
        Position2d center = new Position2d(segment.start().x() + (segment.end().x() - segment.start().x()) * progress,
                segment.start().y() + (segment.end().y() - segment.start().y()) * progress);
        return center.plus(segment.unitY() * offset, -segment.unitX() * offset);
    }

    /** Nearest along-route distance, used only to choose the next semantic progress target. */
    public double projectDistance(Position2d position) {
        Objects.requireNonNull(position, "position");
        double bestDistance = 0.0;
        double bestSquared = Double.POSITIVE_INFINITY;
        double accumulated = 0.0;
        List<Position2d> routePoints = points();
        for (int i = 1; i < routePoints.size(); i++) {
            Position2d start = routePoints.get(i - 1);
            Position2d end = routePoints.get(i);
            double dx = end.x() - start.x();
            double dy = end.y() - start.y();
            double lengthSquared = dx * dx + dy * dy;
            double t = Math.clamp(((position.x() - start.x()) * dx + (position.y() - start.y()) * dy) / lengthSquared,
                    0.0, 1.0);
            double px = start.x() + dx * t;
            double py = start.y() + dy * t;
            double squared = (position.x() - px) * (position.x() - px) + (position.y() - py) * (position.y() - py);
            if (squared < bestSquared) {
                bestSquared = squared;
                bestDistance = accumulated + StrictMath.sqrt(lengthSquared) * t;
            }
            accumulated += StrictMath.sqrt(lengthSquared);
        }
        return bestDistance;
    }

    public Position2d finalUnit() {
        Segment segment = segmentAt(length());
        return new Position2d(segment.unitX(), segment.unitY());
    }

    HoleRoute withLateralScale(double scale) {
        return new HoleRoute(scale(teeOrigin, scale), intermediateAnchors.stream().map(point -> scale(point, scale)).toList(),
                scale(approachAnchor, scale));
    }

    private Segment segmentAt(double distance) {
        List<Position2d> points = points();
        double clamped = Math.clamp(distance, 0.0, length());
        double accumulated = 0.0;
        for (int i = 1; i < points.size(); i++) {
            Position2d start = points.get(i - 1);
            Position2d end = points.get(i);
            double length = start.distanceTo(end);
            if (clamped <= accumulated + length || i == points.size() - 1) {
                return new Segment(start, end, accumulated, length, (end.x() - start.x()) / length,
                        (end.y() - start.y()) / length);
            }
            accumulated += length;
        }
        throw new IllegalStateException("route has no segments");
    }

    private static List<Position2d> points(Position2d tee, List<Position2d> intermediate, Position2d approach) {
        List<Position2d> result = new ArrayList<>(intermediate.size() + 2);
        result.add(tee);
        result.addAll(intermediate);
        result.add(approach);
        return List.copyOf(result);
    }

    private static double turnRadians(Position2d before, Position2d pivot, Position2d after) {
        double ax = pivot.x() - before.x();
        double ay = pivot.y() - before.y();
        double bx = after.x() - pivot.x();
        double by = after.y() - pivot.y();
        double cosine = (ax * bx + ay * by) / (StrictMath.hypot(ax, ay) * StrictMath.hypot(bx, by));
        return StrictMath.acos(Math.clamp(cosine, -1.0, 1.0));
    }

    private static Position2d scale(Position2d point, double scale) {
        return new Position2d(point.x() * scale, point.y());
    }

    private record Segment(Position2d start, Position2d end, double startDistance, double length,
                           double unitX, double unitY) {
    }
}
