package com.progolf.sim.course;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.putt.PhysicalPuttExecutionMode;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.putt.PhysicalPuttModelVersion;
import com.progolf.sim.putt.PhysicalPuttOutcome;
import com.progolf.sim.putt.PhysicalPuttRequest;
import com.progolf.sim.putt.PhysicalPuttSolver;
import com.progolf.sim.putt.PhysicalPuttTrajectoryPoint;
import com.progolf.sim.shot.GolferState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight developer SVGs made from authoritative solver trajectories and GreenSurface height samples. */
public final class PhysicalPuttDiagnostic {
    private static final Attributes DIAGNOSTIC_ATTRIBUTES = Attributes.uniform(72)
            .with(Attribute.PUTTING_ACCURACY, 82).with(Attribute.PUTTING_PROXIMITY, 78);

    private PhysicalPuttDiagnostic() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("target", "physical-putt-diagnostics") : Path.of(args[0]);
        Files.createDirectories(output);
        Files.writeString(output.resolve("controlled-physical-putts.svg"), controlledSvg(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("generator8-physical-putts.svg"), generatedSvg(), StandardCharsets.UTF_8);
        System.out.println(output.toAbsolutePath());
    }

    private static String controlledSvg() {
        GreenSurface flat = plane(0, 0);
        GreenSurface risingY = plane(0, 0.02);
        GreenSurface fallingY = plane(0, -0.015);
        Position2d remoteCup = new Position2d(12, 12);
        List<Scenario> scenarios = List.of(
                scenario("Flat · 15 ft", flat, new Position2d(-8, 0), remoteCup, new Position2d(1, 0), 15, 1),
                scenario("Uphill · 15 ft", risingY, new Position2d(0, -8), remoteCup, new Position2d(0, 1), 15, 2),
                scenario("Downhill · 15 ft", risingY, new Position2d(0, 8), remoteCup, new Position2d(0, -1), 15, 3),
                scenario("Break left", risingY, new Position2d(-8, 0), remoteCup, new Position2d(1, 0), 20, 4),
                scenario("Break right", fallingY, new Position2d(-8, 0), remoteCup, new Position2d(1, 0), 20, 5),
                scenario("Same line · 12 / 30 ft", plane(0, 0.012), new Position2d(-8, 0), remoteCup,
                        new Position2d(1, 0), 12, 6, 30),
                scenario("Central slow capture", flat, new Position2d(-5.0 / 3.0, 0), new Position2d(0, 0),
                        new Position2d(1, 0), 5, 7),
                scenario("Near cup edge miss", flat, new Position2d(-5.0 / 3.0, 0.10), new Position2d(0, 0),
                        new Position2d(1, 0), 5, 8));
        return svg("Controlled physical-putt calibration", scenarios, 4, 32.0);
    }

    private static String generatedSvg() {
        List<Scenario> scenarios = new ArrayList<>();
        EnvironmentClassification[] environments = {EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS,
                EnvironmentClassification.DESERT};
        long[] ids = {831L, 947L, 1059L};
        int[] holes = {1, 4, 7, 10, 13, 16};
        int seed = 40;
        for (int environmentIndex = 0; environmentIndex < environments.length; environmentIndex++) {
            Course course = CourseGenerator.generate(new SeedCoordinate(0x5055545444494147L, 3, ids[environmentIndex],
                    environments[environmentIndex].ordinal(), 0, 0, 0), environments[environmentIndex],
                    CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
            for (int offset = 0; offset < 2; offset++) {
                GeneratedHole hole = course.holes().get(holes[environmentIndex * 2 + offset] - 1);
                GreenSurface surface = GreenSurfaceGenerator.generate(hole, GreenSurfaceVersion.V1);
                Position2d cup = hole.forRound(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN).cupPosition();
                Position2d start = startTowardCentre(surface, cup, hole.geometry().greenCenter());
                Position2d direction = direction(start, cup);
                double feet = start.distanceTo(cup) * 3.0;
                scenarios.add(scenario(environments[environmentIndex] + " H" + hole.number() + " · "
                        + f(feet) + " ft", surface, start, cup, direction, feet, seed++));
            }
        }
        return svg("Generator 8 / GreenSurface V1 physical trajectories", scenarios, 3, 55.0);
    }

    private static Scenario scenario(String label, GreenSurface surface, Position2d start, Position2d cup,
                                     Position2d direction, double feet, long seed, double... additionalFeet) {
        List<PhysicalPuttOutcome> outcomes = new ArrayList<>();
        outcomes.add(resolve(surface, start, cup, direction, feet, seed));
        for (double pace : additionalFeet) outcomes.add(resolve(surface, start, cup, direction, pace, seed + outcomes.size()));
        return new Scenario(label, surface, cup, outcomes);
    }

    private static PhysicalPuttOutcome resolve(GreenSurface surface, Position2d start, Position2d cup,
                                               Position2d direction, double feet, long seed) {
        return PhysicalPuttSolver.resolve(new PhysicalPuttRequest(PhysicalPuttModelVersion.V1, surface, start, cup,
                new PhysicalPuttIntent(direction, feet), DIAGNOSTIC_ATTRIBUTES, GolferState.fresh(), seed,
                PhysicalPuttExecutionMode.IDEAL));
    }

    private static String svg(String title, List<Scenario> scenarios, int columns, double span) {
        int rows = (int) StrictMath.ceil(scenarios.size() / (double) columns);
        double width = columns * 320.0;
        double height = 65.0 + rows * 300.0;
        StringBuilder out = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(f(width)).append("\" height=\"").append(f(height)).append("\" viewBox=\"0 0 ")
                .append(f(width)).append(' ').append(f(height)).append("\"><style>")
                .append("text{font-family:system-ui,sans-serif;fill:#1f2b20}.panel{fill:#f8f5e9;stroke:#9b927d}.green{fill:#e7efb6}")
                .append(".contour{stroke:#a85b45;stroke-width:.8;fill:none}.outline{fill:none;stroke:#37542e;stroke-width:1.2}")
                .append(".cup{fill:#fff;stroke:#202020;stroke-width:1.2}.path0{fill:none;stroke:#2166ac;stroke-width:2}")
                .append(".path1{fill:none;stroke:#d6604d;stroke-width:2}.start{fill:#202020}.meta{font-size:10px}.title{font-size:18px;font-weight:650}</style>")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"#e9e4d5\"/><text class=\"title\" x=\"18\" y=\"25\">")
                .append(title).append("</text><text class=\"meta\" x=\"18\" y=\"43\">Contours are GreenSurface samples; paths are PhysicalPuttSolver trajectory samples. Blue is first pace; red is second pace.</text>");
        for (int index = 0; index < scenarios.size(); index++) {
            double x = 320.0 * (index % columns);
            double y = 65.0 + 300.0 * (index / columns);
            appendPanel(out, scenarios.get(index), x, y, span, "p" + index);
        }
        return out.append("</svg>").toString();
    }

