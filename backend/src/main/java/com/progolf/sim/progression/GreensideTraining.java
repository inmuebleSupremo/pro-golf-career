package com.progolf.sim.progression;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.GeneratedHole;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.PinPlacementVersion;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.RoundHole;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.practice.PracticeRange;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.AimEnvelope;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ClubSpec;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotFamily;
import com.progolf.sim.shot.ShotFamilyEligibility;
import com.progolf.sim.shot.ShotExecutionProfile;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.ShotShapeEligibility;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Persisted objectives on existing generated physical holes. */
public final class GreensideTraining {
    private static final int ROUND = 1;
    private static final double TOLERANCE_YARDS = 9;
    private static final double[] OFFSETS = {1.5, 4, 9, 17, 28, 36};

    private GreensideTraining() { }

    public record Scene(Course course, GeneratedHole hole, RoundHole model, BallState ball) { }
    private record Candidate(TrainingChallenge challenge, Surface lie, double approachAngle, long order) { }

    /** Select distinct real short-game lies, prioritising a genuine bunker where the course pool has one. */
    public static List<TrainingChallenge> generate(List<Course> courses, Attributes skills, GolferState state,
                                                   long sessionSeed, int count) {
        if (courses.isEmpty()) throw new IllegalArgumentException("training requires an existing course");
        if (count < 1 || count > 8) throw new IllegalArgumentException("greenside count must be 1 to 8");
        int firstCourse = (int) Math.floorMod(sessionSeed, courses.size());
        int firstHole = (int) Math.floorMod(sessionSeed >>> 16, 18);
        List<Candidate> options = new ArrayList<>();
        Map<Surface, List<ShotGuidance.ClubReach>> reaches = new EnumMap<>(Surface.class);
        for (int courseOffset = 0; courseOffset < courses.size(); courseOffset++) {
            int courseIndex = (firstCourse + courseOffset) % courses.size();
            Course course = courses.get(courseIndex);
            if (!premiumFamily(course.identity().classification())) continue;
            for (int holeOffset = 0; holeOffset < 18; holeOffset++) {
                int holeNumber = (firstHole + holeOffset) % 18 + 1;
                RoundHole model = physicalHole(course, holeNumber);
                GreenSurface surface = model.greenSurface();
                if (!hasMatchingGreen(model, surface)) continue;
                for (Position2d start : starts(model, surface)) {
                    Surface lie = model.geometry().surfaceAt(start);
                    ShotFamily family = lie == Surface.BUNKER ? ShotFamily.BUNKER
                            : start.distanceTo(model.cupPosition()) >= 35 ? ShotFamily.PITCH : ShotFamily.CHIP;
                    ClubId club = suitableClub(skills, state, reaches, lie, family,
                            start.distanceTo(model.cupPosition()));
                    if (club == null) continue;
                    TrainingChallenge challenge = new TrainingChallenge(1, start, model.cupPosition(), 0,
                            club, family, TOLERANCE_YARDS, 0, courseIndex, holeNumber);
                    long order = Seeds.deriveSeed(sessionSeed, courseIndex * 18L + holeNumber)
                            ^ Double.doubleToLongBits(start.x()) ^ Long.rotateLeft(Double.doubleToLongBits(start.y()), 17);
                    double approachAngle = Math.atan2(start.y() - model.geometry().greenCenter().y(),
                            start.x() - model.geometry().greenCenter().x());
                    options.add(new Candidate(challenge, lie, approachAngle, order));
                }
            }
        }
        options.sort(Comparator.comparingLong(Candidate::order));
        List<Candidate> chosen = new ArrayList<>(count);
        options.stream().filter(candidate -> candidate.lie() == Surface.BUNKER)
                .findFirst().ifPresent(chosen::add);
        while (chosen.size() < count) {
            Candidate best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Candidate candidate : options) {
                if (chosen.stream().anyMatch(previous -> previous.challenge().start().equals(candidate.challenge().start())
                        && previous.challenge().courseIndex().equals(candidate.challenge().courseIndex())
                        && previous.challenge().holeNumber().equals(candidate.challenge().holeNumber()))) continue;
                int score = 0;
                if (chosen.stream().noneMatch(previous -> previous.lie() == candidate.lie())) score += 100;
                if (chosen.stream().noneMatch(previous -> previous.challenge().suggestedFamily()
                        == candidate.challenge().suggestedFamily())) score += 35;
                if (chosen.stream().noneMatch(previous -> previous.challenge().courseIndex().equals(candidate.challenge().courseIndex())
                        && previous.challenge().holeNumber().equals(candidate.challenge().holeNumber()))) score += 65;
                if (chosen.stream().noneMatch(previous -> Math.abs(previous.challenge().distance()
                        - candidate.challenge().distance()) < 9)) score += 20;
                if (chosen.stream().noneMatch(previous -> Math.abs(Math.atan2(
                        Math.sin(previous.approachAngle() - candidate.approachAngle()),
                        Math.cos(previous.approachAngle() - candidate.approachAngle()))) < Math.PI / 6)) score += 30;
                if (score > bestScore) { best = candidate; bestScore = score; }
            }
            if (best == null) throw new IllegalStateException("no varied physical greenside training lies in the course pool");
            chosen.add(best);
        }
        List<TrainingChallenge> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            TrainingChallenge challenge = chosen.get(index).challenge();
            result.add(new TrainingChallenge(index + 1, challenge.start(), challenge.target(), 0,
                    challenge.suggestedClub(), challenge.suggestedFamily(), challenge.tolerance(), 0,
                    challenge.courseIndex(), challenge.holeNumber()));
        }
        return List.copyOf(result);
    }

    public static Scene scene(List<Course> courses, TrainingChallenge challenge) {
        if (!challenge.greenside() || challenge.courseIndex() >= courses.size())
            throw new IllegalArgumentException("unknown greenside training hole");
        Course course = courses.get(challenge.courseIndex());
        RoundHole model = physicalHole(course, challenge.holeNumber());
        if (!premiumFamily(course.identity().classification()) || !hasMatchingGreen(model, model.greenSurface())
                || !model.cupPosition().equals(challenge.target()) || !supportedLie(model.geometry().surfaceAt(challenge.start())))
            throw new IllegalArgumentException("saved greenside training geometry is incompatible");
        return new Scene(course, course.holes().get(challenge.holeNumber() - 1), model,
                new BallState(challenge.start(), model.geometry().surfaceAt(challenge.start())));
    }

    public static PracticeRange.Result hit(Scene scene, TrainingChallenge challenge, BallStrikeIntent intent,
                                            Attributes attributes, GolferState state, Handedness handedness, long seed) {
        if (ClubSpec.of(intent.club()).family() != Club.WEDGE)
            throw new IllegalArgumentException("club does not match the wedge training focus");
        Surface lie = scene.ball().lie();
        var family = ShotFamilyEligibility.evaluate(lie, ClubSpec.of(intent.club()), intent.shotFamily());
        if (!family.allowed()) throw new IllegalArgumentException(family.reason());
        var shape = ShotShapeEligibility.evaluate(intent.shotFamily(), intent.shotShape());
        if (!shape.allowed()) throw new IllegalArgumentException(shape.reason());
        Position2d target = new Position2d(intent.aimPoint().x(), intent.aimPoint().y());
        CourseGeometry geometry = scene.model().geometry();
        var xs = geometry.playableBoundary().stream().mapToDouble(Position2d::x);
        var ys = geometry.playableBoundary().stream().mapToDouble(Position2d::y);
        AimEnvelope envelope = new AimEnvelope(xs.min().orElseThrow() - 100, geometry.playableBoundary().stream()
                .mapToDouble(Position2d::x).max().orElseThrow() + 100,
                ys.min().orElseThrow() - 100, geometry.playableBoundary().stream()
                .mapToDouble(Position2d::y).max().orElseThrow() + 100);
        if (!envelope.contains(intent.aimPoint())) throw new IllegalArgumentException("aim outside the training hole");
        double remaining = challenge.start().distanceTo(scene.model().cupPosition());
        ShotDecision decision = ShotDecision.fromIntent(intent, challenge.start().distanceTo(target), Strategy.BALANCED);
        ShotContext context = new ShotContext(attributes, state, Environment.calm(), remaining,
                scene.model().zoneProfileFor(remaining), decision, new SeedCoordinate(seed, 0, -779, 0, 0,
                challenge.holeNumber(), 1), lie, 0, scene.ball(), geometry, scene.model().cupPosition(), target,
                handedness, ShotContext.physicalShortGameSurface(scene.model(), decision));
        var outcome = ShotResolver.resolveShotWithTrace(context);
        Position2d finish = outcome.trace().finalPoint();
        return new PracticeRange.Result(outcome, challenge.start().distanceTo(finish),
                challenge.target().distanceTo(finish), challenge.number());
    }

    private static RoundHole physicalHole(Course course, int holeNumber) {
        return (RoundHole) course.holeModel(holeNumber, ROUND, CourseSetup.standard(),
                PinPlacementVersion.V5_EFFECTIVE_GREEN, PuttingPolicy.PHYSICAL_V1);
    }

    private static boolean hasMatchingGreen(RoundHole model, GreenSurface surface) {
        return surface != null && model.geometry().regions().stream()
                .anyMatch(region -> region.surface() == Surface.GREEN && region.boundary().equals(surface.footprint()))
                && model.geometry().regions().stream().anyMatch(region -> region.surface() == Surface.FRINGE);
    }

    private static List<Position2d> starts(RoundHole model, GreenSurface surface) {
        CourseGeometry geometry = model.geometry();
        Position2d center = geometry.greenCenter();
        if (!surface.contains(center)) return List.of();
        List<Position2d> result = new ArrayList<>();
        double dx = geometry.tee().x() - center.x(), dy = geometry.tee().y() - center.y();
        double length = Math.hypot(dx, dy);
        if (length < 1) return List.of();
        double angle = Math.atan2(dy, dx);
        for (int ray = 0; ray < 16; ray++) {
            double bearing = ray * Math.PI / 8;
            double ux = Math.cos(angle + bearing), uy = Math.sin(angle + bearing);
            double edge = 0;
            while (edge < 60 && surface.contains(center.plus(ux * edge, uy * edge))) edge += 0.5;
            if (edge >= 60) continue;
            for (double offset : OFFSETS) {
                Position2d candidate = center.plus(ux * (edge + offset), uy * (edge + offset));
                if (supportedLie(geometry.surfaceAt(candidate))
                        && candidate.distanceTo(model.cupPosition()) >= 8
                        && candidate.distanceTo(model.cupPosition()) <= 70) result.add(candidate);
            }
        }
        for (TerrainRegion bunker : geometry.regions()) {
            if (bunker.surface() != Surface.BUNKER) continue;
            double x = bunker.boundary().stream().mapToDouble(Position2d::x).average().orElseThrow();
            double y = bunker.boundary().stream().mapToDouble(Position2d::y).average().orElseThrow();
            Position2d centerPoint = new Position2d(x, y);
            for (Position2d vertex : bunker.boundary()) {
                Position2d candidate = new Position2d((centerPoint.x() + vertex.x()) / 2,
                        (centerPoint.y() + vertex.y()) / 2);
                if (geometry.surfaceAt(candidate) == Surface.BUNKER
                        && candidate.distanceTo(model.cupPosition()) <= 65
                        && distanceToGreen(candidate, surface.footprint()) <= 40) {
                    result.add(candidate);
                    break;
                }
            }
        }
        return result;
    }

    private static double distanceToGreen(Position2d point, List<Position2d> boundary) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int index = 0; index < boundary.size(); index++) {
            Position2d a = boundary.get(index), b = boundary.get((index + 1) % boundary.size());
            double dx = b.x() - a.x(), dy = b.y() - a.y();
            double fraction = Math.max(0, Math.min(1, ((point.x() - a.x()) * dx + (point.y() - a.y()) * dy)
                    / (dx * dx + dy * dy)));
            nearest = Math.min(nearest, Math.hypot(point.x() - a.x() - fraction * dx,
                    point.y() - a.y() - fraction * dy));
        }
        return nearest;
    }

    private static ClubId suitableClub(Attributes skills, GolferState state,
                                       Map<Surface, List<ShotGuidance.ClubReach>> reaches, Surface lie,
                                       ShotFamily family, double distance) {
        List<ClubId> choices = family == ShotFamily.BUNKER ? List.of(ClubId.SAND_WEDGE)
                : List.of(ClubId.GAP_WEDGE, ClubId.PITCHING_WEDGE, ClubId.SAND_WEDGE);
        for (ClubId club : choices) {
            if (!ShotFamilyEligibility.evaluate(lie, ClubSpec.of(club), family).allowed()) continue;
            double reach = reaches.computeIfAbsent(lie, surface -> ShotGuidance.clubReaches(skills, surface)).stream()
                    .filter(candidate -> candidate.club() == club).findFirst().orElseThrow().normalReach()
                    * (1 + state.equipmentPower())
                    * ShotExecutionProfile.derive(lie, ClubSpec.of(club), family,
                            skills, Environment.calm()).effectiveCarryCapMultiplier();
            if (distance <= reach + (family == ShotFamily.CHIP ? 6 : 2)) return club;
        }
        return null;
    }

    private static boolean premiumFamily(EnvironmentClassification classification) {
        return classification == EnvironmentClassification.PARKLAND
                || classification == EnvironmentClassification.LINKS || classification == EnvironmentClassification.HEATHLAND
                || classification == EnvironmentClassification.DESERT;
    }

    private static boolean supportedLie(Surface lie) {
        return lie == Surface.FRINGE || lie == Surface.FIRST_CUT || lie == Surface.PRIMARY_ROUGH
                || lie == Surface.FAIRWAY || lie == Surface.BUNKER;
    }
}
