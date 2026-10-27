package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compiles a V5 plan into the canonical terrain shared by play and rendering. */
final class V5GeometryGenerator {
    private static final double CUT = CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA;
    private static final double ROUGH = CourseGenConstants.FAIRWAY_ROUGH_EXTRA;
    private static final double DEEP = CourseGenConstants.FAIRWAY_DEEP_EXTRA;
    private static final double SAMPLE_SPACING = 10.0;

    private V5GeometryGenerator() { }

    static CourseGeometry generate(HoleArchitecturePlan plan, HoleBrief brief, CourseDesignProfile design,
                                   CourseArchitectureProfile architecture, EnvironmentClassification environment,
                                   EnvironmentHoleCharacter environmentCharacter, long holeSeed, HazardPlan inheritedHazards) {
        List<RouteSample> routeSamples = samples(plan);
        GreenComplex greenComplex = greenComplex(plan, holeSeed);
        List<RouteSample> landscapeSamples = extendForGreenComplex(routeSamples, plan, greenComplex);
        List<RouteSample> fairwaySamples = through(routeSamples, plan.route().length() - 48.0, plan);
        List<TerrainRegion> regions = new ArrayList<>();
        // Deep and primary rough continue around and beyond the green. The fairway deliberately stops before
        // the complex; a broad short-grass approach, collar and green then take over on their own footprints.
        regions.add(new TerrainRegion(Surface.DEEP_ROUGH, corridor(landscapeSamples, DEEP, DEEP)));
        regions.add(new TerrainRegion(Surface.PRIMARY_ROUGH, corridor(landscapeSamples, ROUGH, ROUGH)));
        regions.add(new TerrainRegion(Surface.FIRST_CUT, corridor(routeSamples, CUT, CUT)));
        regions.add(new TerrainRegion(Surface.FAIRWAY, corridor(fairwaySamples, 0.0, 0.0)));
        regions.add(new TerrainRegion(Surface.FIRST_CUT, greenComplex.approachApron()));
        regions.add(new TerrainRegion(Surface.FRINGE, greenComplex.collar()));
        regions.add(new TerrainRegion(Surface.GREEN, counterClockwise(plan.greenFootprint())));
        addArchitectureLandforms(regions, plan, brief, design, architecture, environment, environmentCharacter,
                holeSeed, inheritedHazards);
        return new CourseGeometry(plan.route().teeOrigin(), plan.route().greenCenter(),
                corridor(landscapeSamples, DEEP + 17.0, DEEP + 17.0), regions);
    }

    static CourseGeometry generate(HoleArchitecturePlan plan, HoleBrief brief, CourseDesignProfile design,
                                   CourseArchitectureProfile architecture, EnvironmentClassification environment,
                                   EnvironmentHoleCharacter environmentCharacter, long holeSeed, HazardPlan inheritedHazards,
                                   boolean refined) {
        return generate(plan, brief, design, architecture, environment, environmentCharacter, holeSeed,
                inheritedHazards, refined, null);
    }

    /** Optional development-preview strategy is compiled into the same canonical terrain used by the evaluator. */
    static CourseGeometry generate(HoleArchitecturePlan plan, HoleBrief brief, CourseDesignProfile design,
                                   CourseArchitectureProfile architecture, EnvironmentClassification environment,
                                   EnvironmentHoleCharacter environmentCharacter, long holeSeed, HazardPlan inheritedHazards,
                                   boolean refined, ArchitectureStrategyPlan previewStrategy) {
        if (!refined) return generate(plan, brief, design, architecture, environment, environmentCharacter,
                holeSeed, inheritedHazards);
        List<RouteSample> routeSamples = samples(plan);
        GreenComplex complex = refinedGreenComplex(plan, holeSeed);
        List<RouteSample> landscape = refinedLandscapeSamples(routeSamples, plan, complex, holeSeed);
        List<RouteSample> fairway = through(routeSamples, plan.route().length() - 50.0, plan);
        List<TerrainRegion> regions = new ArrayList<>();
        double finishAt = plan.route().length();
        regions.add(new TerrainRegion(Surface.DEEP_ROUGH, refinedCorridor(landscape, 3, holeSeed, finishAt, true)));
        regions.add(new TerrainRegion(Surface.PRIMARY_ROUGH, refinedCorridor(landscape, 2, holeSeed, finishAt, true)));
        regions.add(new TerrainRegion(Surface.FIRST_CUT, refinedCorridor(routeSamples, 1, holeSeed, finishAt, false)));
        regions.add(new TerrainRegion(Surface.FAIRWAY, refinedCorridor(fairway, 0, holeSeed, finishAt, false)));
        regions.add(new TerrainRegion(Surface.FIRST_CUT, complex.approachApron()));
        addRefinedRecovery(regions, plan, complex, holeSeed);
        regions.add(new TerrainRegion(Surface.FRINGE, complex.collar()));
        regions.add(new TerrainRegion(Surface.GREEN, counterClockwise(plan.greenFootprint())));
        addArchitectureLandforms(regions, plan, brief, design, architecture, environment, environmentCharacter,
                holeSeed, inheritedHazards, true, previewStrategy);
        return new CourseGeometry(plan.route().teeOrigin(), plan.route().greenCenter(),
                refinedBoundary(landscape, regions, holeSeed, finishAt), regions);
    }

    /** The outer edge makes room only where a placed bunker needs it, rather than flaring every finish. */
    private static List<Position2d> refinedBoundary(List<RouteSample> landscape, List<TerrainRegion> regions,
                                                     long holeSeed, double finishAt) {
        double phase = Math.floorMod(Seeds.deriveSeed(holeSeed, 0x5635434f4e544fL), 10_000) / 10_000.0 * 6.283185307179586;
        List<RouteSample> adjusted = new ArrayList<>();
        List<Position2d> bunkerEdges = regions.stream().filter(region -> region.surface() == Surface.BUNKER)
                .flatMap(region -> region.boundary().stream()).toList();
        for (RouteSample sample : landscape) {
            double left = sample.left;
            double right = sample.right;
            double currentLeft = refinedHalfWidth(left, sample.distance, phase, true, 4, finishAt);
            double currentRight = refinedHalfWidth(right, sample.distance, phase, false, 4, finishAt);
            for (Position2d edge : bunkerEdges) {
                if (Math.abs(edge.y() - sample.point.y()) > 35.0) continue;
                double offset = edge.x() - sample.point.x();
                if (offset < 0.0) left = Math.max(left, sample.left + (-offset + 12.0 - currentLeft));
                else right = Math.max(right, sample.right + (offset + 12.0 - currentRight));
            }
            adjusted.add(new RouteSample(sample.distance, sample.point, sample.tangent, left, right));
        }
        return refinedCorridor(adjusted, 4, holeSeed, finishAt, true);
    }