    private static void appendPanel(StringBuilder out, Scenario scenario, double x, double y, double span, String id) {
        Position2d centre = centre(scenario.surface().footprint());
        Frame frame = new Frame(centre, x, y, span);
        out.append("<g><rect class=\"panel\" x=\"").append(f(x + 4)).append("\" y=\"").append(f(y)).append("\" width=\"312\" height=\"286\" rx=\"5\"/>")
                .append("<clipPath id=\"").append(id).append("\"><rect x=\"").append(f(x + 4)).append("\" y=\"").append(f(y))
                .append("\" width=\"312\" height=\"286\"/></clipPath><clipPath id=\"").append(id).append("-green\"><polygon points=\"")
                .append(points(scenario.surface().footprint(), frame)).append("\"/></clipPath><g clip-path=\"url(#").append(id).append(")\">")
                .append("<polygon class=\"green\" points=\"").append(points(scenario.surface().footprint(), frame)).append("\"/>");
        out.append("<g clip-path=\"url(#").append(id).append("-green)\">");
        contours(out, scenario.surface(), frame);
        out.append("</g>");
        out.append("<polygon class=\"outline\" points=\"").append(points(scenario.surface().footprint(), frame)).append("\"/>");
        for (int index = 0; index < scenario.outcomes().size(); index++) {
            PhysicalPuttOutcome outcome = scenario.outcomes().get(index);
            out.append("<polyline class=\"path").append(index).append("\" points=\"")
                    .append(trajectory(outcome.trajectory(), frame)).append("\"/>");
            Position2d start = outcome.trajectory().get(0).position();
            out.append("<circle class=\"start\" cx=\"").append(f(frame.x(start.x()))).append("\" cy=\"")
                    .append(f(frame.y(start.y()))).append("\" r=\"2.2\"/>");
        }
        out.append("<circle class=\"cup\" cx=\"").append(f(frame.x(scenario.cup().x()))).append("\" cy=\"")
                .append(f(frame.y(scenario.cup().y()))).append("\" r=\"3.2\"/></g>")
                .append("<text class=\"meta\" x=\"").append(f(x + 12)).append("\" y=\"").append(f(y + 16)).append("\">")
                .append(scenario.label()).append("</text><text class=\"meta\" x=\"").append(f(x + 12)).append("\" y=\"")
                .append(f(y + 278)).append("\">");
        for (int index = 0; index < scenario.outcomes().size(); index++) {
            if (index > 0) out.append(" · ");
            PhysicalPuttOutcome outcome = scenario.outcomes().get(index);
            out.append(outcome.holed() ? "captured" : outcome.terminalCondition().name().toLowerCase(Locale.ROOT));
        }
        out.append("</text></g>");
    }

