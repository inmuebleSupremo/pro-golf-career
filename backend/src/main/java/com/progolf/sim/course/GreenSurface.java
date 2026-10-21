package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/**
 * Immutable regular height field for one canonical green footprint. Heights are relative yards and are
 * reconstructed from {@link #provenance()} rather than persisted in a save. Queries use bilinear
 * interpolation, so elevation is continuous over the entire authoritative green polygon.
 */
public final class GreenSurface {
    /** Existing fringe/collar geometry is three or more yards wide; queries just beyond the green use edge data. */
    public static final double COLLAR_QUERY_WIDTH_YARDS = 3.0;

    private final GreenSurfaceProvenance provenance;
    private final List<Position2d> footprint;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final int columns;
    private final int rows;
    private final double[] elevations;

    GreenSurface(GreenSurfaceProvenance provenance, List<Position2d> footprint, double minX, double maxX,
                 double minY, double maxY, int columns, int rows, double[] elevations) {
        this.provenance = Objects.requireNonNull(provenance, "provenance");
        this.footprint = List.copyOf(Objects.requireNonNull(footprint, "footprint"));
        if (this.footprint.size() < 3 || !Double.isFinite(minX) || !Double.isFinite(maxX)
                || !Double.isFinite(minY) || !Double.isFinite(maxY) || minX >= maxX || minY >= maxY
                || columns < 2 || rows < 2 || elevations == null || elevations.length != columns * rows) {
            throw new IllegalArgumentException("green surface grid and footprint must be valid");
        }
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.columns = columns;
        this.rows = rows;
        this.elevations = elevations.clone();
        for (double elevation : this.elevations) {
            if (!Double.isFinite(elevation)) throw new IllegalArgumentException("green surface elevations must be finite");
        }
    }

    public GreenSurfaceProvenance provenance() {
        return provenance;
    }

    public GreenSurfaceVersion version() {
        return provenance.version();
    }

