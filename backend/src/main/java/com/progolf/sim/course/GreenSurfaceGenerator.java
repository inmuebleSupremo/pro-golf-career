package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a deterministic, context-sensitive physical surface from existing canonical green geometry.
 * It is deliberately on-demand: introducing a surface cannot alter retained course generation, saves,
 * pin placement, or the current probability-based putting resolver.
 */
public final class GreenSurfaceGenerator {
    private static final long SURFACE_SEED_SALT = 0x475245454e535552L;
    private static final double TARGET_GRID_SPACING_YARDS = 1.25;
    private static final int MIN_GRID_SAMPLES = 17;
    private static final int MAX_GRID_SAMPLES = 65;
    /** 4.5% is a demanding but credible upper bound for the initial generated corpus. */
    public static final double MAXIMUM_SLOPE = 0.045;

    private GreenSurfaceGenerator() {
    }

    public static GreenSurface generate(GeneratedHole hole, GreenSurfaceVersion version) {
        Objects.requireNonNull(hole, "hole");
        Position2d approach = approachDirection(hole);
        return generate(hole.geometry(), hole.holeSeed(), approach, version);
    }

    /** Explicit seam for future event policies that select a surface version and an effective green geometry. */
    public static GreenSurface generate(CourseGeometry geometry, long holeSeed, Position2d approachDirection,
                                        GreenSurfaceVersion version) {
        Objects.requireNonNull(geometry, "geometry");
        Objects.requireNonNull(approachDirection, "approachDirection");
        Objects.requireNonNull(version, "version");
        List<Position2d> footprint = greenFootprint(geometry);
        Position2d approach = normalise(approachDirection);
        long surfaceSeed = Seeds.deriveSeed(holeSeed, SURFACE_SEED_SALT + version.number());
        Rng rng = new SplitMix64Rng(surfaceSeed);
        Bounds bounds = Bounds.of(footprint);
        int columns = samplesFor(bounds.maxX - bounds.minX);
        int rows = samplesFor(bounds.maxY - bounds.minY);
        Position2d center = geometry.greenCenter();
        SurfaceShape shape = shape(rng, footprint, bounds, center, approach);
        double[] elevations = new double[columns * rows];
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            double x = bounds.minX + (bounds.maxX - bounds.minX) * column / (columns - 1.0);
            double y = bounds.minY + (bounds.maxY - bounds.minY) * row / (rows - 1.0);
            elevations[row * columns + column] = shape.elevationAt(new Position2d(x, y));
        }
        GreenSurfaceProvenance provenance = new GreenSurfaceProvenance(version, holeSeed, surfaceSeed,
                fingerprint(footprint), approach);
        GreenSurface surface = new GreenSurface(provenance, footprint, bounds.minX, bounds.maxX, bounds.minY, bounds.maxY,
                columns, rows, elevations);
        return limitSlope(surface, center);
    }

    private static GreenSurface limitSlope(GreenSurface surface, Position2d center) {
        double maximumSlope = surface.maximumSlope();
        if (maximumSlope <= MAXIMUM_SLOPE) return surface;
        double centerElevation = surface.elevationAt(center);
        double scale = MAXIMUM_SLOPE / maximumSlope;
        double[] limited = new double[surface.columns() * surface.rows()];
        for (int row = 0; row < surface.rows(); row++) for (int column = 0; column < surface.columns(); column++) {
            int index = row * surface.columns() + column;
            limited[index] = centerElevation + (surface.gridElevation(column, row) - centerElevation) * scale;
        }
        Bounds bounds = Bounds.of(surface.footprint());
        return new GreenSurface(surface.provenance(), surface.footprint(), bounds.minX, bounds.maxX, bounds.minY, bounds.maxY,
                surface.columns(), surface.rows(), limited);
    }

    private static SurfaceShape shape(Rng rng, List<Position2d> footprint, Bounds bounds, Position2d center,
                                      Position2d approach) {
        double approachGrade = (rng.nextDouble() * 2.0 - 1.0) * 0.017;
        double crossGrade = (rng.nextDouble() * 2.0 - 1.0) * 0.014;
        int featureCount = rng.nextDouble() < 0.30 ? 0 : 1 + (int) StrictMath.floor(rng.nextDouble() * 3.0);
        List<ContourFeature> features = new ArrayList<>(featureCount);
        for (int index = 0; index < featureCount; index++) {
            Position2d featureCenter = pointInside(footprint, bounds, rng);
            // Keep the centre broadly readable rather than creating a conspicuous random pimple at its heart.
            if (featureCenter.distanceTo(center) < 3.0) featureCenter = pointInside(footprint, bounds, rng);
            double amplitude = (0.045 + rng.nextDouble() * 0.105) * (rng.nextDouble() < 0.52 ? 1.0 : -1.0);
            double major = 5.0 + rng.nextDouble() * 7.0;
            double minor = 3.5 + rng.nextDouble() * 5.5;
            double rotation = rng.nextDouble() * 2.0 * StrictMath.PI;
            features.add(new ContourFeature(featureCenter, amplitude, major, minor, rotation));
        }
        return new SurfaceShape(center, approach, approachGrade, crossGrade, features);
    }

    private static Position2d pointInside(List<Position2d> footprint, Bounds bounds, Rng rng) {
        for (int attempt = 0; attempt < 48; attempt++) {
            Position2d candidate = new Position2d(bounds.minX + rng.nextDouble() * (bounds.maxX - bounds.minX),
                    bounds.minY + rng.nextDouble() * (bounds.maxY - bounds.minY));
            if (contains(footprint, candidate)) return candidate;
        }
        double x = 0.0;
        double y = 0.0;
        for (Position2d point : footprint) {
            x += point.x();
            y += point.y();
        }
        return new Position2d(x / footprint.size(), y / footprint.size());
    }

    private static List<Position2d> greenFootprint(CourseGeometry geometry) {
        return geometry.regions().stream().filter(region -> region.surface() == Surface.GREEN)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("canonical geometry has no green region")).boundary();
    }

    private static Position2d approachDirection(GeneratedHole hole) {
        if (hole.spatialPlan() != null) return normalise(hole.spatialPlan().greenComplex().approachDirection());
        Position2d tee = hole.geometry().tee();
        Position2d green = hole.geometry().greenCenter();
        return normalise(new Position2d(green.x() - tee.x(), green.y() - tee.y()));
    }

    private static Position2d normalise(Position2d vector) {
        double magnitude = StrictMath.hypot(vector.x(), vector.y());
        if (magnitude < 1.0e-9) throw new IllegalArgumentException("green approach direction must be non-zero");
        return new Position2d(vector.x() / magnitude, vector.y() / magnitude);
    }

    private static int samplesFor(double span) {
        return Math.clamp((int) StrictMath.ceil(span / TARGET_GRID_SPACING_YARDS) + 1, MIN_GRID_SAMPLES, MAX_GRID_SAMPLES);
    }

    private static long fingerprint(List<Position2d> footprint) {
        long hash = 0x475245454e46494eL;
        for (Position2d point : footprint) {
            hash = Seeds.deriveSeed(hash, Double.doubleToLongBits(point.x()));
            hash = Seeds.deriveSeed(hash, Double.doubleToLongBits(point.y()));
        }
        return hash;
    }

    private static boolean contains(List<Position2d> polygon, Position2d point) {
        boolean inside = false;
        for (int index = 0, previous = polygon.size() - 1; index < polygon.size(); previous = index++) {
            Position2d a = polygon.get(index);
            Position2d b = polygon.get(previous);
            if ((a.y() > point.y()) != (b.y() > point.y())
                    && point.x() < (b.x() - a.x()) * (point.y() - a.y()) / (b.y() - a.y()) + a.x()) inside = !inside;
        }
        return inside;
    }

    private record SurfaceShape(Position2d center, Position2d approach, double approachGrade, double crossGrade,
                                List<ContourFeature> features) {
        double elevationAt(Position2d point) {
            double dx = point.x() - center.x();
            double dy = point.y() - center.y();
            Position2d cross = new Position2d(approach.y(), -approach.x());
            double elevation = approachGrade * (dx * approach.x() + dy * approach.y())
                    + crossGrade * (dx * cross.x() + dy * cross.y());
            for (ContourFeature feature : features) elevation += feature.elevationAt(point);
            return elevation;
        }
    }

    private record ContourFeature(Position2d center, double amplitude, double major, double minor, double rotation) {
        double elevationAt(Position2d point) {
            double dx = point.x() - center.x();
            double dy = point.y() - center.y();
            double along = dx * StrictMath.cos(rotation) + dy * StrictMath.sin(rotation);
            double across = -dx * StrictMath.sin(rotation) + dy * StrictMath.cos(rotation);
            return amplitude * StrictMath.exp(-0.5 * (along * along / (major * major) + across * across / (minor * minor)));
        }
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
        static Bounds of(List<Position2d> footprint) {
            return new Bounds(footprint.stream().mapToDouble(Position2d::x).min().orElseThrow(),
                    footprint.stream().mapToDouble(Position2d::x).max().orElseThrow(),
                    footprint.stream().mapToDouble(Position2d::y).min().orElseThrow(),
                    footprint.stream().mapToDouble(Position2d::y).max().orElseThrow());
        }
    }
}
