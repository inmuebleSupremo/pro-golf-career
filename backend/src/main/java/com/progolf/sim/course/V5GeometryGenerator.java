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

    private static void addBunkers(List<TerrainRegion> regions, HoleArchitecturePlan plan, HoleBrief brief,
                                   CourseArchitectureProfile architecture, EnvironmentClassification environment,
                                   EnvironmentHoleCharacter environmentCharacter, Rng rng) {
        double defence = architecture.greenDefence() + (brief.recoverySeverity() == RecoverySeverity.PENAL ? .28 : 0.0);
        double bunkerCharacter = switch (environment) {
            case LINKS -> .32;
            case COASTAL -> .24;
            case PARKLAND -> .16;
            case WOODLAND -> .12;
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
            case WOODLAND -> .12;
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
        regions.add(new TerrainRegion(surface, points));
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

