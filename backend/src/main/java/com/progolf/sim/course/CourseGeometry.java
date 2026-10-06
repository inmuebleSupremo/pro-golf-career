package com.progolf.sim.course;

import com.progolf.sim.spatial.Surface;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative terrain for one generated hole. Coordinates are local yards: tee at (0,0), positive y
 * toward the green, and positive x golfer-right. The region list is presentation-safe but lookup is always
 * resolved by the central precedence table below.
 */
public record CourseGeometry(Position2d tee, Position2d greenCenter, List<Position2d> playableBoundary,
                             List<TerrainRegion> regions) {
    /**
     * Round setups are shared by every competitor in an event. Cache their immutable coordinate variants
     * so a 150-player field does not rebuild the same polygons for every hole and round.
     */
    private static final Map<LateralScaleKey, CourseGeometry> LATERAL_VARIANTS = new ConcurrentHashMap<>();

    public CourseGeometry {
        Objects.requireNonNull(tee, "tee");
        Objects.requireNonNull(greenCenter, "greenCenter");
        playableBoundary = List.copyOf(Objects.requireNonNull(playableBoundary, "playableBoundary"));
        regions = List.copyOf(Objects.requireNonNull(regions, "regions"));
        double boundaryArea = TerrainRegion.signedArea(playableBoundary);
        boolean simpleBoundary = TerrainRegion.isSimplePolygon(playableBoundary);
        if (playableBoundary.size() < 3 || boundaryArea <= 0.0 || !simpleBoundary) {
            throw new IllegalArgumentException("playable boundary must be a counter-clockwise simple polygon"
                    + " (area=" + boundaryArea + ", simple=" + simpleBoundary + ", violation="
                    + TerrainRegion.simplePolygonViolation(playableBoundary) + ")");
        }
        validateUnambiguousPrecedence(regions);
    }

    public Surface surfaceAt(Position2d position) {
        Objects.requireNonNull(position, "position");
        if (!contains(playableBoundary, position)) {
            return Surface.OUT_OF_BOUNDS;
        }
        return regions.stream()
                .filter(region -> region.contains(position))
                .min(Comparator.comparingInt(region -> precedence(region.surface())))
                .map(TerrainRegion::surface)
                .orElse(Surface.PRIMARY_ROUGH);
    }

    private static void validateUnambiguousPrecedence(List<TerrainRegion> regions) {
        for (int i = 0; i < regions.size(); i++) {
            TerrainRegion first = regions.get(i);
            for (int j = i + 1; j < regions.size(); j++) {
                TerrainRegion second = regions.get(j);
                if (first.surface() != second.surface() && precedence(first.surface()) == precedence(second.surface())
                        && TerrainRegion.strictlyOverlaps(first.boundary(), second.boundary())) {
                    throw new IllegalArgumentException("ambiguous terrain overlap at precedence "
                            + precedence(first.surface()) + ": " + first.surface() + " and " + second.surface());
                }
            }
        }
    }

    public static int precedence(Surface surface) {
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

    /**
     * Returns an immutable lateral-width variant for an existing course setup. The local y axis,
     * tee-to-green progression, and terrain ordering stay unchanged; every x coordinate scales about
     * the course centre line. This keeps the pre-canonical setup width difficulty live once surface
     * resolution reads polygons rather than {@code HoleZones}.
     */
    public CourseGeometry withLateralScale(double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException("lateral scale must be finite and > 0: " + scale);
        }
        if (scale == 1.0) {
            return this;
        }
        return LATERAL_VARIANTS.computeIfAbsent(new LateralScaleKey(this, scale), ignored -> scaled(scale));
    }

    private CourseGeometry scaled(double scale) {
        return new CourseGeometry(scale(tee, scale), scale(greenCenter, scale),
                playableBoundary.stream().map(point -> scale(point, scale)).toList(),
                regions.stream().map(region -> new TerrainRegion(region.surface(),
                        region.boundary().stream().map(point -> scale(point, scale)).toList())).toList());
    }

    private static boolean contains(List<Position2d> polygon, Position2d point) {
        for (int i = 0; i < polygon.size(); i++) {
            if (onSegment(polygon.get(i), polygon.get((i + 1) % polygon.size()), point)) {
                return true;
            }
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

    private static boolean onSegment(Position2d a, Position2d b, Position2d point) {
        double cross = (b.x() - a.x()) * (point.y() - a.y())
                - (b.y() - a.y()) * (point.x() - a.x());
        return Math.abs(cross) < 1.0e-9
                && point.x() >= Math.min(a.x(), b.x()) - 1.0e-9
                && point.x() <= Math.max(a.x(), b.x()) + 1.0e-9
                && point.y() >= Math.min(a.y(), b.y()) - 1.0e-9
                && point.y() <= Math.max(a.y(), b.y()) + 1.0e-9;
    }

    private static Position2d scale(Position2d point, double scale) {
        return new Position2d(point.x() * scale, point.y());
    }

    private record LateralScaleKey(CourseGeometry geometry, double scale) {
    }
}