    private static List<RouteSample> refinedLandscapeSamples(List<RouteSample> routeSamples, HoleArchitecturePlan plan,
                                                               GreenComplex complex, long holeSeed) {
        List<RouteSample> result = new ArrayList<>(routeSamples);
        Position2d center = plan.route().greenCenter();
        Position2d direction = plan.approachDirection();
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x56355245434f56L));
        double left = complex.lateralExtent() * .77 + 3.0 + rng.nextDouble() * 5.0;
        double right = complex.lateralExtent() * .77 + 3.0 + rng.nextDouble() * 5.0;
        for (double fraction : List.of(.32, .67, 1.0)) {
            double behind = complex.backExtent() * fraction;
            Position2d point = center.plus(direction.x() * behind, direction.y() * behind);
            result.add(new RouteSample(plan.route().length() + behind, point, direction,
                    left * (1.0 - .18 * fraction), right * (1.0 - .15 * fraction)));
        }
        return List.copyOf(result);
    }

    /** Each side and each successive surface band changes independently, while staying nested. */
    private static List<Position2d> refinedCorridor(List<RouteSample> samples, int band, long holeSeed,
                                                     double finishAt, boolean roundBack) {
        double phase = Math.floorMod(Seeds.deriveSeed(holeSeed, 0x5635434f4e544fL), 10_000) / 10_000.0 * 6.283185307179586;
        List<Position2d> right = new ArrayList<>();
        List<Position2d> left = new ArrayList<>();
        for (RouteSample sample : samples) {
            double distance = sample.distance;
            double rightWidth = refinedHalfWidth(sample.right, distance, phase, false, band, finishAt);
            double leftWidth = refinedHalfWidth(sample.left, distance, phase, true, band, finishAt);
            right.add(sample.point.plus(rightWidth, 0.0));
            left.add(sample.point.plus(-leftWidth, 0.0));
        }
        List<Position2d> polygon = new ArrayList<>(right);
        if (roundBack) {
            RouteSample end = samples.getLast();
            double rightWidth = refinedHalfWidth(end.right, end.distance, phase, false, band, finishAt);
            double leftWidth = refinedHalfWidth(end.left, end.distance, phase, true, band, finishAt);
            double capDepth = band == 4 ? 25.0 : band == 3 ? 18.0 : 12.0;
            for (int i = 1; i < 8; i++) {
                double angle = StrictMath.PI * i / 8.0;
                double cosine = StrictMath.cos(angle);
                polygon.add(end.point.plus((cosine >= 0.0 ? rightWidth : leftWidth) * cosine,
                        capDepth * StrictMath.sin(angle) + 6.0 * cosine));
            }
        }
        for (int i = left.size() - 1; i >= 0; i--) polygon.add(left.get(i));
        return polygon;
    }

    private static double refinedHalfWidth(double fairwayHalf, double distance, double phase, boolean left,
                                            int band, double finishAt) {
        double side = left ? 1.0 : -1.0;
        double meander = 4.6 * StrictMath.sin(distance * .018 + phase + side * .9)
                + 2.3 * StrictMath.sin(distance * .037 + phase * .47 + side * 1.4);
        double width = Math.max(11.0, fairwayHalf + meander);
        double finish = Math.clamp((distance - (finishAt - 75.0)) / 75.0, 0.0, 1.0);
        double surround = 1.0 - .37 * finish;
        if (band >= 1) width += surround * (5.5 + 1.6 * StrictMath.sin(distance * .026 + phase + side));
        if (band >= 2) width += surround * (11.5 + 3.6 * StrictMath.sin(distance * .013 + phase * 1.3 - side));
        if (band >= 3) width += surround * (13.0 + 4.5 * StrictMath.sin(distance * .021 + phase * .7 + side * 2.0));
        if (band >= 4) width += surround * (20.0 + 5.5 * StrictMath.sin(distance * .016 + phase * 1.8 + side * .3));
        return width;
    }

    private static GreenComplex refinedGreenComplex(HoleArchitecturePlan plan, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x563552475245454eL));
        Position2d center = plan.route().greenCenter();
        Position2d approach = plan.approachDirection();
        Position2d right = new Position2d(approach.y(), -approach.x());
        double front = plan.greenFootprint().stream().mapToDouble(point ->
                -(point.x() - center.x()) * approach.x() - (point.y() - center.y()) * approach.y()).max().orElse(20.0);
        double back = plan.greenFootprint().stream().mapToDouble(point ->
                (point.x() - center.x()) * approach.x() + (point.y() - center.y()) * approach.y()).max().orElse(20.0);
        double lateral = plan.greenFootprint().stream().mapToDouble(point -> Math.abs(
                (point.x() - center.x()) * right.x() + (point.y() - center.y()) * right.y())).max().orElse(18.0);
        List<Position2d> collar = new ArrayList<>();
        for (int i = 0; i < plan.greenFootprint().size(); i++) {
            Position2d point = plan.greenFootprint().get(i);
            double dx = point.x() - center.x();
            double dy = point.y() - center.y();
            double radial = StrictMath.hypot(dx, dy);
            double offset = 3.2 + rng.nextDouble() * 1.8;
            collar.add(center.plus(dx * (1.0 + offset / radial), dy * (1.0 + offset / radial)));
        }
        double approachStart = Math.max(62.0, front + 35.0);
        List<Position2d> rightSide = new ArrayList<>();
        List<Position2d> leftSide = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double t = i / 5.0;
            double before = approachStart * (1.0 - t) + front * .52 * t;
            Position2d point = smoothPoint(plan.route(), Math.max(0.0, plan.route().length() - before));
            double width = (lateral * .58) * (1.0 - t) + (lateral * 1.12) * t
                    + 2.5 * StrictMath.sin(t * 4.0 + rng.nextDouble() * .3);
            rightSide.add(point.plus(right.x() * width, right.y() * width));
            leftSide.add(point.plus(-right.x() * width * (.88 + .12 * t), -right.y() * width * (.88 + .12 * t)));
        }
        List<Position2d> apron = new ArrayList<>(rightSide);
        for (int i = leftSide.size() - 1; i >= 0; i--) apron.add(leftSide.get(i));
        return new GreenComplex(counterClockwise(collar), counterClockwise(apron), lateral,
                Math.max(31.0, back + 15.0));
    }

    private static void addRefinedRecovery(List<TerrainRegion> regions, HoleArchitecturePlan plan,
                                           GreenComplex complex, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x56355245434f5645L));
        Position2d center = plan.route().greenCenter();
        Position2d approach = plan.approachDirection();
        Position2d right = new Position2d(approach.y(), -approach.x());
        double side = rng.nextDouble() < .5 ? -1.0 : 1.0;
        if (rng.nextDouble() > .68) return;
        double lateral = complex.lateralExtent();
        double front = 19.0 + rng.nextDouble() * 7.0;
        double back = 20.0 + rng.nextDouble() * 7.0;
        double[] stations = {-1.0, -.55, 0.0, .55, 1.0};
        List<Position2d> inside = new ArrayList<>();
        List<Position2d> outside = new ArrayList<>();
        for (double station : stations) {
            double forward = station < 0.0 ? front * station : back * station;
            double inset = lateral + 4.0 + 2.0 * Math.abs(station);
            double width = 2.5 + 9.0 * (1.0 - station * station);
            Position2d axis = center.plus(approach.x() * forward, approach.y() * forward);
            inside.add(axis.plus(right.x() * side * inset, right.y() * side * inset));
            outside.add(axis.plus(right.x() * side * (inset + width), right.y() * side * (inset + width)));
        }
        List<Position2d> polygon = new ArrayList<>(inside);
        for (int i = outside.size() - 1; i >= 0; i--) polygon.add(outside.get(i));
        // Short-grass run-off remains below tree precedence; a tree cluster may border or cross its outer edge.
        regions.add(new TerrainRegion(Surface.FIRST_CUT, counterClockwise(polygon)));
    }

    private static List<RouteSample> samples(HoleArchitecturePlan plan) {
        ArchitectureRoute route = plan.route();
        List<Double> distances = new ArrayList<>();
        for (double distance = 0.0; distance < route.length(); distance += SAMPLE_SPACING) distances.add(distance);
        distances.add(route.length());
        for (Position2d anchor : route.intermediateAnchors()) distances.add(route.projectDistance(anchor));
        for (FairwayWidthStation station : plan.widthStations()) distances.add(station.routeDistance());
        distances.sort(Comparator.naturalOrder());
        List<RouteSample> result = new ArrayList<>();
        for (double distance : distances) {
            if (!result.isEmpty() && Math.abs(result.getLast().distance - distance) < 0.01) continue;
            result.add(sampleAt(plan, distance));
        }
        return List.copyOf(result);
    }

    private static RouteSample sampleAt(HoleArchitecturePlan plan, double distance) {
        ArchitectureRoute route = plan.route();
        Position2d point = smoothPoint(route, distance);
        Position2d before = smoothPoint(route, Math.max(0.0, distance - 1.0));
        Position2d after = smoothPoint(route, Math.min(route.length(), distance + 1.0));
        double dx = after.x() - before.x();
        double dy = after.y() - before.y();
        double magnitude = StrictMath.hypot(dx, dy);
        if (magnitude < 0.1) throw new IllegalArgumentException("V5 route sample has no tangent");
        return new RouteSample(distance, point, new Position2d(dx / magnitude, dy / magnitude),
                interpolate(plan.widthStations(), distance, true), interpolate(plan.widthStations(), distance, false));
    }

    private static List<RouteSample> through(List<RouteSample> samples, double distance, HoleArchitecturePlan plan) {
        List<RouteSample> result = new ArrayList<>();
        for (RouteSample sample : samples) if (sample.distance < distance - .01) result.add(sample);
        result.add(sampleAt(plan, Math.max(0.0, distance)));
        return List.copyOf(result);
    }

    private static List<RouteSample> extendForGreenComplex(List<RouteSample> samples, HoleArchitecturePlan plan,
                                                            GreenComplex complex) {
        List<RouteSample> result = new ArrayList<>(samples);
        Position2d center = plan.route().greenCenter();
        Position2d direction = plan.approachDirection();
        double ySpan = complex.backExtent();
        double xSpan = complex.lateralExtent() + 27.0;
        double terminal = plan.route().length();
        result.add(new RouteSample(terminal + ySpan * .46, center.plus(direction.x() * ySpan * .46,
                direction.y() * ySpan * .46), direction, xSpan, xSpan));
        result.add(new RouteSample(terminal + ySpan, center.plus(direction.x() * ySpan,
                direction.y() * ySpan), direction, xSpan * .84, xSpan * .84));
        return List.copyOf(result);
    }

    private static GreenComplex greenComplex(HoleArchitecturePlan plan, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x5635475245454eL));
        Position2d center = plan.route().greenCenter();
        Position2d approach = plan.approachDirection();
        Position2d right = new Position2d(approach.y(), -approach.x());
        double frontExtent = plan.greenFootprint().stream().mapToDouble(point ->
                -(point.x() - center.x()) * approach.x() - (point.y() - center.y()) * approach.y()).max().orElse(14.0);
        double backExtent = plan.greenFootprint().stream().mapToDouble(point ->
                (point.x() - center.x()) * approach.x() + (point.y() - center.y()) * approach.y()).max().orElse(16.0);
        double lateral = plan.greenFootprint().stream().mapToDouble(point -> Math.abs(
                (point.x() - center.x()) * right.x() + (point.y() - center.y()) * right.y())).max().orElse(15.0);
        List<Position2d> collar = new ArrayList<>();
        List<Position2d> green = plan.greenFootprint();
        for (int i = 0; i < green.size(); i++) {
            Position2d point = green.get(i);
            double dx = point.x() - center.x();
            double dy = point.y() - center.y();
            double towardApproach = -(dx * approach.x() + dy * approach.y());
            double scale = towardApproach >= 0.0 ? 1.52 : 1.38;
            scale += (rng.nextDouble() - .5) * .055;
            collar.add(center.plus(dx * scale, dy * scale));
        }
        double approachStart = Math.max(38.0, frontExtent + 26.0);
        Position2d start = smoothPoint(plan.route(), Math.max(0.0, plan.route().length() - approachStart));
        Position2d middle = center.plus(-approach.x() * (frontExtent + 10.0), -approach.y() * (frontExtent + 10.0));
        Position2d near = center.plus(-approach.x() * (frontExtent * .52), -approach.y() * (frontExtent * .52));
        double startWidth = Math.max(11.0, lateral * .56);
        double middleWidth = lateral * 1.18;
        double nearWidth = lateral * 1.38;
        List<Position2d> apron = List.of(start.plus(right.x() * startWidth, right.y() * startWidth),
                middle.plus(right.x() * middleWidth, right.y() * middleWidth),
                near.plus(right.x() * nearWidth, right.y() * nearWidth),
                near.plus(-right.x() * nearWidth, -right.y() * nearWidth),
                middle.plus(-right.x() * middleWidth, -right.y() * middleWidth),
                start.plus(-right.x() * startWidth, -right.y() * startWidth));
        return new GreenComplex(counterClockwise(collar), counterClockwise(apron), lateral,
                Math.max(48.0, backExtent + 34.0));
    }

    /** Smooth x over route distance while retaining increasing y, so corridor sides remain simple polygons. */
    private static Position2d smoothPoint(ArchitectureRoute route, double distance) {
        List<Position2d> points = route.points();
        List<Double> knots = new ArrayList<>();
        knots.add(0.0);
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).distanceTo(points.get(i));
            knots.add(total);
        }
        int segment = 0;
        while (segment + 1 < knots.size() - 1 && distance > knots.get(segment + 1)) segment++;
        double start = knots.get(segment);
        double end = knots.get(segment + 1);
        double span = end - start;
        double t = span < 0.01 ? 0.0 : Math.clamp((distance - start) / span, 0.0, 1.0);
        double x0 = points.get(segment).x();
        double x1 = points.get(segment + 1).x();
        double previous = segment == 0 ? x0 : points.get(segment - 1).x();
        double next = segment + 2 == points.size() ? x1 : points.get(segment + 2).x();
        double bound = Math.abs(x1 - x0) * 1.35 + 4.0;
        double m0 = Math.clamp((x1 - previous) * 0.5, -bound, bound);
        double m1 = Math.clamp((next - x0) * 0.5, -bound, bound);
        double t2 = t * t;
        double t3 = t2 * t;
        double x = (2.0 * t3 - 3.0 * t2 + 1.0) * x0 + (t3 - 2.0 * t2 + t) * m0
                + (-2.0 * t3 + 3.0 * t2) * x1 + (t3 - t2) * m1;
        return new Position2d(x, route.pointAt(distance).y());
    }

    private static List<Position2d> corridor(List<RouteSample> samples, double leftExtra, double rightExtra) {
        List<Position2d> right = new ArrayList<>();
        List<Position2d> left = new ArrayList<>();
        for (RouteSample sample : samples) {
            right.add(sample.point.plus(sample.right + rightExtra, 0.0));
            left.add(sample.point.plus(-(sample.left + leftExtra), 0.0));
        }
        List<Position2d> polygon = new ArrayList<>(right);
        for (int i = left.size() - 1; i >= 0; i--) polygon.add(left.get(i));
        return polygon;
    }

    private static double interpolate(List<FairwayWidthStation> stations, double distance, boolean left) {
        FairwayWidthStation before = stations.getFirst();
        for (FairwayWidthStation after : stations) {
            if (after.routeDistance() >= distance) {
                double span = after.routeDistance() - before.routeDistance();
                double t = span < 0.01 ? 0.0 : Math.clamp((distance - before.routeDistance()) / span, 0.0, 1.0);
                double eased = t * t * (3.0 - 2.0 * t);
                double a = left ? before.leftHalfWidth() : before.rightHalfWidth();
                double b = left ? after.leftHalfWidth() : after.rightHalfWidth();
                return a + (b - a) * eased;
            }
            before = after;
        }
        return left ? before.leftHalfWidth() : before.rightHalfWidth();
    }

    private static void addArchitectureLandforms(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                                  CourseDesignProfile design, CourseArchitectureProfile architecture,
                                                  EnvironmentClassification environment, EnvironmentHoleCharacter environmentCharacter,
                                                  long holeSeed,
                                                  HazardPlan inheritedHazards) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x56354c414e4446L));
        addBunkers(regions, plan, brief, architecture, environment, environmentCharacter, rng);
        addTrees(regions, plan, brief, environmentCharacter.treeEnclosure(), rng);
        addWaterIntent(regions, plan, inheritedHazards, rng);
        if (environment == EnvironmentClassification.PARKLAND) addParklandWater(regions, plan, brief, holeSeed);
        if (environment == EnvironmentClassification.WOODLAND
                && rng.nextDouble() < (brief.recoverySeverity() == RecoverySeverity.PENAL ? .34 : .22)) {
            addWoodlandWater(regions, plan, rng);
        }
        if (environmentCharacter.coastalExposure()) addCoastalShoreline(regions, plan, rng);
    }

    private static void addArchitectureLandforms(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                                  CourseDesignProfile design, CourseArchitectureProfile architecture,
                                                  EnvironmentClassification environment, EnvironmentHoleCharacter environmentCharacter,
                                                  long holeSeed, HazardPlan inheritedHazards, boolean refined) {
        addArchitectureLandforms(regions, plan, brief, design, architecture, environment, environmentCharacter,
                holeSeed, inheritedHazards, refined, null);
    }

    private static void addArchitectureLandforms(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                                  CourseDesignProfile design, CourseArchitectureProfile architecture,
                                                  EnvironmentClassification environment, EnvironmentHoleCharacter environmentCharacter,
                                                  long holeSeed, HazardPlan inheritedHazards, boolean refined,
                                                  ArchitectureStrategyPlan previewStrategy) {
        if (!refined) {
            addArchitectureLandforms(regions, plan, brief, design, architecture, environment,
                    environmentCharacter, holeSeed, inheritedHazards);
            return;
        }
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x5635524c414e4446L));
        List<TerrainRegion> water = new ArrayList<>();
        addWaterIntent(water, plan, inheritedHazards, rng);
        if (environment == EnvironmentClassification.PARKLAND) addRefinedParklandWater(water, plan, brief, holeSeed);
        if (environment == EnvironmentClassification.WOODLAND
                && rng.nextDouble() < (brief.recoverySeverity() == RecoverySeverity.PENAL ? .34 : .22)) {
            addWoodlandWater(water, plan, rng);
        }
        if (environmentCharacter.coastalExposure()) addRefinedCoastalShoreline(water, plan, rng);
        for (TerrainRegion candidate : water) if (hazardsHaveRoom(candidate, regions)) regions.add(candidate);
        List<TerrainRegion> trees = new ArrayList<>();
        addTrees(trees, plan, brief, environmentCharacter.treeEnclosure(), rng);
        for (TerrainRegion candidate : trees) {
            if (hazardsHaveRoom(candidate, regions)
                    && !TerrainRegion.strictlyOverlaps(candidate.boundary(), plan.greenFootprint())) regions.add(candidate);
        }
        if (previewStrategy != null && environment == EnvironmentClassification.PARKLAND) {
            addPreviewParklandTreeFields(regions, plan, brief, holeSeed);
        }
        addRefinedBunkers(regions, plan, brief, architecture, environment, holeSeed, rng);
        if (previewStrategy != null) {
            addPreviewTurnBunkerComplexes(regions, plan, brief, environment, holeSeed);
            addPreviewStrategyBunkers(regions, plan, previewStrategy, environment, holeSeed);
        }
    }

    /**
     * A selected landing choice receives only a real, route-relative bunker edge. The target core remains ordinary
     * ground; exposure is subsequently measured by {@code StrategicTargetPlanner} from this emitted geometry.
     */
    private static void addPreviewStrategyBunkers(List<TerrainRegion> regions, HoleArchitecturePlan plan,
                                                   ArchitectureStrategyPlan strategy, EnvironmentClassification environment,
                                                   long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x5354524154454745L));
        for (ArchitectureLandingZone zone : strategy.landingZones()) {
            if (zone.role() == LandingZoneRole.SAFE) continue;
            if (zone.role() == LandingZoneRole.PRIMARY && rng.nextDouble() >= .28) continue;
            Position2d target = zone.center(strategy.route());
            Position2d tangent = tangent(plan.route(), Math.min(plan.route().length() - 45.0, zone.routeDistance()));
            Position2d right = new Position2d(tangent.y(), -tangent.x());
            double side = zone.preferredApproachSide() == PreferredApproachSide.LEFT ? -1.0
                    : zone.preferredApproachSide() == PreferredApproachSide.RIGHT ? 1.0
                    : rng.nextDouble() < .5 ? -1.0 : 1.0;
            double major = environment == EnvironmentClassification.LINKS ? 8.5 + rng.nextDouble() * 3.0
                    : 10.0 + rng.nextDouble() * 4.0;
            double offset = Math.max(9.0, zone.halfWidth() * .70 + major * .30);
            Position2d center = target.plus(right.x() * side * offset, right.y() * side * offset);
            tryAddRefinedBunker(regions, plan, center, major, major * (.62 + rng.nextDouble() * .16),
                    StrictMath.atan2(tangent.y(), tangent.x()), rng, 15);
        }
    }

    /** Parkland receives larger mature-field masses outside the fairway, while Links remains deliberately open. */
    private static void addPreviewParklandTreeFields(List<TerrainRegion> regions, HoleArchitecturePlan plan,
                                                     HoleBrief brief, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x5052455654524545L));
        int fields = 1 + (brief.par() >= 4 && rng.nextDouble() < .68 ? 1 : 0);
        for (int field = 0; field < fields; field++) {
            double distance;
            if (field == 0 && !plan.route().intermediateAnchors().isEmpty()
                    && PreviewCourseMetrics.meaningfulTurns(plan.route(), brief.par()) > 0) {
                distance = plan.route().projectDistance(plan.route().intermediateAnchors().getFirst());
            } else {
                distance = plan.route().length() * (.18 + rng.nextDouble() * .62);
            }
            Position2d point = smoothPoint(plan.route(), distance);
            Position2d tangent = tangent(plan.route(), distance);
            Position2d right = new Position2d(tangent.y(), -tangent.x());
            double side = field == 0 ? (rng.nextDouble() < .5 ? -1.0 : 1.0) : -1.0;
            double width = interpolate(plan.widthStations(), distance, side < 0.0);
            double major = 15.0 + rng.nextDouble() * 10.0;
            Position2d center = point.plus(right.x() * side * (width + major * .70 + 7.0),
                    right.y() * side * (width + major * .70 + 7.0));
            TerrainRegion fieldMass = irregularRegion(Surface.TREES, center, major, major * (.58 + rng.nextDouble() * .18),
                    StrictMath.atan2(tangent.y(), tangent.x()), rng, 17);
            if (hazardsHaveRoom(fieldMass, regions)
                    && !TerrainRegion.strictlyOverlaps(fieldMass.boundary(), plan.greenFootprint())) regions.add(fieldMass);
        }
    }

    /** A material route turn can receive a real, edge-of-landing complex; cosmetic bends receive none. */
    private static void addPreviewTurnBunkerComplexes(List<TerrainRegion> regions, HoleArchitecturePlan plan,
                                                       HoleBrief brief, EnvironmentClassification environment, long holeSeed) {
        if (brief.par() < 4) return;
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x505245565455524eL));
        List<Position2d> points = plan.route().points();
        for (int index = 1; index < points.size() - 1; index++) {
            if (!PreviewCourseMetrics.meaningfulTurn(plan.route(), brief.par(), index)) continue;
            double distance = plan.route().projectDistance(points.get(index));
            Position2d before = points.get(index - 1);
            Position2d turn = points.get(index);
            Position2d after = points.get(index + 1);
            double cross = (turn.x() - before.x()) * (after.y() - turn.y())
                    - (turn.y() - before.y()) * (after.x() - turn.x());
            double side = cross >= 0.0 ? -1.0 : 1.0;
            double approachDistance = Math.max(74.0, distance - (18.0 + rng.nextDouble() * 17.0));
            Position2d point = smoothPoint(plan.route(), approachDistance);
            Position2d tangent = tangent(plan.route(), approachDistance);
            Position2d right = new Position2d(tangent.y(), -tangent.x());
            double width = interpolate(plan.widthStations(), approachDistance, side < 0.0);
            double major = environment == EnvironmentClassification.LINKS ? 10.0 + rng.nextDouble() * 4.0
                    : 13.0 + rng.nextDouble() * 6.0;
            Position2d center = point.plus(right.x() * side * (width - major * .25),
                    right.y() * side * (width - major * .25));
            if (tryAddRefinedBunker(regions, plan, center, major, major * (.58 + rng.nextDouble() * .14),
                    StrictMath.atan2(tangent.y(), tangent.x()), rng, 18) && rng.nextDouble() < .42) {
                Position2d companion = center.plus(tangent.x() * (major * 1.35), tangent.y() * (major * 1.35));
                tryAddRefinedBunker(regions, plan, companion, major * .68, major * .44,
                        StrictMath.atan2(tangent.y(), tangent.x()), rng, 14);
            }
        }
    }

    static boolean hazardsHaveRoom(TerrainRegion candidate, List<TerrainRegion> placed) {
        for (TerrainRegion existing : placed) {
            if (!isHazard(existing.surface())) continue;
            double clearance = switch (candidate.surface()) {
                case WATER -> existing.surface() == Surface.TREES ? 14.0 : 9.0;
                case TREES -> existing.surface() == Surface.WATER ? 14.0 : 6.0;
                case BUNKER -> existing.surface() == Surface.WATER ? 12.0
                        : existing.surface() == Surface.TREES ? 10.0 : 7.0;
                default -> 0.0;
            };
            if (TerrainRegion.strictlyOverlaps(candidate.boundary(), existing.boundary())
                    || polygonGap(candidate.boundary(), existing.boundary()) < clearance) return false;
        }
        return true;
    }

    private static boolean isHazard(Surface surface) {
        return surface == Surface.WATER || surface == Surface.BUNKER || surface == Surface.TREES
                || surface == Surface.WASTE_AREA;
    }

    private static double polygonGap(List<Position2d> first, List<Position2d> second) {
        double gap = Double.POSITIVE_INFINITY;
        for (Position2d point : first) {
            for (int i = 0; i < second.size(); i++) {
                gap = Math.min(gap, segmentGap(point, second.get(i), second.get((i + 1) % second.size())));
            }
        }
        for (Position2d point : second) {
            for (int i = 0; i < first.size(); i++) {
                gap = Math.min(gap, segmentGap(point, first.get(i), first.get((i + 1) % first.size())));
            }
        }
        return gap;
    }

    private static double segmentGap(Position2d point, Position2d start, Position2d end) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double distanceSquared = dx * dx + dy * dy;
        double t = distanceSquared < 1.0e-9 ? 0.0 : Math.clamp(
                ((point.x() - start.x()) * dx + (point.y() - start.y()) * dy) / distanceSquared, 0.0, 1.0);
        return StrictMath.hypot(point.x() - start.x() - t * dx, point.y() - start.y() - t * dy);
    }

    private static double safeMissSide(long holeSeed) {
        return new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x56355245434f5645L)).nextDouble() < .5 ? -1.0 : 1.0;
    }

    private static void addRefinedBunkers(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                          CourseArchitectureProfile architecture, EnvironmentClassification environment,
                                          long holeSeed, Rng rng) {
        Position2d green = plan.route().greenCenter();
        Position2d tangent = plan.approachDirection();
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double guardedSide = -safeMissSide(holeSeed);
        int guards = rng.nextDouble() < (environment == EnvironmentClassification.LINKS ? .88 : .76)
                ? 1 + (rng.nextDouble() < .31 + architecture.greenDefence() * .20 ? 1 : 0) : 0;
        for (int i = 0; i < guards; i++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                double angle = i == 0 ? guardedSide * (1.16 + rng.nextDouble() * .42 + attempt * .18)
                        : -guardedSide * (1.70 + rng.nextDouble() * .40 + attempt * .16);
                Position2d vector = new Position2d(tangent.x() * StrictMath.cos(angle) + right.x() * StrictMath.sin(angle),
                        tangent.y() * StrictMath.cos(angle) + right.y() * StrictMath.sin(angle));
                double greenExtent = plan.greenFootprint().stream().mapToDouble(point ->
                        (point.x() - green.x()) * vector.x() + (point.y() - green.y()) * vector.y()).max().orElse(20.0);
                double major = environment == EnvironmentClassification.LINKS ? 8.5 + rng.nextDouble() * 3.7
                        : 11.0 + rng.nextDouble() * 4.5;
                double offset = greenExtent + major + 4.0 + rng.nextDouble() * 2.0;
                Position2d center = green.plus(vector.x() * offset, vector.y() * offset);
                if (tryAddRefinedBunker(regions, plan, center, major,
                        major * (.70 + rng.nextDouble() * .13), StrictMath.atan2(vector.y(), vector.x()), rng, 18)) break;
            }
        }
        if (brief.par() == 3) return;
        double landing = Math.min(plan.route().length() - 92.0, 228.0 + rng.nextDouble() * 48.0);
        if (brief.par() == 5 && rng.nextDouble() < .45) landing = Math.min(plan.route().length() - 90.0, landing + 70.0);
        double turn = plan.route().intermediateAnchors().isEmpty() ? 0.0
                : plan.route().intermediateAnchors().getFirst().x() - plan.route().teeOrigin().x();
        double challengeSide = Math.abs(turn) > 12.0 ? Math.signum(turn) : rng.nextDouble() < .5 ? -1.0 : 1.0;
        int group = 1 + (rng.nextDouble() < (environment == EnvironmentClassification.LINKS ? .54 : .46) ? 1 : 0);
        for (int i = 0; i < group; i++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                double distance = landing + (i - (group - 1) / 2.0) * (30.0 + rng.nextDouble() * 8.0)
                        + attempt * 12.0;
                Position2d point = smoothPoint(plan.route(), distance);
                Position2d localTangent = tangent(plan.route(), distance);
                Position2d localRight = new Position2d(localTangent.y(), -localTangent.x());
                double width = interpolate(plan.widthStations(), distance, challengeSide < 0.0);
                double major = environment == EnvironmentClassification.LINKS ? 9.0 + rng.nextDouble() * 4.0
                        : 12.0 + rng.nextDouble() * 5.0;
                Position2d center = point.plus(localRight.x() * challengeSide * (width - major * .20),
                        localRight.y() * challengeSide * (width - major * .20));
                if (tryAddRefinedBunker(regions, plan, center, major,
                        major * (.66 + rng.nextDouble() * .16), StrictMath.atan2(localTangent.y(), localTangent.x()),
                        rng, 16)) break;
            }
        }
    }

    private static boolean tryAddRefinedBunker(List<TerrainRegion> regions, HoleArchitecturePlan plan,
                                                Position2d center, double major, double minor,
                                                double rotation, Rng rng, int vertices) {
        TerrainRegion bunker = irregularRegion(Surface.BUNKER, center, major, minor, rotation, rng, vertices);
        if (TerrainRegion.strictlyOverlaps(bunker.boundary(), plan.greenFootprint())
                || !hazardsHaveRoom(bunker, regions)) return false;
        regions.add(bunker);
        return true;
    }

    private static void addRefinedCoastalShoreline(List<TerrainRegion> regions, HoleArchitecturePlan plan, Rng rng) {
        double length = plan.route().length();
        double first = length * (.34 + rng.nextDouble() * .07);
        double last = length * (.69 + rng.nextDouble() * .09);
        double side = rng.nextDouble() < .5 ? -1.0 : 1.0;
        double phase = rng.nextDouble() * 6.283185307179586;
        List<Position2d> inner = new ArrayList<>();
        List<Position2d> outer = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            double fraction = i / 9.0;
            double distance = first + (last - first) * fraction;
            Position2d point = smoothPoint(plan.route(), distance);
            double width = interpolate(plan.widthStations(), distance, side < 0.0);
            double shoreline = width + 6.0 + 5.5 * StrictMath.sin(fraction * 7.0 + phase);
            double body = 5.0 + 34.0 * StrictMath.pow(StrictMath.sin(StrictMath.PI * fraction), .75)
                    + 5.0 * StrictMath.sin(fraction * 9.0 + phase);
            inner.add(point.plus(side * shoreline, 0.0));
            outer.add(point.plus(side * (shoreline + Math.max(4.0, body)), 0.0));
        }
        List<Position2d> polygon = new ArrayList<>(inner);
        for (int i = outer.size() - 1; i >= 0; i--) polygon.add(outer.get(i));
        regions.add(new TerrainRegion(Surface.WATER, counterClockwise(polygon)));
    }

    private static void addRefinedParklandWater(List<TerrainRegion> regions, HoleArchitecturePlan plan,
                                                HoleBrief brief, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x5635525041524b57L));
        if (rng.nextDouble() >= (brief.archetype() == StrategicArchetype.RISK_REWARD ? .30 : .15)) return;
        double distance = brief.par() == 3 ? plan.route().length() * .65
                : Math.min(plan.route().length() - 85.0, 225.0 + rng.nextDouble() * 60.0);
        Position2d point = smoothPoint(plan.route(), distance);
        Position2d tangent = tangent(plan.route(), distance);
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double side = rng.nextDouble() < .5 ? -1.0 : 1.0;
        double fairwayWidth = interpolate(plan.widthStations(), distance, side < 0.0);
        Position2d center = point.plus(right.x() * side * (fairwayWidth + 8.0),
                right.y() * side * (fairwayWidth + 8.0));
        addIrregular(regions, Surface.WATER, center, 25.0 + rng.nextDouble() * 12.0,
                12.0 + rng.nextDouble() * 8.0, StrictMath.atan2(tangent.y(), tangent.x()) + .45, rng, 22);
    }

    private static void addBunkers(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                   CourseArchitectureProfile architecture, EnvironmentClassification environment,
                                   EnvironmentHoleCharacter environmentCharacter, Rng rng) {
        double defence = architecture.greenDefence() + (brief.recoverySeverity() == RecoverySeverity.PENAL ? .28 : 0.0);
        double bunkerCharacter = switch (environment) {
            case LINKS -> .32;
            case COASTAL -> .24;
            case PARKLAND -> .16;
            case WOODLAND, HEATHLAND -> .12;
            case DESERT -> .08;
            case MOUNTAIN -> .05;
        };
        int greenGuards = rng.nextDouble() < Math.min(.94, .28 + defence * .70 + bunkerCharacter)
                ? 1 + (int) StrictMath.floor(rng.nextDouble() * (1.0 + defence * 2.2)) : 0;
        Position2d green = plan.route().greenCenter();
        Position2d tangent = tangent(plan.route(), plan.route().length() - 2.0);
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double greenRadius = plan.greenFootprint().stream().mapToDouble(point -> point.distanceTo(green)).max().orElse(13.0);
        for (int i = 0; i < greenGuards; i++) {
            double angle = greenGuards == 1 ? (rng.nextDouble() < .5 ? -.85 : .85)
                    : -1.05 + (2.10 * i / Math.max(1, greenGuards - 1)) + (rng.nextDouble() - .5) * .24;
            // Guards orbit outside the effective green; openness shifts their angular mix, never their clearance.
            double clearance = greenRadius + 10.0 + rng.nextDouble() * 3.0;
            double forward = StrictMath.cos(angle) * clearance;
            double lateral = StrictMath.sin(angle) * clearance;
            Position2d center = green.plus(tangent.x() * forward + right.x() * lateral,
                    tangent.y() * forward + right.y() * lateral);
            double scale = 1.0 + bunkerCharacter * .65 + (i == 0 && rng.nextDouble() < .28 ? .38 : 0.0);
            addIrregular(regions, Surface.BUNKER, center, scale * (5.0 + rng.nextDouble() * 4.4),
                    scale * (3.4 + rng.nextDouble() * 2.7), StrictMath.atan2(tangent.y(), tangent.x()) + angle, rng, 14);
        }
        double fairwayBias = switch (environment) {
            case PARKLAND -> .40;
            case LINKS -> .30;
            case COASTAL -> .22;
            case WOODLAND, HEATHLAND -> .12;
            default -> .04;
        };
        boolean fairwaySet = brief.par() != 3 && rng.nextDouble() < Math.min(.92, .26 + architecture.widthRhythm() * .38 + fairwayBias)
                + (plan.routingForm() == RoutingForm.DOGLEG ? .14 : 0.0);
        if (fairwaySet) {
            int group = (environment == EnvironmentClassification.PARKLAND ? 2 : 1)
                    + (rng.nextDouble() < .52 + defence * .22 + bunkerCharacter ? 1 : 0)
                    + (rng.nextDouble() < bunkerCharacter * .72 ? 1 : 0);
            double landing = Math.min(plan.route().length() - 82.0, 215.0 + rng.nextDouble() * 85.0);
            for (int i = 0; i < group; i++) {
                double distance = landing + (i - (group - 1) / 2.0) * (13.0 + rng.nextDouble() * 8.0);
                Position2d point = plan.route().pointAt(distance);
                Position2d localTangent = tangent(plan.route(), distance);
                Position2d localRight = new Position2d(localTangent.y(), -localTangent.x());
                double side = (i == 0 || rng.nextDouble() < .65) ? (rng.nextDouble() < .5 ? -1.0 : 1.0) : -1.0;
                double width = interpolate(plan.widthStations(), distance, side < 0.0);
                Position2d center = point.plus(localRight.x() * side * (width - 1.5), localRight.y() * side * (width - 1.5));
                double scale = 1.0 + bunkerCharacter * .55 + (i == 0 && rng.nextDouble() < .24 ? .28 : 0.0);
                addIrregular(regions, Surface.BUNKER, center, scale * (6.0 + rng.nextDouble() * 4.2),
                        scale * (3.6 + rng.nextDouble() * 2.8), StrictMath.atan2(localTangent.y(), localTangent.x()), rng, 12);
            }
        }
    }

    private static void addTrees(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                 TreeEnclosure enclosure, Rng rng) {
        int clusters = switch (enclosure) {
            case OPEN -> 0;
            case SCATTERED -> 1 + (rng.nextDouble() < .42 ? 1 : 0);
            case BROKEN_LINED -> 3 + (int) StrictMath.floor(rng.nextDouble() * 3.0);
            case WOODED -> 6 + (int) StrictMath.floor(rng.nextDouble() * 4.0);
        };
        for (int i = 0; i < clusters; i++) {
            double fraction = .12 + (i + rng.nextDouble() * .55) / clusters * .76;
            double distance = Math.min(plan.route().length() - 30.0, plan.route().length() * fraction);
            Position2d point = smoothPoint(plan.route(), distance);
            Position2d tangent = tangent(plan.route(), distance);
            Position2d right = new Position2d(tangent.y(), -tangent.x());
            double side = enclosure == TreeEnclosure.BROKEN_LINED && i % 4 != 3 ? (i % 2 == 0 ? -1.0 : 1.0)
                    : ((i % 3 == 0) || rng.nextDouble() < .58) ? -1.0 : 1.0;
            double fairwayWidth = interpolate(plan.widthStations(), distance, side < 0.0);
            // Remain inside the playable rough/boundary envelope, beyond the fairway edge with natural openings.
            double offset = fairwayWidth + 9.0 + rng.nextDouble() * (enclosure == TreeEnclosure.WOODED ? 8.0 : 10.0);
            Position2d center = point.plus(right.x() * side * offset, right.y() * side * offset);
            double major = switch (enclosure) {
                case SCATTERED -> 5.5 + rng.nextDouble() * 2.5;
                case BROKEN_LINED -> 7.0 + rng.nextDouble() * 3.5;
                case WOODED -> 8.0 + rng.nextDouble() * 4.5;
                case OPEN -> 0.0;
            };
            addIrregular(regions, Surface.TREES, center, major, major * (.62 + rng.nextDouble() * .12),
                    StrictMath.atan2(tangent.y(), tangent.x()), rng, 13);
        }
    }

    private static void addCoastalShoreline(List<TerrainRegion> regions, HoleArchitecturePlan plan, Rng rng) {
        double length = plan.route().length();
        double distance = length * (.43 + rng.nextDouble() * .12);
        Position2d point = smoothPoint(plan.route(), distance);
        Position2d tangent = tangent(plan.route(), distance);
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double side = rng.nextDouble() < .5 ? -1.0 : 1.0;
        double fairwayWidth = interpolate(plan.widthStations(), distance, side < 0.0);
        // Long side-water enters the outer playable corridor but leaves a connected fairway/rough route.
        Position2d center = point.plus(right.x() * side * (fairwayWidth + 25.0), right.y() * side * (fairwayWidth + 25.0));
        addIrregular(regions, Surface.WATER, center, Math.min(105.0, length * (.22 + rng.nextDouble() * .06)),
                17.0 + rng.nextDouble() * 5.0, StrictMath.atan2(tangent.y(), tangent.x()), rng, 22);
        if (rng.nextDouble() < .48) {
            double inletDistance = Math.min(length - 38.0, distance + length * (.18 + rng.nextDouble() * .10));
            Position2d inletPoint = smoothPoint(plan.route(), inletDistance);
            Position2d inletTangent = tangent(plan.route(), inletDistance);
            Position2d inletRight = new Position2d(inletTangent.y(), -inletTangent.x());
            double inletWidth = interpolate(plan.widthStations(), inletDistance, side < 0.0);
            Position2d inlet = inletPoint.plus(inletRight.x() * side * (inletWidth + 20.0),
                    inletRight.y() * side * (inletWidth + 20.0));
            addIrregular(regions, Surface.WATER, inlet, 32.0 + rng.nextDouble() * 16.0, 12.0 + rng.nextDouble() * 5.0,
                    StrictMath.atan2(inletTangent.y(), inletTangent.x()), rng, 16);
        }
    }

    private static void addWoodlandWater(List<TerrainRegion> regions, HoleArchitecturePlan plan, Rng rng) {
        double distance = Math.min(plan.route().length() - 62.0, 205.0 + rng.nextDouble() * 110.0);
        Position2d point = smoothPoint(plan.route(), distance);
        Position2d tangent = tangent(plan.route(), distance);
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double side = rng.nextDouble() < .5 ? -1.0 : 1.0;
        double fairwayWidth = interpolate(plan.widthStations(), distance, side < 0.0);
        Position2d center = point.plus(right.x() * side * (fairwayWidth + 16.0), right.y() * side * (fairwayWidth + 16.0));
        addIrregular(regions, Surface.WATER, center, 16.0 + rng.nextDouble() * 8.0, 8.0 + rng.nextDouble() * 4.0,
                StrictMath.atan2(tangent.y(), tangent.x()), rng, 15);
    }

    /**
     * Parkland water is a scarce strategic pond, not a per-hole cosmetic feature. It uses a separate seed
     * stream so tree and bunker composition remains stable when this environmental decision evolves.
     */
    private static void addParklandWater(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                         long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x56355041524b57L));
        double likelihood = .14 + (brief.archetype() == StrategicArchetype.RISK_REWARD ? .12 : 0.0)
                + (plan.routingForm() == RoutingForm.DOGLEG || plan.routingForm() == RoutingForm.DOUBLE_DOGLEG ? .08 : 0.0)
                + (brief.par() == 3 ? .05 : 0.0);
        if (rng.nextDouble() >= likelihood) return;
        double distance = brief.par() == 3
                ? plan.route().length() * (.56 + rng.nextDouble() * .20)
                : plan.route().length() * (.40 + rng.nextDouble() * .32);
        Position2d point = smoothPoint(plan.route(), distance);
        Position2d tangent = tangent(plan.route(), distance);
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double side = rng.nextDouble() < .5 ? -1.0 : 1.0;
        double fairwayWidth = interpolate(plan.widthStations(), distance, side < 0.0);
        // The pond bites into one side of the landing corridor but leaves its opposite line fully connected.
        Position2d center = point.plus(right.x() * side * (fairwayWidth + 11.0),
                right.y() * side * (fairwayWidth + 11.0));
        addIrregular(regions, Surface.WATER, center, 15.0 + rng.nextDouble() * 11.0,
                8.0 + rng.nextDouble() * 5.0, StrictMath.atan2(tangent.y(), tangent.x()), rng, 18);
        if (brief.par() != 3 && rng.nextDouble() < .28) {
            Position2d companion = center.plus(tangent.x() * (22.0 + rng.nextDouble() * 15.0),
                    tangent.y() * (22.0 + rng.nextDouble() * 15.0));
            addIrregular(regions, Surface.WATER, companion, 10.0 + rng.nextDouble() * 6.0,
                    6.0 + rng.nextDouble() * 3.0, StrictMath.atan2(tangent.y(), tangent.x()), rng, 14);
        }
    }

    private static void addWaterIntent(List<TerrainRegion> regions, HoleArchitecturePlan plan, HazardPlan inherited, Rng rng) {
        if (inherited == null || inherited.features().stream().noneMatch(feature -> feature.surface() == Surface.WATER)) return;
        HazardFeature water = inherited.features().stream().filter(feature -> feature.surface() == Surface.WATER).findFirst().orElseThrow();
        double distance = water.role() == HazardRole.GREEN_GUARD ? plan.route().length() - 17.0
                : Math.min(plan.route().length() - 75.0, 255.0);
        Position2d point = plan.route().pointAt(distance);
        Position2d tangent = tangent(plan.route(), distance);
        Position2d right = new Position2d(tangent.y(), -tangent.x());
        double side = water.side() == HazardSide.LEFT ? -1.0 : 1.0;
        double width = interpolate(plan.widthStations(), distance, side < 0.0) + 14.0;
        Position2d center = point.plus(right.x() * side * width, right.y() * side * width);
        addIrregular(regions, Surface.WATER, center, 17.0 + rng.nextDouble() * 7.0, 8.0 + rng.nextDouble() * 4.0,
                StrictMath.atan2(tangent.y(), tangent.x()), rng, 16);
    }

    private static void addIrregular(List<TerrainRegion> regions, Surface surface, Position2d center, double major,
                                     double minor, double rotation, Rng rng, int vertices) {
        regions.add(irregularRegion(surface, center, major, minor, rotation, rng, vertices));
    }

    private static TerrainRegion irregularRegion(Surface surface, Position2d center, double major,
                                                 double minor, double rotation, Rng rng, int vertices) {
        double phase = rng.nextDouble() * 2.0 * StrictMath.PI;
        List<Position2d> points = new ArrayList<>();
        double cos = StrictMath.cos(rotation);
        double sin = StrictMath.sin(rotation);
        for (int i = 0; i < vertices; i++) {
            double theta = 2.0 * StrictMath.PI * i / vertices;
            double variation = 1.0 + .11 * StrictMath.sin(2.0 * theta + phase) + .06 * StrictMath.sin(3.0 * theta - phase);
            double x = major * variation * StrictMath.cos(theta);
            double y = minor * variation * StrictMath.sin(theta);
            points.add(new Position2d(center.x() + x * cos - y * sin, center.y() + x * sin + y * cos));
        }
        return new TerrainRegion(surface, points);
    }

    private static Position2d tangent(ArchitectureRoute route, double distance) {
        Position2d before = route.pointAt(Math.max(0.0, distance - 1.0));
        Position2d after = route.pointAt(Math.min(route.length(), distance + 1.0));
        double dx = after.x() - before.x();
        double dy = after.y() - before.y();
        double magnitude = StrictMath.hypot(dx, dy);
        return new Position2d(dx / magnitude, dy / magnitude);
    }

    private static List<Position2d> scaleAbout(List<Position2d> points, Position2d center, double scale) {
        return points.stream().map(point -> center.plus((point.x() - center.x()) * scale,
                (point.y() - center.y()) * scale)).toList();
    }

    private static List<Position2d> counterClockwise(List<Position2d> points) {
        if (TerrainRegion.signedArea(points) > 0.0) return points;
        List<Position2d> copy = new ArrayList<>(points);
        java.util.Collections.reverse(copy);
        return List.copyOf(copy);
    }

    private record GreenComplex(List<Position2d> collar, List<Position2d> approachApron, double lateralExtent,
                                double backExtent) { }

    private record RouteSample(double distance, Position2d point, Position2d tangent, double left, double right) { }
}

