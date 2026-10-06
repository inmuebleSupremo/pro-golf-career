package com.progolf.sim.course;

import com.progolf.sim.shot.Strategy;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** V3 semantic data retained beside a generated hole for compilation, progression, diagnostics, and future aim work. */
public record HoleSpatialPlan(HoleRoute route, List<LandingZone> landingZones, GreenComplexPlan greenComplex) {
    public HoleSpatialPlan {
        Objects.requireNonNull(route, "route");
        landingZones = List.copyOf(Objects.requireNonNull(landingZones, "landingZones"));
        Objects.requireNonNull(greenComplex, "greenComplex");
        if (landingZones.stream().filter(zone -> zone.role() == LandingZoneRole.PRIMARY).count() != 1) {
            throw new IllegalArgumentException("V3 plan must contain exactly one primary landing zone");
        }
        if (landingZones.stream().anyMatch(zone -> zone.routeDistance() >= route.length() - 10.0)) {
            throw new IllegalArgumentException("landing zones must leave a usable final approach segment");
        }
        if (landingZones.stream().anyMatch(zone -> zone.routeDistance() - zone.depth() / 2.0 <= 0.0
                || zone.routeDistance() + zone.depth() / 2.0 >= route.length() - 8.0)) {
            throw new IllegalArgumentException("landing-zone envelope must remain reachable within the route");
        }
        Position2d finalUnit = route.finalUnit();
        double approachAlignment = finalUnit.x() * greenComplex.approachDirection().x()
                + finalUnit.y() * greenComplex.approachDirection().y();
        if (approachAlignment < 0.90 || greenComplex.center(route).distanceTo(route.approachAnchor()) < 8.0) {
            throw new IllegalArgumentException("green complex must have a forward valid final approach");
        }
    }

    public Position2d greenCenter() {
        return greenComplex.center(route);
    }

    /** Deterministic compatibility target for current AI and no-free-aim human flows. */
    public Position2d progressionTarget(Position2d ball, Strategy strategy) {
        Objects.requireNonNull(ball, "ball");
        Objects.requireNonNull(strategy, "strategy");
        // A straight V3 route needs no compatibility detour. Returning no semantic target deliberately
        // preserves the established green-centre/cup-depth aim frame while retaining its zones for
        // diagnostics and later explicit aiming.
        if (route.intermediateAnchors().isEmpty()) {
            return null;
        }
        Position2d green = greenCenter();
        // Current controls can safely attack the green inside a normal long-club range. Holding a player on
        // a semantic landing zone below that range would turn ordinary par 3s and short par 4s into forced
        // extra-shot holes before the future explicit aim UI exists.
        if (ball.distanceTo(green) <= 340.0) {
            return green;
        }
        double progress = route.projectDistance(ball);
        List<LandingZone> future = landingZones.stream().filter(zone -> zone.routeDistance() > progress + 18.0)
                .sorted(Comparator.comparingDouble(LandingZone::routeDistance)).toList();
        if (future.isEmpty()) return green;
        LandingZoneRole preferred = switch (strategy) {
            case CONSERVATIVE -> LandingZoneRole.SAFE;
            case AGGRESSIVE -> LandingZoneRole.AGGRESSIVE;
            case BALANCED -> LandingZoneRole.PRIMARY;
        };
        LandingZone selected = future.stream().filter(zone -> zone.role() == preferred).findFirst()
                .orElseGet(() -> future.stream().filter(zone -> zone.role() == LandingZoneRole.PRIMARY).findFirst()
                        .orElse(future.getFirst()));
        return selected.center(route);
    }

    HoleSpatialPlan withLateralScale(double scale) {
        HoleRoute scaledRoute = route.withLateralScale(scale);
        return new HoleSpatialPlan(scaledRoute, landingZones.stream().map(zone -> zone.withLateralScale(scale)).toList(),
                greenComplex.withLateralScale(scale));
    }
}
