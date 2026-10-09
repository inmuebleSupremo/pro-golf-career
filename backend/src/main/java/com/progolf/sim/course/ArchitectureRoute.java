package com.progolf.sim.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** V5 connected centreline. It deliberately allows one additional turn beyond the retained V3 route. */
public record ArchitectureRoute(Position2d teeOrigin, List<Position2d> intermediateAnchors, Position2d greenCenter) {
    public ArchitectureRoute {
        Objects.requireNonNull(teeOrigin, "teeOrigin");
        intermediateAnchors = List.copyOf(Objects.requireNonNull(intermediateAnchors, "intermediateAnchors"));
        Objects.requireNonNull(greenCenter, "greenCenter");
        if (intermediateAnchors.size() > 2) throw new IllegalArgumentException("V5 permits at most two route turns");
        List<Position2d> points = points(teeOrigin, intermediateAnchors, greenCenter);
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i - 1).distanceTo(points.get(i)) < 45.0 || points.get(i).y() <= points.get(i - 1).y()) {
                throw new IllegalArgumentException("V5 route must progress through usable forward segments");
            }
        }
    }

    public List<Position2d> points() {
        return points(teeOrigin, intermediateAnchors, greenCenter);
    }

    private static List<Position2d> points(Position2d tee, List<Position2d> intermediate, Position2d green) {
        List<Position2d> result = new ArrayList<>(intermediate.size() + 2);
        result.add(tee);
        result.addAll(intermediate);
        result.add(green);
        return List.copyOf(result);
    }

    public double length() {
        List<Position2d> points = points();
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) total += points.get(i - 1).distanceTo(points.get(i));
        return total;
    }

    public Position2d pointAt(double distance) {
        List<Position2d> points = points();
        double remaining = Math.clamp(distance, 0.0, length());
        for (int i = 1; i < points.size(); i++) {
            Position2d start = points.get(i - 1);
            Position2d end = points.get(i);
            double segment = start.distanceTo(end);
            if (remaining <= segment || i == points.size() - 1) {
                double t = remaining / segment;
                return new Position2d(start.x() + (end.x() - start.x()) * t, start.y() + (end.y() - start.y()) * t);
            }
            remaining -= segment;
        }
        return greenCenter;
    }

    public double projectDistance(Position2d point) {
        double best = 0.0;
        double bestSquared = Double.POSITIVE_INFINITY;
        double accumulated = 0.0;
        List<Position2d> points = points();
        for (int i = 1; i < points.size(); i++) {
            Position2d start = points.get(i - 1);
            Position2d end = points.get(i);
            double dx = end.x() - start.x();
            double dy = end.y() - start.y();
            double squaredLength = dx * dx + dy * dy;
            double t = Math.clamp(((point.x() - start.x()) * dx + (point.y() - start.y()) * dy) / squaredLength, 0.0, 1.0);
            double px = start.x() + dx * t;
            double py = start.y() + dy * t;
            double squared = (point.x() - px) * (point.x() - px) + (point.y() - py) * (point.y() - py);
            if (squared < bestSquared) {
                bestSquared = squared;
                best = accumulated + StrictMath.sqrt(squaredLength) * t;
            }
            accumulated += StrictMath.sqrt(squaredLength);
        }
        return best;
    }
}
