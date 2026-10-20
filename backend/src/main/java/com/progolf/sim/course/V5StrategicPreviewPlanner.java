package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.List;

/** Strategic intent derived from the same selected V5-family route and widths that produce canonical terrain. */
final class V5StrategicPreviewPlanner {
    private static final long STRATEGY_SALT = 0x5354524154474943L;

    private V5StrategicPreviewPlanner() { }

    static ArchitectureStrategyPlan plan(HoleBrief brief, HoleArchitecturePlan architecture, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, STRATEGY_SALT));
        ArchitectureRoute route = architecture.route();
        double normalLanding = switch (brief.par()) {
            case 3 -> route.length() * (.62 + rng.nextDouble() * .10);
            case 4 -> 205.0 + rng.nextDouble() * 58.0;
            default -> 235.0 + rng.nextDouble() * 52.0;
        };
        double primaryDistance = Math.clamp(normalLanding, 52.0, route.length() - 72.0);
        double width = halfWidthAt(architecture.widthStations(), primaryDistance);
        PreferredApproachSide preferred = preferredSide(architecture, rng);
        double primaryOffset = preferred == PreferredApproachSide.LEFT ? -Math.min(8.0, width * .26)
                : preferred == PreferredApproachSide.RIGHT ? Math.min(8.0, width * .26) : 0.0;
        List<ArchitectureLandingZone> zones = new ArrayList<>();
        zones.add(new ArchitectureLandingZone(LandingZoneRole.PRIMARY, primaryDistance, primaryOffset, 44.0,
                Math.max(10.0, width * .72), preferred));

        double movement = route.length() / route.teeOrigin().distanceTo(route.greenCenter()) - 1.0;
        double opportunity = movement * 2.3 + (brief.par() == 5 ? .32 : 0.0)
                + (width < 18.0 ? .16 : 0.0) + rng.nextDouble() * .72;
        if (brief.par() >= 4 && route.length() - primaryDistance > 112.0 && opportunity > .78) {
            double safeDistance = Math.max(44.0, primaryDistance - (34.0 + rng.nextDouble() * 24.0));
            zones.add(new ArchitectureLandingZone(LandingZoneRole.SAFE, safeDistance, -primaryOffset * .25, 50.0,
                    Math.max(12.0, halfWidthAt(architecture.widthStations(), safeDistance) * .86),
                    PreferredApproachSide.NEUTRAL));
        }
        if (brief.par() >= 4 && route.length() - primaryDistance > 94.0 && opportunity > .98) {
            double aggressiveDistance = Math.min(route.length() - 54.0, primaryDistance + 30.0 + rng.nextDouble() * 27.0);
            zones.add(new ArchitectureLandingZone(LandingZoneRole.AGGRESSIVE, aggressiveDistance, primaryOffset * 1.18,
                    36.0, Math.max(9.0, halfWidthAt(architecture.widthStations(), aggressiveDistance) * .58), preferred));
        }
        return new ArchitectureStrategyPlan(route, zones);
    }

    private static PreferredApproachSide preferredSide(HoleArchitecturePlan architecture, Rng rng) {
        double imbalance = architecture.widthStations().stream().mapToDouble(station ->
                station.leftHalfWidth() - station.rightHalfWidth()).average().orElse(0.0);
        if (StrictMath.abs(imbalance) < 1.4) return PreferredApproachSide.NEUTRAL;
        boolean left = imbalance > 0.0;
        if (rng.nextDouble() < .58) left = !left;
        return left ? PreferredApproachSide.LEFT : PreferredApproachSide.RIGHT;
    }

    private static double halfWidthAt(List<FairwayWidthStation> stations, double distance) {
        FairwayWidthStation nearest = stations.stream().min(java.util.Comparator.comparingDouble(station ->
                StrictMath.abs(station.routeDistance() - distance))).orElseThrow();
        return Math.min(nearest.leftHalfWidth(), nearest.rightHalfWidth());
    }
}
