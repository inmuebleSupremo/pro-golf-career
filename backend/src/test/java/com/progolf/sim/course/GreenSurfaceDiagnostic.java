package com.progolf.sim.course;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes complete 18-hole Generator 8 SVG contact sheets. Every contour and slope arrow is sampled
 * from GreenSurface itself; this is a developer review artifact, not a frontend renderer.
 */
public final class GreenSurfaceDiagnostic {
    private static final long WORLD = 0x475245454e444941L;
    private static final EnvironmentClassification[] ENVIRONMENTS = {
            EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS, EnvironmentClassification.DESERT};
    private static final long[] COURSE_IDS = {401L, 503L, 607L};
    private static final int COLUMNS = 6;
    private static final double PANEL_WIDTH = 240.0;
    private static final double PANEL_HEIGHT = 280.0;

    private GreenSurfaceDiagnostic() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("target", "green-surface-diagnostics") : Path.of(args[0]);
        Files.createDirectories(output);
        List<String> summary = new ArrayList<>();
        summary.add("environment,courseId,hole,par,widthYd,depthYd,elevationRangeYd,maxSlopePercent,cupSlopePercent,downhillDegrees");
        for (int index = 0; index < ENVIRONMENTS.length; index++) {
            EnvironmentClassification environment = ENVIRONMENTS[index];
            long courseId = COURSE_IDS[index];
            Course course = CourseGenerator.generate(coordinate(courseId, environment), environment,
                    CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
            StringBuilder svg = new StringBuilder(header(environment, courseId));
            for (GeneratedHole hole : course.holes()) {
                GreenSurface surface = GreenSurfaceGenerator.generate(hole, GreenSurfaceVersion.V1);
                Position2d cup = hole.forRound(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN).cupPosition();
                svg.append(panel(hole, surface, cup));
                summary.add(summary(environment, courseId, hole, surface, cup));
            }
            svg.append("</svg>");
            Files.writeString(output.resolve(environment.name().toLowerCase(Locale.ROOT) + "-contact-sheet.svg"),
                    svg, StandardCharsets.UTF_8);
        }
        Files.writeString(output.resolve("green-surface-summary.csv"), String.join("\n", summary) + "\n",
                StandardCharsets.UTF_8);
        System.out.println(output.toAbsolutePath());
    }

