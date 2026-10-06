package com.progolf.sim.course;

import com.progolf.sim.spatial.Surface;
import java.util.List;
import java.util.Objects;

/** One immutable, simple-polygon terrain region in canonical yard-space. */
public record TerrainRegion(Surface surface, List<Position2d> boundary) {
    public TerrainRegion {
        Objects.requireNonNull(surface, "surface");
        boundary = List.copyOf(Objects.requireNonNull(boundary, "boundary"));
        if (boundary.size() < 3) {
            throw new IllegalArgumentException("terrain boundary needs at least three vertices");
        }
        if (signedArea(boundary) <= 0.0) {
            throw new IllegalArgumentException("terrain boundary must be counter-clockwise");
        }
        if (!isSimplePolygon(boundary)) {
            throw new IllegalArgumentException("terrain boundary must be a simple polygon");
        }
    }

    static double signedArea(List<Position2d> points) {
        double area = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Position2d a = points.get(i);
            Position2d b = points.get((i + 1) % points.size());
            area += a.x() * b.y() - b.x() * a.y();
        }
        return area / 2.0;
    }

    boolean contains(Position2d point) {
        boolean inside = false;
        for (int i = 0, j = boundary.size() - 1; i < boundary.size(); j = i++) {
            Position2d a = boundary.get(i);
            Position2d b = boundary.get(j);
            boolean crosses = (a.y() > point.y()) != (b.y() > point.y());
            if (crosses && point.x() < (b.x() - a.x()) * (point.y() - a.y()) / (b.y() - a.y()) + a.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    static boolean isSimplePolygon(List<Position2d> polygon) {
        return simplePolygonViolation(polygon) == null;
    }

    static String simplePolygonViolation(List<Position2d> polygon) {
        int size = polygon.size();
        for (int i = 0; i < size; i++) {
            Position2d a = polygon.get(i);
            Position2d b = polygon.get((i + 1) % size);
            if (a.equals(b)) {
                return "repeated vertex at edge " + i;
            }
            for (int j = i + 1; j < size; j++) {
                // Adjacent edges share one endpoint by design, including the closing edge.
                if (j == i + 1 || (i == 0 && j == size - 1)) {
                    continue;
                }
                Position2d c = polygon.get(j);
                Position2d d = polygon.get((j + 1) % size);
                if (properlyIntersects(a, b, c, d)) {
                    return "edges " + i + " (" + a + " to " + b + ") and " + j + " (" + c + " to " + d + ")";
                }
            }
        }
        return null;
    }

    static boolean strictlyOverlaps(List<Position2d> first, List<Position2d> second) {
        for (int i = 0; i < first.size(); i++) {
            Position2d a = first.get(i);
            Position2d b = first.get((i + 1) % first.size());
            for (int j = 0; j < second.size(); j++) {
                Position2d c = second.get(j);
                Position2d d = second.get((j + 1) % second.size());
                if (properlyIntersects(a, b, c, d)) {
                    return true;
                }
            }
        }
        return containsStrictly(first, second.getFirst()) || containsStrictly(second, first.getFirst());
    }

    private static boolean containsStrictly(List<Position2d> polygon, Position2d point) {
        if (onBoundary(polygon, point)) {
            return false;
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

    private static boolean onBoundary(List<Position2d> polygon, Position2d point) {
        for (int i = 0; i < polygon.size(); i++) {
            if (onSegment(polygon.get(i), polygon.get((i + 1) % polygon.size()), point)) {
                return true;
            }
        }
        return false;
    }

    private static boolean onSegment(Position2d a, Position2d b, Position2d point) {
        return Math.abs(cross(a, b, point)) < 1.0e-9
                && point.x() >= Math.min(a.x(), b.x()) - 1.0e-9
                && point.x() <= Math.max(a.x(), b.x()) + 1.0e-9
                && point.y() >= Math.min(a.y(), b.y()) - 1.0e-9
                && point.y() <= Math.max(a.y(), b.y()) + 1.0e-9;
    }

    private static boolean properlyIntersects(Position2d a, Position2d b, Position2d c, Position2d d) {
        int abC = sign(cross(a, b, c));
        int abD = sign(cross(a, b, d));
        int cdA = sign(cross(c, d, a));
        int cdB = sign(cross(c, d, b));
        return abC != 0 && abD != 0 && cdA != 0 && cdB != 0 && abC != abD && cdA != cdB;
    }

    private static double cross(Position2d a, Position2d b, Position2d point) {
        return (b.x() - a.x()) * (point.y() - a.y()) - (b.y() - a.y()) * (point.x() - a.x());
    }

    private static int sign(double value) {
        return Double.compare(value, 0.0);
    }
}
