package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;

/** Deterministic V4 translation from spatial course intent to a small semantic hazard plan. */
final class V4HazardPlanner {
    private static final long HAZARD_PLAN_SALT = 0x563448415a415244L;

    private V4HazardPlanner() {
    }

    static HazardPlan plan(HoleBrief brief, CourseDesignProfile profile, EnvironmentClassification biome,
                           HoleSpatialPlan spatial, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, HAZARD_PLAN_SALT));
        HazardSeverity severity = severity(brief.recoverySeverity());
        List<HazardFeature> features = new ArrayList<>();
        boolean dogleg = !spatial.route().intermediateAnchors().isEmpty();
        boolean usableTurn = dogleg;

        boolean greenGuard = switch (brief.archetype()) {
            case POSITIONAL -> rng.nextDouble() < 0.62;
            case BALANCED -> rng.nextDouble() < 0.42;
            case RISK_REWARD -> rng.nextDouble() < 0.72;
        };
        // A turn guard is the intentional feature on a dogleg; do not crowd it with an unrelated green guard.
        if (greenGuard && !usableTurn) features.add(greenGuard(spatial, severity));

        switch (brief.archetype()) {
            case POSITIONAL -> {
                boolean useBailoutBoundary = !usableTurn && !greenGuard
                        && profile.strategicEmphasis() == StrategicEmphasis.POSITIONAL
                        && rng.nextDouble() < 0.30;
                features.add(usableTurn ? turnRecovery(spatial, biome, severity)
                        : useBailoutBoundary ? bailoutBoundary(spatial, biome, severity)
                        : landingBunker(spatial, LandingZoneRole.PRIMARY, severity));
            }
            case BALANCED -> {
                if (!usableTurn && rng.nextDouble() < 0.55) {
                    features.add(landingBunker(spatial, LandingZoneRole.PRIMARY, HazardSeverity.LIGHT));
                }
            }
            case RISK_REWARD -> features.add(riskFeature(spatial, biome, severity));
        }

        if (usableTurn && features.size() < 4 && features.stream().noneMatch(feature -> feature.role() == HazardRole.TURN_GUARD)
                && features.stream().noneMatch(feature -> feature.role() == HazardRole.LANDING_GUARD)) {
            features.add(turnRecovery(spatial, biome, severity));
        } else if (features.isEmpty() && brief.par() > 3 && brief.archetype() != StrategicArchetype.RISK_REWARD
                && (biome == EnvironmentClassification.WOODLAND || brief.recoverySeverity() == RecoverySeverity.PENAL)
                && features.size() < 4) {
            features.add(recoveryBoundary(spatial, biome, severity));
        }
        if (features.isEmpty()) features.add(greenGuard(spatial, HazardSeverity.LIGHT));
        return new HazardPlan(features);
    }

    private static HazardFeature landingBunker(HoleSpatialPlan spatial, LandingZoneRole role, HazardSeverity severity) {
        LandingZone zone = zone(spatial, role);
        HazardSide side = sideFor(zone.preferredApproachSide(), HazardSide.RIGHT);
        return new HazardFeature(Surface.BUNKER, HazardRole.LANDING_GUARD, HazardAnchor.landingZone(role), side,
                severity, landingEnvelope(zone, severity, false));
    }

    private static HazardFeature greenGuard(HoleSpatialPlan spatial, HazardSeverity severity) {
        GreenSide protectedSide = spatial.greenComplex().protectedSide();
        return new HazardFeature(Surface.BUNKER, HazardRole.GREEN_GUARD,
                HazardAnchor.greenSide(protectedSide), toHazardSide(protectedSide), severity, greenEnvelope(severity));
    }

    private static HazardFeature riskFeature(HoleSpatialPlan spatial, EnvironmentClassification biome,
                                             HazardSeverity severity) {
        LandingZone aggressive = spatial.landingZones().stream()
                .filter(zone -> zone.role() == LandingZoneRole.AGGRESSIVE).findFirst()
                .orElseGet(() -> zone(spatial, LandingZoneRole.PRIMARY));
        HazardSide side = sideFor(aggressive.preferredApproachSide(), HazardSide.RIGHT);
        // Water is a bounded lateral endpoint risk only on water-plausible biomes; it is never a carry claim.
        Surface surface = biome.waterBias() >= 0.25 ? Surface.WATER : Surface.BUNKER;
        return new HazardFeature(surface, HazardRole.LANDING_GUARD, HazardAnchor.landingZone(aggressive.role()),
                side, severity, landingEnvelope(aggressive, severity, surface == Surface.WATER));
    }

    private static HazardFeature turnRecovery(HoleSpatialPlan spatial, EnvironmentClassification biome,
                                              HazardSeverity severity) {
        Position2d corner = spatial.route().intermediateAnchors().getFirst();
        HazardSide side = corner.x() >= 0.0 ? HazardSide.RIGHT : HazardSide.LEFT;
        Surface surface = biome == EnvironmentClassification.WOODLAND ? Surface.TREES : Surface.RECOVERY_AREA;
        return new HazardFeature(surface, HazardRole.TURN_GUARD, HazardAnchor.doglegCorner(), side, severity,
                // A tee-ward offset keeps the recovery boundary tied to the turn without crowding the final green complex.
                new HazardEnvelope(-52.0, 34.0, severity == HazardSeverity.STRONG ? 19.0 : 16.0, 9.0));
    }

    private static HazardFeature recoveryBoundary(HoleSpatialPlan spatial, EnvironmentClassification biome,
                                                  HazardSeverity severity) {
        LandingZone primary = zone(spatial, LandingZoneRole.PRIMARY);
        HazardSide side = sideFor(primary.preferredApproachSide(), HazardSide.LEFT) == HazardSide.LEFT
                ? HazardSide.RIGHT : HazardSide.LEFT;
        Surface surface = biome == EnvironmentClassification.WOODLAND ? Surface.TREES : Surface.RECOVERY_AREA;
        return new HazardFeature(surface, HazardRole.RECOVERY_BOUNDARY,
                HazardAnchor.routeDistance(Math.min(spatial.route().length() - 55.0, primary.routeDistance() + 64.0)),
                side, severity, new HazardEnvelope(0.0, 38.0, 17.0, 8.0));
    }

    private static HazardFeature bailoutBoundary(HoleSpatialPlan spatial, EnvironmentClassification biome,
                                                 HazardSeverity severity) {
        LandingZone primary = zone(spatial, LandingZoneRole.PRIMARY);
        HazardSide side = sideFor(primary.preferredApproachSide(), HazardSide.LEFT) == HazardSide.LEFT
                ? HazardSide.RIGHT : HazardSide.LEFT;
        Surface surface = biome == EnvironmentClassification.WOODLAND ? Surface.TREES : Surface.BUNKER;
        return new HazardFeature(surface, HazardRole.BAILOUT_BOUNDARY,
                HazardAnchor.routeDistance(primary.routeDistance()), side, severity,
                new HazardEnvelope(0.0, primary.halfWidth() * 1.28 + 16.0, 15.0, 7.0));
    }

    private static HazardEnvelope landingEnvelope(LandingZone zone, HazardSeverity severity, boolean water) {
        double radius = water ? 9.0 : 6.0;
        // Water remains a lateral endpoint hazard in the outer recovery envelope. This both protects the
        // landing core and leaves the tee-ward PRIMARY_ROUGH corridor required by the existing drop rule.
        double outerOffset = water ? zone.halfWidth() * 1.20 + 20.0
                : zone.halfWidth() * 0.82 + (severity == HazardSeverity.STRONG ? 5.0 : 3.0);
        return new HazardEnvelope(0.0, outerOffset, water ? 24.0 : radius + 2.0,
                radius + (severity == HazardSeverity.STRONG ? 2.0 : 0.0));
    }

    private static HazardEnvelope greenEnvelope(HazardSeverity severity) {
        return new HazardEnvelope(0.0, severity == HazardSeverity.STRONG ? 14.0 : 12.0,
                severity == HazardSeverity.STRONG ? 7.0 : 6.0, severity == HazardSeverity.LIGHT ? 5.0 : 6.0);
    }

    private static HazardSeverity severity(RecoverySeverity recovery) {
        return switch (recovery) {
            case FORGIVING -> HazardSeverity.LIGHT;
            case BALANCED -> HazardSeverity.STANDARD;
            case PENAL -> HazardSeverity.STRONG;
        };
    }

    private static LandingZone zone(HoleSpatialPlan plan, LandingZoneRole role) {
        return plan.landingZones().stream().filter(zone -> zone.role() == role).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("V4 role needs " + role + " landing zone"));
    }

    private static HazardSide sideFor(PreferredApproachSide preferred, HazardSide fallback) {
        return switch (preferred) {
            case LEFT -> HazardSide.LEFT;
            case RIGHT -> HazardSide.RIGHT;
            case NEUTRAL -> fallback;
        };
    }

    private static HazardSide toHazardSide(GreenSide side) {
        return switch (side) {
            case LEFT -> HazardSide.LEFT;
            case RIGHT -> HazardSide.RIGHT;
            case FRONT -> HazardSide.FRONT;
            case BACK -> HazardSide.BACK;
        };
    }
}
