package com.progolf.sim.practice;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.shot.*;
import com.progolf.sim.spatial.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** An unsaved sequence of independent balls from seven fixed hitting bays. */
public final class PracticeRange {
    public record Target(String label, Position2d position) { }
    public record Result(ShotOutcome outcome, double totalDistance, double proximity, int ballNumber) { }

    // The seven mat centres are 168 SVG units apart at the range's 2.64 units-per-yard tee projection.
    private static final double BAY_SPACING_YARDS = 700.0 / 11.0;
    private static final List<Position2d> BAYS = java.util.stream.IntStream.range(0, 7)
            .mapToObj(index -> new Position2d((index - 3) * BAY_SPACING_YARDS, 0)).toList();
    private static final Position2d BAY = BAYS.get(3);
    private static final Position2d REFERENCE_CUP = new Position2d(0, 320);
    private static final double AIM_HALF_WIDTH = 220;
    private static final double FIELD_HALF_WIDTH = 250;
    private static final double TARGET_GREEN_RADIUS = 11;
    private static final List<Target> TARGETS = List.of(
            new Target("40 yd", new Position2d(-18, 36)),
            new Target("75 yd", new Position2d(48, 58)),
            new Target("110 yd", new Position2d(-65, 89)),
            new Target("150 yd", new Position2d(95, 116)),
            new Target("200 yd", new Position2d(-110, 167)),
            new Target("245 yd", new Position2d(135, 205)),
            new Target("290 yd", new Position2d(-145, 251)));
    private static final CourseGeometry GEOMETRY = geometry();
    private static final ShotZoneProfile ZONES = new ShotZoneProfile(List.of(
            new ZoneBand(0, 500, List.of(new LateralRegion(260, Surface.FAIRWAY)))));

    private final Attributes attributes;
    private final GolferState state;
    private final Handedness handedness;
    private final long seed;
    private int ballsHit;

    public PracticeRange(Attributes attributes, GolferState state, Handedness handedness, long seed) {
        this.attributes = Objects.requireNonNull(attributes);
        this.state = Objects.requireNonNull(state);
        this.handedness = handedness == null ? Handedness.RIGHT : handedness;
        this.seed = seed;
    }

    public static List<Target> targets() { return TARGETS; }
    public static Position2d bay() { return BAY; }
    public static List<Position2d> bays() { return BAYS; }
    public static CourseGeometry geometryView() { return GEOMETRY; }
    public static double aimHalfWidth() { return AIM_HALF_WIDTH; }
    public static double fieldHalfWidth() { return FIELD_HALF_WIDTH; }
    public static double targetGreenRadius() { return TARGET_GREEN_RADIUS; }
    public synchronized int ballsHit() { return ballsHit; }
    public List<ShotGuidance.ClubReach> clubs() { return ShotGuidance.clubReaches(attributes, Surface.TEE_BOX); }

    public synchronized Result hit(BallStrikeIntent intent, String expectedRevision) {
        return hit(intent, 4, expectedRevision);
    }

    public synchronized Result hit(BallStrikeIntent intent, int bayNumber, String expectedRevision) {
        Objects.requireNonNull(intent, "intent");
        if (bayNumber < 1 || bayNumber > BAYS.size()) throw new IllegalArgumentException("unknown practice bay");
        if (!Integer.toString(ballsHit).equals(expectedRevision)) {
            throw new IllegalArgumentException("stale practice shot revision");
        }
        Position2d origin = BAYS.get(bayNumber - 1);
        Position2d target = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
        if (target.x() < -AIM_HALF_WIDTH || target.x() > AIM_HALF_WIDTH || target.y() < 15 || target.y() > 320) {
            throw new IllegalArgumentException("aim outside the driving range");
        }
        var eligibility = ShotFamilyEligibility.evaluate(Surface.TEE_BOX, ClubSpec.of(intent.club()), intent.shotFamily());
        if (!eligibility.allowed()) throw new IllegalArgumentException(eligibility.reason());
        ShotDecision decision = ShotDecision.fromIntent(intent, origin.distanceTo(target), Strategy.BALANCED);
        ShotContext context = new ShotContext(attributes, state, Environment.calm(), origin.distanceTo(REFERENCE_CUP),
                ZONES, decision, new SeedCoordinate(seed, 0, -777, 0, 0, 1, ballsHit + 1),
                Surface.TEE_BOX, 0, new BallState(origin, Surface.TEE_BOX), GEOMETRY, REFERENCE_CUP,
                target, handedness);
        ShotOutcome outcome = ShotResolver.resolveShotWithTrace(context);
        ballsHit++;
        Position2d finish = outcome.trace().finalPoint();
        return new Result(outcome, origin.distanceTo(finish), target.distanceTo(finish), ballsHit);
    }

    private static CourseGeometry geometry() {
        List<TerrainRegion> regions = new ArrayList<>();
        for (Position2d bay : BAYS) {
            regions.add(new TerrainRegion(Surface.TEE_BOX, rectangle(bay.x() - 7, -4, bay.x() + 7, 5)));
        }
        // Generous runout beyond the last flag keeps ordinary drives free of course-rule penalties.
        regions.add(new TerrainRegion(Surface.FAIRWAY, rectangle(-FIELD_HALF_WIDTH, 5, FIELD_HALF_WIDTH, 500)));
        for (Target target : TARGETS) regions.add(new TerrainRegion(Surface.GREEN, disc(target.position(), TARGET_GREEN_RADIUS)));
        return new CourseGeometry(BAY, REFERENCE_CUP, rectangle(-260, -25, 260, 505), regions);
    }

    private static List<Position2d> rectangle(double left, double near, double right, double far) {
        return List.of(new Position2d(left, near), new Position2d(right, near),
                new Position2d(right, far), new Position2d(left, far));
    }

    private static List<Position2d> disc(Position2d center, double radius) {
        List<Position2d> points = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            double angle = 2 * Math.PI * index / 16;
            points.add(new Position2d(center.x() + radius * Math.cos(angle), center.y() + radius * Math.sin(angle)));
        }
        return List.copyOf(points);
    }
}
