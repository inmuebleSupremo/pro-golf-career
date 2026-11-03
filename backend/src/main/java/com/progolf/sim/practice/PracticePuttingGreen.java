package com.progolf.sim.practice;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.GreenSurfaceGenerator;
import com.progolf.sim.course.GreenSurfaceVersion;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.putt.PhysicalPuttExecutionMode;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.putt.PhysicalPuttModelVersion;
import com.progolf.sim.putt.PhysicalPuttOutcome;
import com.progolf.sim.putt.PhysicalPuttRequest;
import com.progolf.sim.putt.PhysicalPuttSolver;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One temporary five-cup surface; resting balls are retained as world-space entities for future collisions. */
public final class PracticePuttingGreen {
    public record Cup(int number, Position2d position) { }
    public record Ball(int number, Position2d position) { }
    public record State(String shotRevision, int puttsHit, Position2d startingPosition,
                        Position2d readyPosition, List<Ball> restingBalls) { }
    public record Result(PhysicalPuttOutcome outcome, Integer holedCup, int ballNumber, State state) { }

    private static final int MAX_RESTING_BALLS = 30;
    private static final long SURFACE_SEED = 71482L;
    // Approved 1280×720 layout coordinates map uniformly to yards; positive world y points up the green.
    private static final int[][] OUTLINE = {
            {146, 125}, {260, 88}, {393, 103}, {528, 74}, {657, 95}, {813, 83},
            {960, 112}, {1097, 179}, {1142, 294}, {1112, 414}, {1041, 527},
            {913, 606}, {751, 631}, {574, 616}, {443, 650}, {285, 626},
            {148, 553}, {94, 444}, {81, 310}, {96, 213}
    };
    private static final List<Position2d> FOOTPRINT = footprint();
    private static final List<Cup> CUPS = List.of(
            new Cup(1, yardPoint(335, 213)), new Cup(2, yardPoint(855, 175)),
            new Cup(3, yardPoint(694, 374)), new Cup(4, yardPoint(1010, 495)),
            new Cup(5, yardPoint(314, 514)));
    private static final Position2d DEFAULT_START = yardPoint(515, 565);
    private static final CourseGeometry GEOMETRY = new CourseGeometry(DEFAULT_START, new Position2d(0, 0),
            List.of(new Position2d(-20, -12), new Position2d(20, -12),
                    new Position2d(20, 12), new Position2d(-20, 12)),
            List.of(new TerrainRegion(Surface.GREEN, FOOTPRINT)));
    private static final GreenSurface SURFACE = GreenSurfaceGenerator.generate(GEOMETRY, SURFACE_SEED,
            new Position2d(0, 1), GreenSurfaceVersion.V1);

    private final Attributes attributes;
    private final GolferState golferState;
    private final long seed;
    private final List<Ball> restingBalls = new ArrayList<>();
    private Position2d startingPosition = DEFAULT_START;
    private Position2d readyPosition = DEFAULT_START;
    private int revision;
    private int puttsHit;

    public PracticePuttingGreen(Attributes attributes, GolferState golferState, long seed) {
        this.attributes = Objects.requireNonNull(attributes);
        this.golferState = Objects.requireNonNull(golferState);
        this.seed = seed;
    }

    public static List<Cup> cups() { return CUPS; }
    public static List<Position2d> footprintView() { return FOOTPRINT; }
    public static GreenSurface surfaceView() { return SURFACE; }

    public synchronized State state() {
        return new State(Integer.toString(revision), puttsHit, startingPosition, readyPosition,
                List.copyOf(restingBalls));
    }

    public synchronized State placeBall(Position2d requested, String expectedRevision) {
        checkRevision(expectedRevision);
        if (!SURFACE.contains(Objects.requireNonNull(requested, "requested"))) {
            throw new IllegalArgumentException("starting position must be on the putting green");
        }
        Position2d next = nearestClearPosition(requested);
        if (next == null) throw new IllegalArgumentException("no clear starting position near the chosen spot");
        startingPosition = requested;
        readyPosition = next;
        revision++;
        return state();
    }

    public synchronized State collectBalls(String expectedRevision) {
        checkRevision(expectedRevision);
        restingBalls.clear();
        readyPosition = nearestClearPosition(startingPosition);
        revision++;
        return state();
    }

    public synchronized Result putt(int targetCup, PhysicalPuttIntent intent, String expectedRevision) {
        checkRevision(expectedRevision);
        Objects.requireNonNull(intent, "intent");
        if (targetCup < 1 || targetCup > CUPS.size()) throw new IllegalArgumentException("unknown practice cup");
        Position2d selectedCup = CUPS.get(targetCup - 1).position();
        var request = new PhysicalPuttRequest(PhysicalPuttModelVersion.V1, SURFACE, readyPosition,
                selectedCup, intent, attributes, golferState,
                Seeds.forCoordinate(new SeedCoordinate(seed, 0, -778, 0, 0, 1, puttsHit + 1)),
                PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN);
        PhysicalPuttOutcome outcome = PhysicalPuttSolver.resolve(request,
                CUPS.stream().map(Cup::position).toList());
        puttsHit++;
        Integer holedCup = outcome.holed() ? CUPS.stream().filter(cup -> cup.position().equals(outcome.finalPosition()))
                .findFirst().orElseThrow().number() : null;
        if (!outcome.holed()) {
            restingBalls.add(new Ball(puttsHit, outcome.finalPosition()));
            if (restingBalls.size() > MAX_RESTING_BALLS) restingBalls.removeFirst();
        }
        readyPosition = nearestClearPosition(startingPosition);
        if (readyPosition == null) throw new IllegalStateException("no clear starting position remains on the green");
        revision++;
        return new Result(outcome, holedCup, puttsHit, state());
    }

    private void checkRevision(String expected) {
        if (!Integer.toString(revision).equals(expected)) {
            throw new IllegalArgumentException("stale practice putting revision");
        }
    }

    private Position2d nearestClearPosition(Position2d desired) {
        if (clear(desired)) return desired;
        for (double radius = .25; radius <= 3.25; radius += .25) {
            for (int index = 0; index < 24; index++) {
                double angle = index * StrictMath.PI / 12;
                Position2d candidate = desired.plus(radius * StrictMath.cos(angle), radius * StrictMath.sin(angle));
                if (clear(candidate)) return candidate;
            }
        }
        return null;
    }

    private boolean clear(Position2d position) {
        return SURFACE.contains(position)
                && CUPS.stream().allMatch(cup -> cup.position().distanceTo(position) >= .4)
                && restingBalls.stream().allMatch(ball -> ball.position().distanceTo(position) >= .32);
    }

    private static Position2d yardPoint(int pixelX, int pixelY) {
        return new Position2d((pixelX - 640.0) / 35.0, (360.0 - pixelY) / 35.0);
    }

    private static List<Position2d> footprint() {
        List<Position2d> points = new ArrayList<>(OUTLINE.length);
        for (int index = OUTLINE.length - 1; index >= 0; index--) {
            points.add(yardPoint(OUTLINE[index][0], OUTLINE[index][1]));
        }
        return List.copyOf(points);
    }
}