    private static void contours(StringBuilder out, GreenSurface surface, Frame frame) {
        double range = surface.maximumElevation() - surface.minimumElevation();
        double interval = Math.max(0.025, range / 6.0);
        for (double level = StrictMath.ceil(surface.minimumElevation() / interval) * interval;
             level < surface.maximumElevation() - 1.0e-9; level += interval) {
            for (int row = 0; row < surface.rows() - 1; row++) for (int column = 0; column < surface.columns() - 1; column++) {
                Position2d[] points = {surface.gridPoint(column, row), surface.gridPoint(column + 1, row),
                        surface.gridPoint(column + 1, row + 1), surface.gridPoint(column, row + 1)};
                double[] heights = {surface.gridElevation(column, row), surface.gridElevation(column + 1, row),
                        surface.gridElevation(column + 1, row + 1), surface.gridElevation(column, row + 1)};
                List<Position2d> crossings = new ArrayList<>();
                crossing(crossings, points[0], heights[0], points[1], heights[1], level);
                crossing(crossings, points[1], heights[1], points[2], heights[2], level);
                crossing(crossings, points[2], heights[2], points[3], heights[3], level);
                crossing(crossings, points[3], heights[3], points[0], heights[0], level);
                if (crossings.size() == 2) line(out, frame, crossings.get(0), crossings.get(1));
                if (crossings.size() == 4) { line(out, frame, crossings.get(0), crossings.get(1)); line(out, frame, crossings.get(2), crossings.get(3)); }
            }
        }
    }

    private static void crossing(List<Position2d> crossings, Position2d a, double heightA, Position2d b, double heightB, double level) {
        if ((heightA < level && heightB >= level) || (heightB < level && heightA >= level)) {
            double t = (level - heightA) / (heightB - heightA);
            crossings.add(new Position2d(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t));
        }
    }

    private static void line(StringBuilder out, Frame frame, Position2d a, Position2d b) {
        out.append("<line class=\"contour\" x1=\"").append(f(frame.x(a.x()))).append("\" y1=\"").append(f(frame.y(a.y())))
                .append("\" x2=\"").append(f(frame.x(b.x()))).append("\" y2=\"").append(f(frame.y(b.y()))).append("\"/>");
    }

    private static String points(List<Position2d> points, Frame frame) {
        StringBuilder result = new StringBuilder();
        for (Position2d point : points) result.append(f(frame.x(point.x()))).append(',').append(f(frame.y(point.y()))).append(' ');
        return result.toString();
    }

    private static String trajectory(List<PhysicalPuttTrajectoryPoint> trajectory, Frame frame) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < trajectory.size(); index += 3) {
            Position2d point = trajectory.get(index).position();
            result.append(f(frame.x(point.x()))).append(',').append(f(frame.y(point.y()))).append(' ');
        }
        Position2d last = trajectory.get(trajectory.size() - 1).position();
        return result.append(f(frame.x(last.x()))).append(',').append(f(frame.y(last.y()))).toString();
    }

    private static Position2d startTowardCentre(GreenSurface surface, Position2d cup, Position2d centre) {
        Position2d away = direction(cup, centre);
        for (double distance = 6.0; distance >= 1.0; distance -= 0.5) {
            Position2d candidate = cup.plus(away.x() * distance, away.y() * distance);
            if (surface.contains(candidate)) return candidate;
        }
        return centre;
    }

    private static Position2d direction(Position2d from, Position2d to) {
        double x = to.x() - from.x();
        double y = to.y() - from.y();
        double length = StrictMath.hypot(x, y);
        return length < 1.0e-9 ? new Position2d(1, 0) : new Position2d(x / length, y / length);
    }

    private static Position2d centre(List<Position2d> points) {
        double x = 0, y = 0;
        for (Position2d point : points) { x += point.x(); y += point.y(); }
        return new Position2d(x / points.size(), y / points.size());
    }

    private static GreenSurface plane(double gradientX, double gradientY) {
        int columns = 49, rows = 49;
        double min = -16, max = 16;
        double[] elevations = new double[columns * rows];
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            double x = min + (max - min) * column / (columns - 1.0);
            double y = min + (max - min) * row / (rows - 1.0);
            elevations[row * columns + column] = gradientX * x + gradientY * y;
        }
        List<Position2d> footprint = List.of(new Position2d(min, min), new Position2d(max, min),
                new Position2d(max, max), new Position2d(min, max));
        return new GreenSurface(new GreenSurfaceProvenance(GreenSurfaceVersion.V1, 21L, 22L, 23L, new Position2d(0, 1)),
                footprint, min, max, min, max, columns, rows, elevations);
    }

    private static String f(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    private record Scenario(String label, GreenSurface surface, Position2d cup, List<PhysicalPuttOutcome> outcomes) { }

    private record Frame(Position2d centre, double panelX, double panelY, double span) {
        double x(double worldX) { return panelX + 10 + (worldX - centre.x() + span / 2) / span * 300; }
        double y(double worldY) { return panelY + 28 + (span / 2 - (worldY - centre.y())) / span * 235; }
    }
}