    /** The exact canonical GREEN polygon that bounds this physical surface. */
    public List<Position2d> footprint() {
        return footprint;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /** Grid vertex location for diagnostics and future authoritative rendering. */
    public Position2d gridPoint(int column, int row) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("green-surface grid coordinate out of range");
        }
        return new Position2d(minX + column * cellWidth(), minY + row * cellHeight());
    }

    /** Raw authoritative grid sample for diagnostics; ordinary simulation should query continuously. */
    public double gridElevation(int column, int row) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("green-surface grid coordinate out of range");
        }
        return elevations[row * columns + column];
    }

    public boolean contains(Position2d position) {
        Objects.requireNonNull(position, "position");
        return contains(footprint, position);
    }

    public double elevationAt(Position2d position) {
        return sampleAt(position).elevation();
    }

    public GreenSurfaceGradient gradientAt(Position2d position) {
        return sampleAt(position).gradient();
    }

    /**
     * Samples the field at any finite location. On-green points interpolate directly; collar and exterior
     * points use the closest canonical green edge, preventing an invented surface beyond the green.
     */
    public GreenSurfaceSample sampleAt(Position2d position) {
        Objects.requireNonNull(position, "position");
        if (contains(footprint, position)) return interpolate(position, GreenSurfaceLocation.ON_GREEN);
        Position2d edge = nearestPointOnBoundary(position);
        GreenSurfaceLocation location = position.distanceTo(edge) <= COLLAR_QUERY_WIDTH_YARDS
                ? GreenSurfaceLocation.COLLAR : GreenSurfaceLocation.OUTSIDE;
        return interpolate(edge, location);
    }

    /** Largest gradient magnitude over the regular field; useful for generation quality checks. */
    public double maximumSlope() {
        double maximum = 0.0;
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            maximum = Math.max(maximum, gradientAt(gridPoint(column, row)).magnitude());
        }
        return maximum;
    }

    public double minimumElevation() {
        double minimum = Double.POSITIVE_INFINITY;
        for (double elevation : elevations) minimum = Math.min(minimum, elevation);
        return minimum;
    }

    public double maximumElevation() {
        double maximum = Double.NEGATIVE_INFINITY;
        for (double elevation : elevations) maximum = Math.max(maximum, elevation);
        return maximum;
    }

    private GreenSurfaceSample interpolate(Position2d position, GreenSurfaceLocation location) {
        double x = Math.clamp(position.x(), minX, maxX);
        double y = Math.clamp(position.y(), minY, maxY);
        double gridX = (x - minX) / cellWidth();
        double gridY = (y - minY) / cellHeight();
        int column = Math.min(columns - 2, (int) StrictMath.floor(gridX));
        int row = Math.min(rows - 2, (int) StrictMath.floor(gridY));
        double xFraction = Math.clamp(gridX - column, 0.0, 1.0);
        double yFraction = Math.clamp(gridY - row, 0.0, 1.0);
        double lowerLeft = gridElevation(column, row);
        double lowerRight = gridElevation(column + 1, row);
        double upperLeft = gridElevation(column, row + 1);
        double upperRight = gridElevation(column + 1, row + 1);
        double lower = lowerLeft + (lowerRight - lowerLeft) * xFraction;
        double upper = upperLeft + (upperRight - upperLeft) * xFraction;
        double elevation = lower + (upper - lower) * yFraction;
        double gradientX = ((lowerRight - lowerLeft) * (1.0 - yFraction)
                + (upperRight - upperLeft) * yFraction) / cellWidth();
        double gradientY = ((upperLeft - lowerLeft) * (1.0 - xFraction)
                + (upperRight - lowerRight) * xFraction) / cellHeight();
        return new GreenSurfaceSample(elevation, new GreenSurfaceGradient(gradientX, gradientY), location);
    }

    private double cellWidth() {
        return (maxX - minX) / (columns - 1);
    }

    private double cellHeight() {
        return (maxY - minY) / (rows - 1);
    }

    private Position2d nearestPointOnBoundary(Position2d position) {
        Position2d nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (int i = 0; i < footprint.size(); i++) {
            Position2d a = footprint.get(i);
            Position2d b = footprint.get((i + 1) % footprint.size());
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            double lengthSquared = dx * dx + dy * dy;
            double t = lengthSquared == 0.0 ? 0.0
                    : Math.clamp(((position.x() - a.x()) * dx + (position.y() - a.y()) * dy) / lengthSquared, 0.0, 1.0);
            Position2d candidate = new Position2d(a.x() + t * dx, a.y() + t * dy);
            double candidateDistanceSquared = squaredDistance(position, candidate);
            if (candidateDistanceSquared < nearestDistanceSquared) {
                nearest = candidate;
                nearestDistanceSquared = candidateDistanceSquared;
            }
        }
        return Objects.requireNonNull(nearest, "green footprint boundary");
    }

    private static boolean contains(List<Position2d> polygon, Position2d point) {
        for (int i = 0; i < polygon.size(); i++) {
            if (onSegment(polygon.get(i), polygon.get((i + 1) % polygon.size()), point)) return true;
        }
        boolean inside = false;
        for (int i = 0, previous = polygon.size() - 1; i < polygon.size(); previous = i++) {
            Position2d a = polygon.get(i);
            Position2d b = polygon.get(previous);
            if ((a.y() > point.y()) != (b.y() > point.y())
                    && point.x() < (b.x() - a.x()) * (point.y() - a.y()) / (b.y() - a.y()) + a.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean onSegment(Position2d a, Position2d b, Position2d point) {
        double cross = (b.x() - a.x()) * (point.y() - a.y()) - (b.y() - a.y()) * (point.x() - a.x());
        return Math.abs(cross) < 1.0e-9
                && point.x() >= Math.min(a.x(), b.x()) - 1.0e-9 && point.x() <= Math.max(a.x(), b.x()) + 1.0e-9
                && point.y() >= Math.min(a.y(), b.y()) - 1.0e-9 && point.y() <= Math.max(a.y(), b.y()) + 1.0e-9;
    }

    private static double squaredDistance(Position2d first, Position2d second) {
        double x = first.x() - second.x();
        double y = first.y() - second.y();
        return x * x + y * y;
    }
}