    private static String header(EnvironmentClassification environment, long courseId) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1440\" height=\"875\" viewBox=\"0 0 1440 875\">"
                + "<defs><marker id=\"arrow\" markerWidth=\"5\" markerHeight=\"5\" refX=\"4\" refY=\"2.5\" orient=\"auto\">"
                + "<path d=\"M0 0L5 2.5L0 5z\" fill=\"#654321\"/></marker></defs><style>"
                + "text{font-family:system-ui,sans-serif;fill:#1e2b20}.panel{fill:#f6f3e8;stroke:#9b927d}"
                + ".rough{fill:#9bbf7a}.fairway{fill:#b9d88a}.fringe{fill:#d6e79c}.green{fill:#e7efb6}"
                + ".bunker{fill:#ead39d}.water{fill:#93c8dc}.trees{fill:#5d8a61}.contour{stroke:#a85b45;stroke-width:.85;fill:none}"
                + ".outline{fill:none;stroke:#37542e;stroke-width:1.1}.approach{stroke:#335f8d;stroke-width:1.25;marker-end:url(#arrow)}"
                + ".slope{stroke:#654321;stroke-width:1;marker-end:url(#arrow)}.cup{fill:#fff;stroke:#202020;stroke-width:1.1}"
                + ".meta{font-size:9px}.title{font-size:17px;font-weight:650}</style><rect width=\"1440\" height=\"875\" fill=\"#e9e4d5\"/>"
                + "<text x=\"18\" y=\"24\" class=\"title\">" + environment + " · Generator 8 complete green-surface sample · course "
                + courseId + "</text><text x=\"18\" y=\"41\" class=\"meta\">Canonical terrain, green footprint, effective cup and approach are shown. "
                + "Contours and downhill arrows use the same GreenSurface V1 height and gradient data.</text>";
    }

    private static String panel(GeneratedHole hole, GreenSurface surface, Position2d cup) {
        int index = hole.number() - 1;
        double x = (index % COLUMNS) * PANEL_WIDTH;
        double y = 55.0 + (index / COLUMNS) * (PANEL_HEIGHT - 10.0);
        Frame frame = new Frame(hole.geometry().greenCenter(), x, y, PANEL_WIDTH, PANEL_HEIGHT - 15.0);
        String greenClip = "green-" + hole.number();
        String panelClip = "panel-" + hole.number();
        StringBuilder result = new StringBuilder("<g><rect class=\"panel\" x=\"").append(f(x + 3)).append("\" y=\"").append(f(y))
                .append("\" width=\"234\" height=\"265\" rx=\"5\"/><clipPath id=\"").append(panelClip)
                .append("\"><rect x=\"").append(f(x + 3)).append("\" y=\"").append(f(y)).append("\" width=\"234\" height=\"265\"/></clipPath>")
                .append("<clipPath id=\"").append(greenClip).append("\"><polygon points=\"")
                .append(points(surface.footprint(), frame)).append("\"/></clipPath><g clip-path=\"url(#").append(panelClip).append(")\">");
        for (TerrainRegion region : hole.geometry().regions()) {
            result.append("<polygon class=\"").append(terrainClass(region.surface())).append("\" points=\"")
                    .append(points(region.boundary(), frame)).append("\"/>");
        }
        result.append("<g clip-path=\"url(#").append(greenClip).append(")\">");
        contours(result, surface, frame);
        arrows(result, surface, frame);
        result.append("</g><polygon class=\"outline\" points=\"").append(points(surface.footprint(), frame)).append("\"/>");
        Position2d centre = hole.geometry().greenCenter();
        Position2d approach = surface.provenance().approachDirection();
        result.append(line(frame, centre.plus(-approach.x() * 27, -approach.y() * 27), centre, "approach"));
        result.append("<circle class=\"cup\" cx=\"").append(f(frame.x(cup.x()))).append("\" cy=\"").append(f(frame.y(cup.y())))
                .append("\" r=\"3\"/></g>");
        GreenSurfaceSample pin = surface.sampleAt(cup);
        result.append("<text x=\"").append(f(x + 10)).append("\" y=\"").append(f(y + 14)).append("\" class=\"meta\">H")
                .append(hole.number()).append(" · Par ").append(hole.par()).append(" · ").append(Math.round(hole.length())).append(" yd</text>")
                .append("<text x=\"").append(f(x + 10)).append("\" y=\"").append(f(y + 247)).append("\" class=\"meta\">range ")
                .append(f(surface.maximumElevation() - surface.minimumElevation())).append(" yd · max ")
                .append(f(surface.maximumSlope() * 100)).append("% · cup ").append(f(pin.gradient().magnitude() * 100))
                .append("%</text><text x=\"").append(f(x + 10)).append("\" y=\"").append(f(y + 259)).append("\" class=\"meta\">grid ")
                .append(surface.columns()).append("×").append(surface.rows()).append(" · contour ").append(f(interval(surface)))
                .append(" yd</text></g>");
        return result.toString();
    }

    private static void contours(StringBuilder out, GreenSurface surface, Frame frame) {
        double interval = interval(surface);
        for (double level = StrictMath.ceil(surface.minimumElevation() / interval) * interval;
             level < surface.maximumElevation() - 1.0e-9; level += interval) {
            for (int row = 0; row < surface.rows() - 1; row++) for (int column = 0; column < surface.columns() - 1; column++) {
                Position2d[] points = {surface.gridPoint(column, row), surface.gridPoint(column + 1, row),
                        surface.gridPoint(column + 1, row + 1), surface.gridPoint(column, row + 1)};
                double[] height = {surface.gridElevation(column, row), surface.gridElevation(column + 1, row),
                        surface.gridElevation(column + 1, row + 1), surface.gridElevation(column, row + 1)};
                List<Position2d> crossings = new ArrayList<>(4);
                crossing(crossings, points[0], height[0], points[1], height[1], level);
                crossing(crossings, points[1], height[1], points[2], height[2], level);
                crossing(crossings, points[2], height[2], points[3], height[3], level);
                crossing(crossings, points[3], height[3], points[0], height[0], level);
                if (crossings.size() == 2) out.append(line(frame, crossings.get(0), crossings.get(1), "contour"));
                if (crossings.size() == 4) {
                    out.append(line(frame, crossings.get(0), crossings.get(1), "contour"));
                    out.append(line(frame, crossings.get(2), crossings.get(3), "contour"));
                }
            }
        }
    }

    private static void crossing(List<Position2d> crossings, Position2d a, double heightA, Position2d b,
                                 double heightB, double level) {
        if ((heightA < level && heightB >= level) || (heightB < level && heightA >= level)) {
            double t = (level - heightA) / (heightB - heightA);
            crossings.add(new Position2d(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t));
        }
    }

    private static void arrows(StringBuilder out, GreenSurface surface, Frame frame) {
        Bounds bounds = Bounds.of(surface.footprint());
        for (int y = 1; y <= 4; y++) for (int x = 1; x <= 4; x++) {
            Position2d point = new Position2d(bounds.minX + (bounds.maxX - bounds.minX) * x / 5.0,
                    bounds.minY + (bounds.maxY - bounds.minY) * y / 5.0);
            GreenSurfaceSample sample = surface.sampleAt(point);
            if (sample.location() != GreenSurfaceLocation.ON_GREEN || sample.gradient().magnitude() < 0.002) continue;
            Position2d downhill = sample.gradient().downhillDirection();
            out.append(line(frame, point, point.plus(downhill.x() * 3.0, downhill.y() * 3.0), "slope"));
        }
    }

    private static String summary(EnvironmentClassification environment, long courseId, GeneratedHole hole,
                                  GreenSurface surface, Position2d cup) {
        Bounds bounds = Bounds.of(surface.footprint());
        GreenSurfaceGradient dominant = meanGradient(surface);
        double degrees = dominant.magnitude() < 1.0e-9 ? Double.NaN
                : (StrictMath.toDegrees(StrictMath.atan2(-dominant.y(), -dominant.x())) + 360.0) % 360.0;
        return String.format(Locale.ROOT, "%s,%d,%d,%d,%.3f,%.3f,%.4f,%.3f,%.3f,%.1f", environment, courseId,
                hole.number(), hole.par(), bounds.maxX - bounds.minX, bounds.maxY - bounds.minY,
                surface.maximumElevation() - surface.minimumElevation(), surface.maximumSlope() * 100,
                surface.gradientAt(cup).magnitude() * 100, degrees);
    }

    private static GreenSurfaceGradient meanGradient(GreenSurface surface) {
        double x = 0.0, y = 0.0;
        int count = 0;
        for (int row = 0; row < surface.rows(); row++) for (int column = 0; column < surface.columns(); column++) {
            Position2d point = surface.gridPoint(column, row);
            if (!surface.contains(point)) continue;
            GreenSurfaceGradient gradient = surface.gradientAt(point);
            x += gradient.x();
            y += gradient.y();
            count++;
        }
        return new GreenSurfaceGradient(x / count, y / count);
    }

    private static double interval(GreenSurface surface) {
        return Math.max(0.025, (surface.maximumElevation() - surface.minimumElevation()) / 6.0);
    }

    private static String terrainClass(Surface surface) {
        return switch (surface) {
            case GREEN -> "green";
            case FRINGE -> "fringe";
            case FAIRWAY, FIRST_CUT -> "fairway";
            case BUNKER, WASTE_AREA -> "bunker";
            case WATER -> "water";
            case TREES, RECOVERY_AREA -> "trees";
            case PRIMARY_ROUGH, DEEP_ROUGH, TEE_BOX, OUT_OF_BOUNDS -> "rough";
        };
    }

    private static String points(List<Position2d> points, Frame frame) {
        StringBuilder result = new StringBuilder();
        for (Position2d point : points) result.append(f(frame.x(point.x()))).append(',').append(f(frame.y(point.y()))).append(' ');
        return result.toString();
    }

    private static String line(Frame frame, Position2d from, Position2d to, String css) {
        return "<line class=\"" + css + "\" x1=\"" + f(frame.x(from.x())) + "\" y1=\"" + f(frame.y(from.y()))
                + "\" x2=\"" + f(frame.x(to.x())) + "\" y2=\"" + f(frame.y(to.y())) + "\"/>";
    }

    private static String f(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static SeedCoordinate coordinate(long courseId, EnvironmentClassification environment) {
        return new SeedCoordinate(WORLD, 2, courseId, environment.ordinal(), 0, 0, 0);
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
        static Bounds of(List<Position2d> points) {
            return new Bounds(points.stream().mapToDouble(Position2d::x).min().orElseThrow(),
                    points.stream().mapToDouble(Position2d::x).max().orElseThrow(),
                    points.stream().mapToDouble(Position2d::y).min().orElseThrow(),
                    points.stream().mapToDouble(Position2d::y).max().orElseThrow());
        }
    }

    private record Frame(Position2d centre, double panelX, double panelY, double width, double height) {
        private static final double SPAN = 86.0;
        double x(double worldX) { return panelX + 8 + (worldX - centre.x() + SPAN / 2) / SPAN * (width - 16); }
        double y(double worldY) { return panelY + 24 + (SPAN / 2 - (worldY - centre.y())) / SPAN * (height - 50); }
    }
}
