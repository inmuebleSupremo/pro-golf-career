package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.HoleSpatialPlan;
import com.progolf.sim.course.LandingZone;
import com.progolf.sim.course.LandingZoneRole;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure pre-shot V4 landing-zone evaluator. It deliberately describes a bounded current-course exposure
 * envelope rather than sampling the next execution result; {@link ShotResolver} remains final authority.
 */
public final class StrategicTargetPlanner {
    private static final double FUTURE_ROUTE_BUFFER = 18.0;
    private static final double MATERIAL_NEXT_SHOT_DELTA = 12.0;

    private StrategicTargetPlanner() { }

    public static List<ShotGuidance.StrategicOption> options(HoleModel hole, BallState ball, double remainingDistance,
                                                               Surface lie, Attributes attributes, Environment environment) {
        Objects.requireNonNull(hole, "hole");
        Objects.requireNonNull(ball, "ball");
        Objects.requireNonNull(lie, "lie");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(environment, "environment");
        HoleSpatialPlan plan = hole.strategicLandingPlan();
        CourseGeometry geometry = hole.geometry();
        if (plan == null || geometry == null || hole.cupPosition() == null || !eligibleLie(lie) || remainingDistance <= 80.0) {
            return List.of();
        }

        double progress = plan.route().projectDistance(ball.position());
        Map<LandingZoneRole, Candidate> candidates = new EnumMap<>(LandingZoneRole.class);
        for (LandingZone zone : plan.landingZones()) {
            if (zone.routeDistance() <= progress + FUTURE_ROUTE_BUFFER) continue;
            Candidate candidate = candidateFor(zone, plan, geometry, hole.cupPosition(), ball.position(), lie,
                    attributes, environment);
            if (candidate != null) candidates.put(zone.role(), candidate);
        }
        Candidate primary = candidates.get(LandingZoneRole.PRIMARY);
        if (primary == null) return List.of();

        List<ShotGuidance.StrategicOption> result = new ArrayList<>();
        Candidate safe = candidates.get(LandingZoneRole.SAFE);
        Candidate aggressive = candidates.get(LandingZoneRole.AGGRESSIVE);
        // A V4 risk/reward hole commonly has an open SAFE core and an equally open PRIMARY core, with its
        // actual guard beside AGGRESSIVE. SAFE is therefore truthfully compared to the most advanced viable
        // route, not only to PRIMARY; it remains hidden unless that route is both shorter and more exposed.
        Candidate saferComparison = aggressive != null
                && aggressive.nextShotDistance <= primary.nextShotDistance - MATERIAL_NEXT_SHOT_DELTA
                ? aggressive : primary;
        if (safe != null && safe.exposure < saferComparison.exposure
                && safe.nextShotDistance >= saferComparison.nextShotDistance + MATERIAL_NEXT_SHOT_DELTA) {
            result.add(option(safe, "Longer next shot leaves about " + rounded(safe.nextShotDistance)
                    + " yd for lower landing exposure", "Lower landing exposure"));
        }
        result.add(option(primary, "Balanced route leaves about " + rounded(primary.nextShotDistance) + " yd to the pin",
                exposureText(primary.exposure, "Balanced landing exposure")));
        if (aggressive != null && aggressive.exposure > primary.exposure
                && aggressive.nextShotDistance <= primary.nextShotDistance - MATERIAL_NEXT_SHOT_DELTA) {
            result.add(option(aggressive, "Shorter next shot leaves about " + rounded(aggressive.nextShotDistance) + " yd to the pin",
                    "Higher landing exposure"));
        }
        return List.copyOf(result);
    }

    /** Returns the disposition's published option, or null so callers retain their established policy. */
    public static ShotGuidance.StrategicOption preferred(List<ShotGuidance.StrategicOption> options, Strategy strategy) {
        LandingZoneRole wanted = switch (strategy) {
            case CONSERVATIVE -> LandingZoneRole.SAFE;
            case BALANCED -> LandingZoneRole.PRIMARY;
            case AGGRESSIVE -> LandingZoneRole.AGGRESSIVE;
        };
        return options.stream().filter(option -> option.role() == wanted).findFirst()
                .orElseGet(() -> options.stream().filter(option -> option.role() == LandingZoneRole.PRIMARY).findFirst()
                        .orElse(null));
    }

