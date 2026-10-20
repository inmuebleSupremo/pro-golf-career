package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** Strategic intent derived from the exact selected Generator 8 route, not from legacy metadata. */
public record ArchitectureStrategyPlan(ArchitectureRoute route, List<ArchitectureLandingZone> landingZones) {
    public ArchitectureStrategyPlan {
        Objects.requireNonNull(route, "route");
        landingZones = List.copyOf(Objects.requireNonNull(landingZones, "landingZones"));
        if (landingZones.stream().filter(zone -> zone.role() == LandingZoneRole.PRIMARY).count() != 1) {
            throw new IllegalArgumentException("architecture strategy requires exactly one primary landing zone");
        }
        if (landingZones.stream().anyMatch(zone -> zone.routeDistance() >= route.length() - 28.0)) {
            throw new IllegalArgumentException("architecture landing zones must leave an approach segment");
        }
    }
}
