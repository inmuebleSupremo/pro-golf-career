package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;

/** Small seeded polygon generator for the gameplay geometry of a generated hole. */
final class CanonicalGeometryGenerator {
    private static final int SAMPLES = 18;
    private CanonicalGeometryGenerator() {
    }

    static CourseGeometry generate(double length, double fairwayHalf, double greenHalf, double greenDepth,
                                   boolean bunker, boolean water, boolean trees, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x43414e4f4eL));
        // The current decision model always aims down the tee-to-cup line. Keep the first canonical bends
        // modest enough that they are meaningful visually/strategically without requiring new shot-routing AI.
        double bend = (rng.nextDouble() * 2.0 - 1.0) * (5.0 + rng.nextDouble() * 10.0);
        // The bend is zero at both tee and green. This keeps the canonical target line and the generated
        // fairway connected while retaining an asymmetric, non-straight middle.
        double phase = 0.0;
        Position2d tee = new Position2d(0, 0);
        Position2d green = new Position2d(0, length);
        double overGreenExtent = greenDepth / 2.0 + CourseGenConstants.OVER_GREEN_MARGIN
                + CourseGenConstants.APPROACH_FRINGE;
        double fairwayOuter = fairwayHalf + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA
                + CourseGenConstants.FAIRWAY_ROUGH_EXTRA + CourseGenConstants.FAIRWAY_DEEP_EXTRA;
        double hazardExtra = (water || trees) ? CourseGenConstants.HAZARD_EXTRA : 0.0;
        double greenHalfDepth = greenDepth / 2.0 + CourseGenConstants.APPROACH_FRINGE;
        double greenOuter = greenHalf + CourseGenConstants.GREEN_FRINGE_EXTRA
                + CourseGenConstants.GREEN_ROUGH_EXTRA;
        double overGreenOuter = CourseGenConstants.FRINGE_WIDTH + CourseGenConstants.GREEN_FRINGE_EXTRA
                + CourseGenConstants.GREEN_ROUGH_EXTRA + CourseGenConstants.FAIRWAY_DEEP_EXTRA;
        // HoleZones has always allowed a bounded over-green trouble area. The canonical boundary must
        // likewise continue behind the green; ending it at the green centre silently turns ordinary
        // long approaches into out-of-bounds penalties.
        List<Position2d> boundary = corridor(length, length + overGreenExtent, bend, phase,
                (y, t) -> playableHalfWidth(y, length, fairwayOuter, greenHalfDepth, greenOuter,
                        overGreenOuter, hazardExtra));
        List<TerrainRegion> regions = new ArrayList<>();
        regions.add(region(Surface.DEEP_ROUGH, corridor(length, bend, phase, (y, t) -> fairwayOuter)));
        regions.add(region(Surface.PRIMARY_ROUGH, corridor(length, bend, phase,
                (y, t) -> fairwayHalf + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA
                        + CourseGenConstants.FAIRWAY_ROUGH_EXTRA)));
        regions.add(region(Surface.FIRST_CUT, corridor(length, bend, phase,
                (y, t) -> fairwayHalf + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA)));
        regions.add(region(Surface.FAIRWAY, corridor(length, bend, phase,
                (y, t) -> fairwayHalf * (1.0 + 0.08 * StrictMath.sin(2.0 * StrictMath.PI * t + phase)))));
        regions.add(region(Surface.FRINGE, ellipse(green, greenHalf + CourseGenConstants.GREEN_FRINGE_EXTRA,
                greenHalfDepth, 16)));
        regions.add(region(Surface.GREEN, ellipse(green, greenHalf, greenHalfDepth, 16)));
        // Preserve the playable over-green rough/deep-rough corridor from the legacy spatial model.
        // Green and fringe take precedence where their footprints overlap this extension.
        double overGreenStart = length + greenHalfDepth;
        double overGreenEnd = length + overGreenExtent;
        regions.add(region(Surface.DEEP_ROUGH, overGreen(overGreenStart, overGreenEnd,
                CourseGenConstants.FRINGE_WIDTH + CourseGenConstants.GREEN_FRINGE_EXTRA
                        + CourseGenConstants.GREEN_ROUGH_EXTRA + CourseGenConstants.FAIRWAY_DEEP_EXTRA)));
        regions.add(region(Surface.PRIMARY_ROUGH, overGreen(overGreenStart, overGreenEnd,
                CourseGenConstants.FRINGE_WIDTH + CourseGenConstants.GREEN_FRINGE_EXTRA
                        + CourseGenConstants.GREEN_ROUGH_EXTRA)));
        regions.add(region(Surface.FRINGE, overGreen(overGreenStart, overGreenEnd,
                CourseGenConstants.FRINGE_WIDTH + CourseGenConstants.GREEN_FRINGE_EXTRA)));

        if (bunker) {
            int bunkerCount = 4 + (rng.nextDouble() > 0.55 ? 1 : 0);
            for (int i = 0; i < bunkerCount; i++) {
                double side = i % 2 == 0 ? -1.0 : 1.0;
                double y = length + (i < 2 ? -1.0 : 1.0) * greenDepth * (0.12 + rng.nextDouble() * 0.48);
                double radiusX = 4.0 + rng.nextDouble() * 3.0;
                double radiusY = 5.0 + rng.nextDouble() * 4.0;
                // The legacy bunker ring starts outside the green fringe. Account for this ellipse's
                // radius so a bunker cannot overwrite the green just because it is larger than average.
                double x = centerX(y / length, bend, phase)
                        + side * (greenHalf + CourseGenConstants.GREEN_FRINGE_EXTRA + radiusX);
                regions.add(region(Surface.BUNKER, ellipse(new Position2d(x, y), radiusX, radiusY, 10)));
            }
        }
        if (water) {
            // A single compact pond was visually plausible but too short to affect the legacy model's
            // normal miss envelope. Retain a one-sided feature, but make it a substantial flank carry
            // or creek through the primary landing zone.
            double t = 0.40 + rng.nextDouble() * 0.25;
            double y = length * t;
            double side = rng.nextDouble() < 0.5 ? -1.0 : 1.0;
            double radiusX = 8.0 + rng.nextDouble() * 5.0;
            double radiusY = 70.0 + rng.nextDouble() * 30.0;
            // Water begins outside the legacy fairway/cut/rough/deep-rough corridor. Its radius
            // must be included in the centre offset, otherwise an ellipse may cover the fairway.
            // The legacy water band began at the outer miss corridor. Set the organic shoreline a
            // little inside that mathematical edge so its curved contour presents real risk rather
            // than merely touching an otherwise unreachable boundary.
            double x = centerX(t, bend, phase) + side * (fairwayOuter - 12.0 + radiusX);
            // A flank hazard preserves a playable rough corridor on the tee side for deterministic relief.
            regions.add(region(Surface.WATER, ellipse(new Position2d(x, y), radiusX, radiusY, 14)));
        }
        if (trees) {
            for (int i = 0; i < 5; i++) {
                double t = 0.18 + rng.nextDouble() * 0.68;
                double side = rng.nextDouble() < 0.5 ? -1.0 : 1.0;
                double radiusX = 11.0 + rng.nextDouble() * 4.0;
                double x = centerX(t, bend, phase) + side * (fairwayOuter - 12.0 + radiusX);
                regions.add(region(Surface.TREES, ellipse(new Position2d(x, length * t), radiusX,
                        18.0 + rng.nextDouble() * 10.0, 10)));
            }
        }
        return new CourseGeometry(tee, green, boundary, regions);
    }

    /** V3 compiler: turns semantic route/zone/green intent into the same authoritative polygon terrain. */
    static CourseGeometry generateV3(double fairwayHalf, boolean bunker, boolean water, boolean trees, long holeSeed,
                                     HoleSpatialPlan plan) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, 0x563347454f4dL));
        HoleRoute route = plan.route();
        Position2d green = plan.greenCenter();
        Position2d finalUnit = route.finalUnit();
        List<Position2d> fairwayPath = new ArrayList<>(route.points());
        fairwayPath.add(green);
        double fairwayOuter = fairwayHalf + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA
                + CourseGenConstants.FAIRWAY_ROUGH_EXTRA + CourseGenConstants.FAIRWAY_DEEP_EXTRA;
        double hazardExtra = water || trees ? CourseGenConstants.HAZARD_EXTRA : 0.0;
        double overExtent = plan.greenComplex().majorAxis() + CourseGenConstants.OVER_GREEN_MARGIN;
        List<Position2d> boundaryPath = new ArrayList<>(fairwayPath);
        boundaryPath.add(green.plus(finalUnit.x() * overExtent, finalUnit.y() * overExtent));
        List<TerrainRegion> regions = new ArrayList<>();
        regions.add(region(Surface.DEEP_ROUGH, routeCorridor(fairwayPath,
                distance -> fairwayWidth(plan, fairwayHalf, distance) + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA
                        + CourseGenConstants.FAIRWAY_ROUGH_EXTRA + CourseGenConstants.FAIRWAY_DEEP_EXTRA)));
        regions.add(region(Surface.PRIMARY_ROUGH, routeCorridor(fairwayPath,
                distance -> fairwayWidth(plan, fairwayHalf, distance) + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA
                        + CourseGenConstants.FAIRWAY_ROUGH_EXTRA)));
        regions.add(region(Surface.FIRST_CUT, routeCorridor(fairwayPath,
                distance -> fairwayWidth(plan, fairwayHalf, distance) + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA)));
        regions.add(region(Surface.FAIRWAY, routeCorridor(fairwayPath,
                distance -> fairwayWidth(plan, fairwayHalf, distance))));
        GreenComplexPlan greenPlan = plan.greenComplex();
        regions.add(region(Surface.FRINGE, rotatedEllipse(green, greenPlan.majorAxis() + CourseGenConstants.GREEN_FRINGE_EXTRA,
                greenPlan.minorAxis() + CourseGenConstants.GREEN_FRINGE_EXTRA, greenPlan.rotationRadians(), 16)));
        regions.add(region(Surface.GREEN, rotatedEllipse(green, greenPlan.majorAxis(), greenPlan.minorAxis(),
                greenPlan.rotationRadians(), 16)));

        if (bunker) addV3Bunkers(regions, rng, plan, green);
        if (water) addV3Water(regions, rng, plan, fairwayOuter);
        if (trees) addV3Trees(regions, rng, plan, fairwayOuter);

        return new CourseGeometry(route.teeOrigin(), green, routeCorridor(boundaryPath,
                distance -> boundaryWidth(plan, fairwayOuter, distance) + hazardExtra), regions);
    }

    /** V4 compiler: semantic hazard features become canonical terrain while route/green geometry remains V3-shaped. */
    static CourseGeometry generateV4(double fairwayHalf, long holeSeed, HoleSpatialPlan plan, HazardPlan hazards) {
        CourseGeometry base = generateV3(fairwayHalf, false, false, false, holeSeed, plan);
        List<TerrainRegion> regions = new ArrayList<>(base.regions());
        List<TerrainRegion> strategicHazards = new ArrayList<>();
        for (HazardFeature feature : hazards.features()) {
            TerrainRegion region = compileV4Feature(feature, plan);
            for (TerrainRegion prior : strategicHazards) {
                if (TerrainRegion.strictlyOverlaps(prior.boundary(), region.boundary())) {
                    throw new IllegalArgumentException("V4 strategic hazards may not overlap: " + prior.surface()
                            + " and " + feature.surface() + " for " + feature.role());
                }
            }
            strategicHazards.add(region);
            regions.add(region);
        }
        CourseGeometry geometry = new CourseGeometry(base.tee(), base.greenCenter(), base.playableBoundary(), regions);
        validateV4Feasibility(plan, geometry, strategicHazards);
        return geometry;
    }

    private static TerrainRegion compileV4Feature(HazardFeature feature, HoleSpatialPlan plan) {
        HazardFrame frame = frameFor(feature.anchor(), plan);
        HazardEnvelope envelope = feature.envelope();
        double forward = envelope.forwardOffset();
        double lateral = 0.0;
        switch (feature.side()) {
            case LEFT -> lateral = -envelope.lateralOffset();
            case RIGHT -> lateral = envelope.lateralOffset();
            case FRONT -> forward = -envelope.lateralOffset();
            case BACK -> forward = envelope.lateralOffset();
        }
        if (feature.anchor().type() == HazardAnchorType.GREEN_SIDE) {
            GreenComplexPlan green = plan.greenComplex();
            // Green guards sit beyond green/fringe support in their semantic direction, never across the core.
            double clearance = Math.max(green.majorAxis(), green.minorAxis())
                    + CourseGenConstants.GREEN_FRINGE_EXTRA + envelope.lateralRadius() + 1.0;
            switch (feature.side()) {
                case LEFT -> lateral = -clearance;
                case RIGHT -> lateral = clearance;
                case FRONT -> forward = -clearance;
                case BACK -> forward = clearance;
            }
        }
        Position2d center = frame.origin().plus(frame.unit().x() * forward + frame.right().x() * lateral,
                frame.unit().y() * forward + frame.right().y() * lateral);
        double rotation = StrictMath.atan2(frame.unit().y(), frame.unit().x());
        int vertices = feature.surface() == Surface.WATER ? 14 : 10;
        return region(feature.surface(), rotatedEllipse(center, envelope.longitudinalRadius(), envelope.lateralRadius(),
                rotation, vertices));
    }

    private static HazardFrame frameFor(HazardAnchor anchor, HoleSpatialPlan plan) {
        HoleRoute route = plan.route();
        return switch (anchor.type()) {
            case LANDING_ZONE -> {
                LandingZone zone = plan.landingZones().stream().filter(candidate -> candidate.role() == anchor.landingZoneRole())
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("missing V4 landing-zone anchor"));
                yield routeFrame(route, zone.center(route), zone.routeDistance());
            }
            case ROUTE_DISTANCE -> routeFrame(route, route.pointAt(anchor.routeDistance(), 0.0), anchor.routeDistance());
            case DOGLEG_CORNER -> {
                if (route.intermediateAnchors().isEmpty()) throw new IllegalArgumentException("turn guard without dogleg");
                Position2d corner = route.intermediateAnchors().getFirst();
                yield routeFrame(route, corner, route.teeOrigin().distanceTo(corner));
            }
            case GREEN_SIDE -> {
                Position2d unit = route.finalUnit();
                yield new HazardFrame(plan.greenCenter(), unit, new Position2d(unit.y(), -unit.x()));
            }
        };
    }

    private static HazardFrame routeFrame(HoleRoute route, Position2d origin, double distance) {
        Position2d before = route.pointAt(Math.max(0.0, distance - 1.0), 0.0);
        Position2d after = route.pointAt(Math.min(route.length(), distance + 1.0), 0.0);
        double dx = after.x() - before.x();
        double dy = after.y() - before.y();
        double magnitude = StrictMath.hypot(dx, dy);
        if (magnitude < 0.1) throw new IllegalArgumentException("invalid V4 route frame");
        Position2d unit = new Position2d(dx / magnitude, dy / magnitude);
        return new HazardFrame(origin, unit, new Position2d(unit.y(), -unit.x()));
    }

    private static void validateV4Feasibility(HoleSpatialPlan plan, CourseGeometry geometry,
                                              List<TerrainRegion> strategicHazards) {
        if (hazardSurface(geometry.surfaceAt(geometry.tee())) || hazardSurface(geometry.surfaceAt(geometry.greenCenter()))) {
            throw new IllegalArgumentException("V4 hazard covers tee or green centre");
        }
        for (Position2d anchor : plan.route().points()) {
            if (hazardSurface(geometry.surfaceAt(anchor))) throw new IllegalArgumentException("V4 hazard blocks route anchor");
        }
        for (LandingZone zone : plan.landingZones()) {
            if (zone.role() != LandingZoneRole.PRIMARY && zone.role() != LandingZoneRole.SAFE) continue;
            requireClearZoneCore(plan.route(), zone, geometry);
        }
        for (TerrainRegion water : strategicHazards.stream().filter(region -> region.surface() == Surface.WATER).toList()) {
            if (!hasWaterRelief(water, geometry)) throw new IllegalArgumentException("V4 water lacks deterministic rough relief");
        }
    }

    private static void requireClearZoneCore(HoleRoute route, LandingZone zone, CourseGeometry geometry) {
        double forward = zone.depth() * 0.22;
        double lateral = zone.halfWidth() * 0.22;
        for (double f : List.of(-forward, 0.0, forward)) {
            for (double l : List.of(-lateral, 0.0, lateral)) {
                if (hazardSurface(geometry.surfaceAt(route.pointAt(zone.routeDistance() + f, zone.centreOffset() + l)))) {
                    throw new IllegalArgumentException("V4 hazard occupies required " + zone.role() + " core");
                }
            }
        }
    }

    private static boolean hasWaterRelief(TerrainRegion water, CourseGeometry geometry) {
        double x = water.boundary().stream().mapToDouble(Position2d::x).average().orElseThrow();
        double y = water.boundary().stream().mapToDouble(Position2d::y).average().orElseThrow();
        Position2d contact = new Position2d(x, y);
        Position2d tee = geometry.tee();
        double dx = tee.x() - contact.x();
        double dy = tee.y() - contact.y();
        double distance = StrictMath.hypot(dx, dy);
        if (distance < 15.0) return false;
        double ux = dx / distance;
        double uy = dy / distance;
        for (double setback = 15.0; setback <= distance; setback += 1.0) {
            if (geometry.surfaceAt(contact.plus(ux * setback, uy * setback)) == Surface.PRIMARY_ROUGH) return true;
        }
        return false;
    }

    private static boolean hazardSurface(Surface surface) {
        return surface == Surface.BUNKER || surface == Surface.WATER || surface == Surface.TREES
                || surface == Surface.RECOVERY_AREA;
    }

    private record HazardFrame(Position2d origin, Position2d unit, Position2d right) {
    }

    private static double fairwayWidth(HoleSpatialPlan plan, double base, double routeDistance) {
        double width = base;
        for (LandingZone zone : plan.landingZones()) {
            double span = zone.depth() / 2.0 + 18.0;
            double influence = Math.max(0.0, 1.0 - Math.abs(routeDistance - zone.routeDistance()) / span);
            width += (zone.halfWidth() - base) * influence;
        }
        // Keep the last approach leg purposeful without severing the playable corridor.
        double finalStart = plan.route().length() - 48.0;
        if (routeDistance > finalStart) {
            width *= 1.0 - 0.16 * Math.clamp((routeDistance - finalStart) / 48.0, 0.0, 1.0);
        }
        // A route turn concentrates the current carry/lateral dispersion onto a smaller footprint than V2's
        // shallow corridor. Preserve strategic pinch/zone ratios while giving the transitional no-free-aim
        // controls a comparable baseline landing envelope.
        return Math.max(10.0, width * 1.20);
    }

    private static double boundaryWidth(HoleSpatialPlan plan, double fairwayOuter, double routeDistance) {
        double base = fairwayWidth(plan, fairwayOuter - CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA
                - CourseGenConstants.FAIRWAY_ROUGH_EXTRA - CourseGenConstants.FAIRWAY_DEEP_EXTRA, routeDistance);
        return base + CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA + CourseGenConstants.FAIRWAY_ROUGH_EXTRA
                + CourseGenConstants.FAIRWAY_DEEP_EXTRA;
    }

    private static List<Position2d> routeCorridor(List<Position2d> path, WidthAtDistance width) {
        if (path.size() < 2) throw new IllegalArgumentException("route corridor needs at least two points");
        List<Double> distances = new ArrayList<>(path.size());
        distances.add(0.0);
        for (int i = 1; i < path.size(); i++) distances.add(distances.get(i - 1) + path.get(i - 1).distanceTo(path.get(i)));
        List<Position2d> right = new ArrayList<>(path.size());
        List<Position2d> left = new ArrayList<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            Position2d tangent = tangent(path, i);
            double halfWidth = width.at(distances.get(i));
            right.add(path.get(i).plus(tangent.y() * halfWidth, -tangent.x() * halfWidth));
            left.add(path.get(i).plus(-tangent.y() * halfWidth, tangent.x() * halfWidth));
        }
        List<Position2d> result = new ArrayList<>(right);
        for (int i = left.size() - 1; i >= 0; i--) result.add(left.get(i));
        return result;
    }

    private static Position2d tangent(List<Position2d> path, int index) {
        Position2d before = path.get(Math.max(0, index - 1));
        Position2d after = path.get(Math.min(path.size() - 1, index + 1));
        double dx = after.x() - before.x();
        double dy = after.y() - before.y();
        double magnitude = StrictMath.hypot(dx, dy);
        if (magnitude < 1.0e-9) throw new IllegalArgumentException("route corridor contains repeated points");
        return new Position2d(dx / magnitude, dy / magnitude);
    }

    private static List<Position2d> rotatedEllipse(Position2d center, double major, double minor, double rotation,
                                                    int vertices) {
        List<Position2d> result = new ArrayList<>(vertices);
        double cosRotation = StrictMath.cos(rotation);
        double sinRotation = StrictMath.sin(rotation);
        for (int i = 0; i < vertices; i++) {
            double theta = 2.0 * StrictMath.PI * i / vertices;
            double x = major * StrictMath.cos(theta);
            double y = minor * StrictMath.sin(theta);
            result.add(new Position2d(center.x() + x * cosRotation - y * sinRotation,
                    center.y() + x * sinRotation + y * cosRotation));
        }
        return result;
    }

    private static void addV3Bunkers(List<TerrainRegion> regions, Rng rng, HoleSpatialPlan plan, Position2d green) {
        GreenComplexPlan greenPlan = plan.greenComplex();
        double normalX = -StrictMath.sin(greenPlan.rotationRadians());
        double normalY = StrictMath.cos(greenPlan.rotationRadians());
        for (int i = 0; i < 3 + (rng.nextDouble() > 0.55 ? 1 : 0); i++) {
            double side = i % 2 == 0 ? 1.0 : -1.0;
            double radiusX = 4.0 + rng.nextDouble() * 2.5;
            double radiusY = 4.5 + rng.nextDouble() * 3.0;
            double distance = greenPlan.minorAxis() + CourseGenConstants.GREEN_FRINGE_EXTRA + radiusY + 1.5;
            Position2d center = green.plus(normalX * side * distance, normalY * side * distance);
            regions.add(region(Surface.BUNKER, rotatedEllipse(center, radiusX, radiusY, greenPlan.rotationRadians(), 10)));
        }
    }

    private static void addV3Water(List<TerrainRegion> regions, Rng rng, HoleSpatialPlan plan, double fairwayOuter) {
        LandingZone anchor = plan.landingZones().stream().filter(zone -> zone.role() == LandingZoneRole.PRIMARY)
                .findFirst().orElseThrow();
        Position2d point = anchor.center(plan.route());
        Position2d unit = plan.route().finalUnit();
        // Use the local segment direction at the zone, not an obsolete global vertical axis.
        Position2d next = plan.route().pointAt(anchor.routeDistance() + 1.0, 0.0);
        Position2d current = plan.route().pointAt(anchor.routeDistance(), 0.0);
        double dx = next.x() - current.x();
        double dy = next.y() - current.y();
        double magnitude = StrictMath.hypot(dx, dy);
        if (magnitude > 0.1) unit = new Position2d(dx / magnitude, dy / magnitude);
        double side = rng.nextDouble() < 0.5 ? -1.0 : 1.0;
        double radiusX = 8.0 + rng.nextDouble() * 4.0;
        double radiusY = 38.0 + rng.nextDouble() * 20.0;
        Position2d center = point.plus(unit.y() * side * (fairwayOuter - 10.0 + radiusX),
                -unit.x() * side * (fairwayOuter - 10.0 + radiusX));
        regions.add(region(Surface.WATER, rotatedEllipse(center, radiusY, radiusX,
                StrictMath.atan2(unit.y(), unit.x()), 14)));
    }

    private static void addV3Trees(List<TerrainRegion> regions, Rng rng, HoleSpatialPlan plan, double fairwayOuter) {
        for (int i = 0; i < 4; i++) {
            double distance = plan.route().length() * (0.18 + rng.nextDouble() * 0.62);
            Position2d point = plan.route().pointAt(distance, 0.0);
            Position2d next = plan.route().pointAt(Math.min(plan.route().length(), distance + 1.0), 0.0);
            double dx = next.x() - point.x();
            double dy = next.y() - point.y();
            double magnitude = StrictMath.hypot(dx, dy);
            if (magnitude < 0.1) continue;
            double side = rng.nextDouble() < 0.5 ? -1.0 : 1.0;
            double radius = 10.0 + rng.nextDouble() * 3.0;
            Position2d center = point.plus(dy / magnitude * side * (fairwayOuter - 9.0 + radius),
                    -dx / magnitude * side * (fairwayOuter - 9.0 + radius));
            regions.add(region(Surface.TREES, rotatedEllipse(center, radius, 16.0 + rng.nextDouble() * 7.0,
                    StrictMath.atan2(dy, dx), 10)));
        }
    }

    private static TerrainRegion region(Surface surface, List<Position2d> boundary) {
        return new TerrainRegion(surface, boundary);
    }

    private static List<Position2d> corridor(double length, double bend, double phase, Width width) {
        return corridor(length, length, bend, phase, width);
    }

    /** Extends a centreline corridor after the green without bending it back toward the tee. */
    private static List<Position2d> corridor(double length, double endY, double bend, double phase, Width width) {
        List<Position2d> right = new ArrayList<>();
        List<Position2d> left = new ArrayList<>();
        int samples = SAMPLES + (endY > length ? 2 : 0);
        for (int i = 0; i <= samples; i++) {
            double y = endY * i / samples;
            double t = Math.min(1.0, y / length);
            double x = centerX(t, bend, phase);
            double w = width.at(y, t);
            // Keep each side monotone in y. A strict perpendicular offset can fold a polygon back over
            // itself when a narrow green transition meets a sharply bending centreline; these canonical
            // regions need stable containment, not a differential-geometry offset curve.
            // Right side upward then left side downward is counter-clockwise.
            right.add(new Position2d(x + w, y));
            left.add(new Position2d(x - w, y));
        }
        List<Position2d> result = new ArrayList<>(right);
        for (int i = left.size() - 1; i >= 0; i--) result.add(left.get(i));
        return result;
    }

    private static List<Position2d> overGreen(double startY, double endY, double halfWidth) {
        return List.of(new Position2d(-halfWidth, startY), new Position2d(halfWidth, startY),
                new Position2d(halfWidth, endY), new Position2d(-halfWidth, endY));
    }

    private static List<Position2d> ellipse(Position2d center, double rx, double ry, int vertices) {
        List<Position2d> result = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            double theta = 2.0 * StrictMath.PI * i / vertices;
            result.add(new Position2d(center.x() + rx * StrictMath.cos(theta), center.y() + ry * StrictMath.sin(theta)));
        }
        return result;
    }

    private static double centerX(double t, double bend, double phase) {
        return bend * StrictMath.sin(StrictMath.PI * (t + phase));
    }

    /**
     * Keeps the canonical playable boundary compatible with the legacy shot bands. The fairway miss
     * corridor must narrow around the green complex, then use the distinct over-green envelope; leaving
     * it fairway-wide there made ordinary long or wide approaches artificially safe.
     */
    private static double playableHalfWidth(double y, double length, double fairwayOuter, double greenHalfDepth,
                                            double greenOuter, double overGreenOuter, double hazardExtra) {
        double greenStart = length - greenHalfDepth;
        double taperStart = Math.max(0.0, greenStart - 25.0);
        double greenEnd = length + greenHalfDepth;
        if (y < taperStart) {
            return fairwayOuter + hazardExtra;
        }
        if (y <= greenStart) {
            return interpolate(fairwayOuter, greenOuter, (y - taperStart) / (greenStart - taperStart)) + hazardExtra;
        }
        if (y <= greenEnd) {
            return greenOuter + hazardExtra;
        }
        return interpolate(greenOuter, overGreenOuter, (y - greenEnd) / (greenHalfDepth
                + CourseGenConstants.OVER_GREEN_MARGIN)) + hazardExtra;
    }

    private static double interpolate(double from, double to, double progress) {
        return from + (to - from) * Math.clamp(progress, 0.0, 1.0);
    }

    @FunctionalInterface
    private interface Width { double at(double y, double t); }

    @FunctionalInterface
    private interface WidthAtDistance { double at(double routeDistance); }
}