    private static Candidate candidateFor(LandingZone zone, HoleSpatialPlan plan, CourseGeometry geometry, Position2d cup,
                                          Position2d ball, Surface lie, Attributes attributes, Environment environment) {
        Position2d target = zone.center(plan.route());
        Surface landing = geometry.surfaceAt(target);
        if (!ordinaryLanding(landing)) return null;
        double distance = ball.distanceTo(target);
        ClubSpec club = ClubSpec.all().stream()
                .filter(spec -> ShotFamilyEligibility.evaluate(lie, spec, ShotFamily.FULL).allowed())
                .filter(spec -> normalReach(spec, attributes) >= distance)
                .min(Comparator.comparingDouble(ClubSpec::baseCarry)).orElse(null);
        if (club == null) return null;
        ShotExecutionProfile profile = ShotExecutionProfile.derive(lie, club, ShotFamily.FULL, attributes, environment);
        Position2d nominalRest = nominalRest(ball, target, geometry, profile.rollYardsOn(landing));
        double exposure = exposure(geometry, target, distance, club, attributes, profile);
        return new Candidate(zone.role(), target, club, landing, exposure, nominalRest.distanceTo(cup));
    }

    private static Position2d nominalRest(Position2d ball, Position2d target, CourseGeometry geometry, double release) {
        ShotFrame frame = ShotFrame.toward(ball, target);
        Position2d endpoint = frame.project(ball.distanceTo(target) + release, 0.0);
        Surface surface = geometry.surfaceAt(endpoint);
        // Match the approved planner boundary: do not predict a roll-created hazard/recovery. The resolver
        // will apply its existing bounded clamp/settlement rule to the actual contact.
        return surface.isPlayable() && !surface.isHazard() ? endpoint : target;
    }

    private static double exposure(CourseGeometry geometry, Position2d target, double distance, ClubSpec club,
                                   Attributes attributes, ShotExecutionProfile profile) {
        // The resolver's lateral uncertainty is a supported current fact. This bounded two-sigma envelope
        // deliberately reports nearby canonical trouble as exposure, not as a predicted penalty.
        double accuracyFactor = StrictMath.exp(SimConstants.ATTRIBUTE_FACTOR_K
                * (attributes.norm(club.lateralAttribute()) - SimConstants.ATTRIBUTE_FACTOR_PIVOT));
        double supportedSigma = (SimConstants.LATERAL_DISPERSION_FRACTION * distance * club.lateralDispersion()
                + SimConstants.LATERAL_DISPERSION_FLOOR) / accuracyFactor * profile.lateralDispersionMultiplier();
        double radius = Math.max(12.0, 2.0 * supportedSigma);
        double score = surfaceWeight(geometry.surfaceAt(target));
        // Inspect the centre-adjacent and outer parts of the existing club-sized landing envelope. A single
        // outer ring can step over a compact V4 guard and falsely call the aggressive side equally open.
        for (double fraction : List.of(0.50, 0.75, 1.0)) {
            for (int i = 0; i < 8; i++) {
                double angle = 2.0 * StrictMath.PI * i / 8.0;
                score += surfaceWeight(geometry.surfaceAt(target.plus(radius * fraction * StrictMath.cos(angle),
                        radius * fraction * StrictMath.sin(angle))));
            }
        }
        // A compact, route-relative guard can sit between radial sample spokes. Add a small fixed planar
        // grid so its risk is measured only when it exists in canonical geometry, rather than inferred
        // from the landing-zone role or from generator metadata.
        double gridStep = 4.0;
        for (double x = -radius; x <= radius; x += gridStep) {
            for (double y = -radius; y <= radius; y += gridStep) {
                if (x == 0.0 && y == 0.0 || x * x + y * y > radius * radius) continue;
                score += surfaceWeight(geometry.surfaceAt(target.plus(x, y)));
            }
        }
        return score;
    }

    private static double surfaceWeight(Surface surface) {
        return switch (surface) {
            case WATER, OUT_OF_BOUNDS -> 4.0;
            case BUNKER -> 2.0;
            case TREES, RECOVERY_AREA, DEEP_ROUGH, WASTE_AREA -> 1.0;
            default -> 0.0;
        };
    }

    private static boolean ordinaryLanding(Surface surface) {
        return surface == Surface.FAIRWAY || surface == Surface.FIRST_CUT || surface == Surface.PRIMARY_ROUGH;
    }

    private static boolean eligibleLie(Surface lie) {
        return lie != Surface.GREEN && lie != Surface.BUNKER && lie.isPlayable();
    }

    private static double normalReach(ClubSpec club, Attributes attributes) {
        return club.baseCarry() * (SimConstants.REACH_FLOOR + SimConstants.REACH_SPAN
                * attributes.norm(club.distanceAttribute()));
    }

    private static ShotGuidance.StrategicOption option(Candidate candidate, String route, String exposure) {
        return new ShotGuidance.StrategicOption(candidate.role, new AimPoint(candidate.target.x(), candidate.target.y()),
                candidate.club.id(), ShotFamily.FULL, route, exposure);
    }

    private static String exposureText(double exposure, String defaultText) {
        return exposure > 0.0 ? "Guarded landing area" : defaultText;
    }

    private static long rounded(double yards) { return Math.round(yards); }

    private record Candidate(LandingZoneRole role, Position2d target, ClubSpec club, Surface landing,
                             double exposure, double nextShotDistance) { }
}
