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
        return select(coursePlan, profile, courseSeed, lengths, fairwayHalves, false);
    }

    static List<SelectedCandidate> select(CoursePlan coursePlan, CourseArchitectureProfile profile, long courseSeed,
                                          List<Double> lengths, List<Double> fairwayHalves, boolean refined) {
        return select(coursePlan, profile, courseSeed, lengths, fairwayHalves, refined, false);
    }

    /** Development-only selection mode: continuous candidate relationships replace fixed form quotas. */
    static List<SelectedCandidate> select(CoursePlan coursePlan, CourseArchitectureProfile profile, long courseSeed,
                                          List<Double> lengths, List<Double> fairwayHalves, boolean refined,
                                          boolean principlesDriven) {
        List<SelectedCandidate> selected = new ArrayList<>(18);
        for (int index = 0; index < coursePlan.briefs().size(); index++) {
            HoleBrief brief = coursePlan.briefs().get(index);
            List<Candidate> candidates = candidates(brief, profile, lengths.get(index), fairwayHalves.get(index),
                    Seeds.deriveSeed(courseSeed, brief.number()), refined, principlesDriven);
            Candidate best = candidates.stream().max(Comparator.comparingDouble(candidate -> candidate.score
                    - repetitionPenalty(candidate, selected) + (principlesDriven
                    ? principleDiversityAdjustment(candidate, selected, brief) : compositionAdjustment(candidate, selected, profile,
                    coursePlan.briefs().size())))).orElseThrow();
            selected.add(new SelectedCandidate(best.plan, best.ordinal, brief.par()));
        }
        return List.copyOf(selected);
    }

    private static List<Candidate> candidates(HoleBrief brief, CourseArchitectureProfile profile, double length,
                                              double fairwayHalf, long holeSeed, boolean refined,
                                              boolean principlesDriven) {
        int candidateCount = principlesDriven ? 11 : CANDIDATES_PER_BRIEF;
        List<Candidate> result = new ArrayList<>(candidateCount);
        for (int ordinal = 0; ordinal < candidateCount; ordinal++) {
            Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, CANDIDATE_SALT + ordinal));
            RoutingForm form = principlesDriven ? null : routingForm(brief, ordinal);
            double movement = principlesDriven ? 48.0 + profile.routingMovement() * 62.0 + rng.nextDouble() * 28.0
                    : refined ? 29.0 + profile.routingMovement() * 47.0 + rng.nextDouble() * 17.0
                    : 18.0 + profile.routingMovement() * 34.0 + rng.nextDouble() * 12.0;
            int turns = principlesDriven ? turnsFor(brief, profile, length, rng)
                    : form == RoutingForm.DOUBLE_DOGLEG ? 2 : form == RoutingForm.STRAIGHT ? 0 : 1;
            List<Position2d> anchors = new ArrayList<>(turns);
            double finalX = 0.0;
            double firstSide = rng.nextDouble() < 0.5 ? -1.0 : 1.0;
            for (int turn = 0; turn < turns; turn++) {
                double fraction = principlesDriven ? turnFraction(turn, turns, rng)
                        : turns == 1 ? 0.56 : (turn == 0 ? 0.33 : 0.67);
                double sign = principlesDriven ? (turn == 0 ? firstSide : rng.nextDouble() < .72 ? -firstSide : firstSide)
                        : (turn == 0 || rng.nextDouble() < 0.55) ? (rng.nextDouble() < 0.5 ? -1.0 : 1.0)
                        : -Math.signum(finalX == 0.0 ? 1.0 : finalX);
                double scale = principlesDriven ? .58 + rng.nextDouble() * .47
                        : form == RoutingForm.GENTLE ? 0.34 : turns == 2 && turn == 0 ? 0.66 : 1.0;
                finalX += sign * movement * scale;
                anchors.add(new Position2d(finalX, length * fraction));
            }
            finalX *= principlesDriven ? .16 + rng.nextDouble() * .34
                    : refined ? 0.56 + rng.nextDouble() * 0.27 : 0.30 + rng.nextDouble() * 0.25;
            ArchitectureRoute route = new ArchitectureRoute(new Position2d(0.0, 0.0), anchors,
                    new Position2d(finalX, length));
            // A candidate with two geometric inflections but fewer than two genuine landing/turn decisions is not
            // an honest double dogleg. Keep its strongest single route relationship instead of merely relabelling it.
            if (principlesDriven && anchors.size() == 2 && PreviewCourseMetrics.meaningfulTurns(route, brief.par()) < 2) {
                anchors.removeLast();
                route = new ArchitectureRoute(new Position2d(0.0, 0.0), anchors, new Position2d(finalX, length));
            }
            if (principlesDriven) form = formFor(route, brief);
            double corridorBase = principlesDriven ? Math.clamp(fairwayHalf * 1.15, 18.0, 25.0)
                    : refined ? fairwayHalf * 1.12 : fairwayHalf;
            List<FairwayWidthStation> stations = stations(route, corridorBase,
                    profile, ordinal, rng, refined, principlesDriven);
            Position2d direction = finalDirection(route);
            List<Position2d> green = greenFootprint(route.greenCenter(), direction, profile, rng, brief.par(), refined);
            double signature = signature(route, stations, green, ordinal);
            double score = 42.0 + profile.widthRhythm() * 8.0
                    + (brief.archetype() == StrategicArchetype.BALANCED ? 3.0 : 6.0) + rng.nextDouble();
            if (principlesDriven) score += routingSuitability(brief, route);
            result.add(new Candidate(new HoleArchitecturePlan(form, route, stations, green, direction, signature), score, ordinal));
        }
        return result;
    }

    private static List<FairwayWidthStation> stations(ArchitectureRoute route, double base,
                                                       CourseArchitectureProfile profile, int ordinal, Rng rng,
                                                       boolean refined, boolean principlesDriven) {
        double total = route.length();
        double asymmetry = (profile.lateralAsymmetry() * 0.38 + 0.06) * (ordinal % 2 == 0 ? 1.0 : -1.0);
        List<FairwayWidthStation> result = new ArrayList<>();
        for (double fraction : refined ? List.of(0.0, 0.17, 0.38, 0.55, 0.72, 0.87, 1.0)
                : List.of(0.0, 0.20, 0.47, 0.67, 0.84, 1.0)) {
            double rhythm = StrictMath.sin((fraction * 2.0 + ordinal * 0.37) * StrictMath.PI)
                    * (0.18 + profile.widthRhythm() * 0.22);
            double landingWiden = principlesDriven && fraction > .18 && fraction < .83
                    ? (rng.nextDouble() < .58 ? .12 + rng.nextDouble() * .34 : -.10 * rng.nextDouble())
                    : refined
                    ? (fraction > 0.35 && fraction < 0.73 ? 0.31 + rng.nextDouble() * 0.30 : 0.0)
                    : (fraction > 0.42 && fraction < 0.70 ? 0.12 + rng.nextDouble() * 0.11 : 0.0);
            if (refined && fraction == 0.87) rhythm -= .14;
            double minimum = principlesDriven ? 17.0 : 10.0;
            double left = Math.max(minimum, base * (1.0 + rhythm - asymmetry + landingWiden));
            double right = Math.max(minimum, base * (1.0 - rhythm + asymmetry + landingWiden * 0.45));
            result.add(new FairwayWidthStation(total * fraction, left, right));
        }
        return result;
    }

    private static int turnsFor(HoleBrief brief, CourseArchitectureProfile profile, double length, Rng rng) {
        if (brief.par() == 3) return 0;
        double complexity = profile.routingMovement() * .44 + (brief.archetype() == StrategicArchetype.POSITIONAL ? .15 : 0.0)
                + (brief.par() == 5 ? .18 : length > 470.0 ? .10 : 0.0) + rng.nextDouble() * .50;
        if (brief.par() == 5 && complexity > .83 || brief.par() == 4 && length > 465.0 && complexity > .98) return 2;
        return complexity > .47 ? 1 : 0;
    }

    private static double turnFraction(int turn, int turns, Rng rng) {
        if (turns == 1) return .38 + rng.nextDouble() * .30;
        return turn == 0 ? .25 + rng.nextDouble() * .16 : .58 + rng.nextDouble() * .17;
    }

    private static RoutingForm formFor(ArchitectureRoute route, HoleBrief brief) {
        int meaningful = PreviewCourseMetrics.meaningfulTurns(route, brief.par());
        if (meaningful > 1) return RoutingForm.DOUBLE_DOGLEG;
        if (meaningful == 1) return RoutingForm.DOGLEG;
        return route.intermediateAnchors().isEmpty() ? RoutingForm.STRAIGHT : RoutingForm.GENTLE;
    }

    private static double routingSuitability(HoleBrief brief, ArchitectureRoute route) {
        int meaningful = PreviewCourseMetrics.meaningfulTurns(route, brief.par());
        if (brief.par() == 3) return route.intermediateAnchors().isEmpty() ? 3.0 : -8.0;
        // Let simple par 4/5 holes compete on their own merits. Whole-course selection—not a per-hole mandate—
        // applies additional pressure only once the course begins to read as a run of corridors.
        return meaningful * .45 + (route.intermediateAnchors().isEmpty() ? .75 : 0.0);
    }

    private static double principleDiversityAdjustment(Candidate candidate, List<SelectedCandidate> selected,
                                                       HoleBrief brief) {
        if (selected.isEmpty()) return 0.0;
        double chordRatio = candidate.plan.route().length()
                / candidate.plan.route().teeOrigin().distanceTo(candidate.plan.route().greenCenter());
        double widthImbalance = candidate.plan.widthStations().stream().mapToDouble(station ->
                station.leftHalfWidth() - station.rightHalfWidth()).average().orElse(0.0);
        double penalty = 0.0;
        for (SelectedCandidate prior : selected) {
            double priorChord = prior.plan.route().length()
                    / prior.plan.route().teeOrigin().distanceTo(prior.plan.route().greenCenter());
            double priorImbalance = prior.plan.widthStations().stream().mapToDouble(station ->
                    station.leftHalfWidth() - station.rightHalfWidth()).average().orElse(0.0);
            if (StrictMath.abs(chordRatio - priorChord) < .035) penalty += 5.0;
            if (StrictMath.abs(widthImbalance - priorImbalance) < 1.6) penalty += 2.5;
        }
        if (brief.par() >= 4) {
            long comparable = selected.stream().filter(prior -> prior.par >= 4).count();
            long straightforward = selected.stream().filter(prior -> prior.par >= 4
                    && PreviewCourseMetrics.meaningfulTurns(prior.plan.route(), prior.par) == 0).count();
            boolean candidateTurns = PreviewCourseMetrics.meaningfulTurns(candidate.plan.route(), brief.par()) > 0;
            // This is a soft pressure against a course becoming corridor-heavy, not a required form quota:
            // simple long holes remain legal when their other relationships score well.
            double corridorExcess = straightforward - (comparable + 1) * .30;
            if (!candidateTurns && corridorExcess > -.25) penalty += 16.0 + corridorExcess * 12.0;
            if (candidateTurns && corridorExcess > 0.0) penalty -= Math.min(16.0, corridorExcess * 8.0);
        }
        return -penalty;
    }

    private static Position2d finalDirection(ArchitectureRoute route) {
        List<Position2d> points = route.points();
        Position2d from = points.get(points.size() - 2);
        Position2d to = route.greenCenter();
        double length = from.distanceTo(to);
        return new Position2d((to.x() - from.x()) / length, (to.y() - from.y()) / length);
    }

    private static List<Position2d> greenFootprint(Position2d center, Position2d direction,
                                                     CourseArchitectureProfile profile, Rng rng, int par, boolean refined) {
        // Modern putting surfaces have room for several pin positions and are not circular end-caps. Their
        // orientation can sit slightly across the approach, while remaining legible from the connected route.
        double major = refined ? (par == 3 ? 22.0 : par == 4 ? 24.0 : 26.0) + rng.nextDouble() * 7.0
                : 16.0 + rng.nextDouble() * 8.0;
        double minor = refined ? (par == 3 ? 17.0 : par == 4 ? 18.0 : 19.0) + rng.nextDouble() * 5.0
                : 12.5 + rng.nextDouble() * 7.0;
        double defence = 0.07 + profile.greenDefence() * 0.13;
        double rotation = (rng.nextDouble() * 2.0 - 1.0) * (refined ? .55 + profile.lateralAsymmetry() * .52
                : .18 + profile.lateralAsymmetry() * .30);
        double cos = StrictMath.cos(rotation);
        double sin = StrictMath.sin(rotation);
        Position2d longAxis = new Position2d(direction.x() * cos - direction.y() * sin,
                direction.x() * sin + direction.y() * cos);
        Position2d right = new Position2d(longAxis.y(), -longAxis.x());
        List<Position2d> points = new ArrayList<>();
        double primaryPhase = rng.nextDouble() * 2.0 * StrictMath.PI;
        double secondaryPhase = rng.nextDouble() * 2.0 * StrictMath.PI;
        for (int i = 0; i < 22; i++) {
            double theta = 2.0 * StrictMath.PI * i / 22.0;
            double wobble = 1.0 + (refined ? defence * 2.20 : defence)
                    * (0.62 * StrictMath.sin(2.0 * theta + primaryPhase)
                    + 0.23 * StrictMath.sin(3.0 * theta + secondaryPhase)
                    + 0.15 * StrictMath.sin(theta - primaryPhase));
            points.add(center.plus(longAxis.x() * StrictMath.cos(theta) * major * wobble
                    + right.x() * StrictMath.sin(theta) * minor * wobble,
                    longAxis.y() * StrictMath.cos(theta) * major * wobble
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

    record SelectedCandidate(HoleArchitecturePlan plan, int ordinal, int par) { }
    private record Candidate(HoleArchitecturePlan plan, double score, int ordinal) { }
}

