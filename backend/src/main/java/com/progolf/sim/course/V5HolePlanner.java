package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded V5 candidate generator and deterministic whole-course selector. */
final class V5HolePlanner {
    private static final long PROFILE_SALT = 0x563541524348L;
    private static final long CANDIDATE_SALT = 0x563543414e44L;
    private static final int CANDIDATES_PER_BRIEF = 7;

    private V5HolePlanner() { }

    static CourseArchitectureProfile profileFor(long courseSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(courseSeed, PROFILE_SALT));
        return new CourseArchitectureProfile(rng.nextDouble(), rng.nextDouble(), rng.nextDouble(), rng.nextDouble(), rng.nextDouble());
    }

    static List<SelectedCandidate> select(CoursePlan coursePlan, CourseArchitectureProfile profile, long courseSeed,
                                          List<Double> lengths, List<Double> fairwayHalves) {
        List<SelectedCandidate> selected = new ArrayList<>(18);
        for (int index = 0; index < coursePlan.briefs().size(); index++) {
            HoleBrief brief = coursePlan.briefs().get(index);
            List<Candidate> candidates = candidates(brief, profile, lengths.get(index), fairwayHalves.get(index),
                    Seeds.deriveSeed(courseSeed, brief.number()));
            Candidate best = candidates.stream().max(Comparator.comparingDouble(candidate -> candidate.score
                    - repetitionPenalty(candidate, selected) + compositionAdjustment(candidate, selected, profile,
                    coursePlan.briefs().size()))).orElseThrow();
            selected.add(new SelectedCandidate(best.plan, best.ordinal));
        }
        return List.copyOf(selected);
    }

    private static List<Candidate> candidates(HoleBrief brief, CourseArchitectureProfile profile, double length,
                                              double fairwayHalf, long holeSeed) {
        List<Candidate> result = new ArrayList<>(CANDIDATES_PER_BRIEF);
        for (int ordinal = 0; ordinal < CANDIDATES_PER_BRIEF; ordinal++) {
            Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, CANDIDATE_SALT + ordinal));
            RoutingForm form = routingForm(brief, ordinal);
            double movement = 18.0 + profile.routingMovement() * 34.0 + rng.nextDouble() * 12.0;
            List<Position2d> anchors = new ArrayList<>(form == RoutingForm.DOUBLE_DOGLEG ? 2 : form == RoutingForm.STRAIGHT ? 0 : 1);
            double finalX = 0.0;
            int turns = form == RoutingForm.DOUBLE_DOGLEG ? 2 : form == RoutingForm.STRAIGHT ? 0 : 1;
            for (int turn = 0; turn < turns; turn++) {
                double fraction = turns == 1 ? 0.56 : (turn == 0 ? 0.33 : 0.67);
                double sign = (turn == 0 || rng.nextDouble() < 0.55) ? (rng.nextDouble() < 0.5 ? -1.0 : 1.0)
                        : -Math.signum(finalX == 0.0 ? 1.0 : finalX);
                double scale = form == RoutingForm.GENTLE ? 0.34 : turns == 2 && turn == 0 ? 0.66 : 1.0;
                finalX += sign * movement * scale;
                anchors.add(new Position2d(finalX, length * fraction));
            }
            finalX *= 0.30 + rng.nextDouble() * 0.25;
            ArchitectureRoute route = new ArchitectureRoute(new Position2d(0.0, 0.0), anchors,
                    new Position2d(finalX, length));
            List<FairwayWidthStation> stations = stations(route, fairwayHalf, profile, ordinal, rng);
            Position2d direction = finalDirection(route);
            List<Position2d> green = greenFootprint(route.greenCenter(), direction, profile, rng);
            double signature = signature(route, stations, green, ordinal);
            double score = 42.0 + profile.widthRhythm() * 8.0
                    + (brief.archetype() == StrategicArchetype.BALANCED ? 3.0 : 6.0) + rng.nextDouble();
            result.add(new Candidate(new HoleArchitecturePlan(form, route, stations, green, direction, signature), score, ordinal));
        }
        return result;
    }

    private static List<FairwayWidthStation> stations(ArchitectureRoute route, double base,
                                                       CourseArchitectureProfile profile, int ordinal, Rng rng) {
        double total = route.length();
        double asymmetry = (profile.lateralAsymmetry() * 0.38 + 0.06) * (ordinal % 2 == 0 ? 1.0 : -1.0);
        List<FairwayWidthStation> result = new ArrayList<>();
        for (double fraction : List.of(0.0, 0.20, 0.47, 0.67, 0.84, 1.0)) {
            double rhythm = StrictMath.sin((fraction * 2.0 + ordinal * 0.37) * StrictMath.PI)
                    * (0.18 + profile.widthRhythm() * 0.22);
            double landingWiden = fraction > 0.42 && fraction < 0.70 ? 0.12 + rng.nextDouble() * 0.11 : 0.0;
            double left = Math.max(10.0, base * (1.0 + rhythm - asymmetry + landingWiden));
            double right = Math.max(10.0, base * (1.0 - rhythm + asymmetry + landingWiden * 0.45));
            result.add(new FairwayWidthStation(total * fraction, left, right));
        }
        return result;
    }

    private static Position2d finalDirection(ArchitectureRoute route) {
        List<Position2d> points = route.points();
        Position2d from = points.get(points.size() - 2);
        Position2d to = route.greenCenter();
        double length = from.distanceTo(to);
        return new Position2d((to.x() - from.x()) / length, (to.y() - from.y()) / length);
    }

    private static List<Position2d> greenFootprint(Position2d center, Position2d direction,
                                                     CourseArchitectureProfile profile, Rng rng) {
        double major = 11.0 + rng.nextDouble() * 7.0;
        double minor = 9.0 + rng.nextDouble() * 6.0;
        double defense = 0.10 + profile.greenDefence() * 0.18;
        Position2d right = new Position2d(direction.y(), -direction.x());
        List<Position2d> points = new ArrayList<>();
        double primaryPhase = rng.nextDouble() * 2.0 * StrictMath.PI;
        double secondaryPhase = rng.nextDouble() * 2.0 * StrictMath.PI;
        for (int i = 0; i < 18; i++) {
            double theta = 2.0 * StrictMath.PI * i / 18.0;
            double wobble = 1.0 + defense * (0.70 * StrictMath.sin(2.0 * theta + primaryPhase)
                    + 0.30 * StrictMath.sin(3.0 * theta + secondaryPhase));
            points.add(center.plus(direction.x() * StrictMath.cos(theta) * major * wobble
                    + right.x() * StrictMath.sin(theta) * minor * wobble,
                    direction.y() * StrictMath.cos(theta) * major * wobble
                            + right.y() * StrictMath.sin(theta) * minor * wobble));
        }
        return List.copyOf(points);
    }

    private static double signature(ArchitectureRoute route, List<FairwayWidthStation> stations,
                                    List<Position2d> green, int ordinal) {
        double widths = stations.stream().mapToDouble(station -> station.rightHalfWidth() - station.leftHalfWidth()).sum();
        return route.length() / route.teeOrigin().distanceTo(route.greenCenter()) + route.intermediateAnchors().size() * 0.31
                + widths / 400.0 + green.getFirst().distanceTo(route.greenCenter()) / 100.0 + ordinal * 0.013;
    }

    private static double repetitionPenalty(Candidate candidate, List<SelectedCandidate> selected) {
        double penalty = 0.0;
        for (SelectedCandidate prior : selected) {
            double difference = Math.abs(candidate.plan.structuralSignature() - prior.plan.structuralSignature());
            if (difference < 0.12) penalty += (0.12 - difference) * 95.0;
        }
        return penalty;
    }

    private static RoutingForm routingForm(HoleBrief brief, int ordinal) {
        return switch (ordinal) {
            case 0, 4 -> RoutingForm.STRAIGHT;
            case 1, 5 -> RoutingForm.GENTLE;
            case 2, 6 -> RoutingForm.DOGLEG;
            default -> brief.par() == 3 ? RoutingForm.GENTLE : RoutingForm.DOUBLE_DOGLEG;
        };
    }

    private static double compositionAdjustment(Candidate candidate, List<SelectedCandidate> selected,
                                               CourseArchitectureProfile profile, int holeCount) {
        RoutingForm form = candidate.plan.routingForm();
        int selectedCount = (int) selected.stream().filter(prior -> prior.plan.routingForm() == form).count();
        int target = targetFor(form, profile, holeCount);
        if (selectedCount < target) return 20.0 + (target - selectedCount) * 2.0;
        return -30.0 - (selectedCount - target) * 9.0;
    }

    private static int targetFor(RoutingForm form, CourseArchitectureProfile profile, int holeCount) {
        int doubleDoglegs = 1 + (int) StrictMath.floor(profile.routingMovement() * 2.0);
        int gentle = 4 + (int) StrictMath.round(profile.routingMovement() * 2.0);
        int doglegs = 6 + (int) StrictMath.floor(profile.routingMovement() * 2.0);
        return switch (form) {
            case DOUBLE_DOGLEG -> Math.min(doubleDoglegs, Math.max(1, holeCount / 6));
            case GENTLE -> gentle;
            case DOGLEG -> doglegs;
            case STRAIGHT -> Math.max(0, holeCount - doubleDoglegs - gentle - doglegs);
        };
    }

    record SelectedCandidate(HoleArchitecturePlan plan, int ordinal) { }
    private record Candidate(HoleArchitecturePlan plan, double score, int ordinal) { }
}
