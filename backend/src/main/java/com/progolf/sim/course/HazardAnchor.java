package com.progolf.sim.course;

import java.util.Objects;

/** Immutable semantic V4 reference; coordinates remain compiler-owned. */
public record HazardAnchor(HazardAnchorType type, LandingZoneRole landingZoneRole, double routeDistance,
                           GreenSide greenSide) {
    public HazardAnchor {
        Objects.requireNonNull(type, "type");
        switch (type) {
            case LANDING_ZONE -> Objects.requireNonNull(landingZoneRole, "landingZoneRole");
            case ROUTE_DISTANCE -> {
                if (!Double.isFinite(routeDistance) || routeDistance < 0.0) {
                    throw new IllegalArgumentException("routeDistance must be finite and >= 0");
                }
            }
            case DOGLEG_CORNER -> {
                if (!Double.isNaN(routeDistance)) throw new IllegalArgumentException("dogleg anchor carries no distance");
            }
            case GREEN_SIDE -> Objects.requireNonNull(greenSide, "greenSide");
        }
    }

    public static HazardAnchor landingZone(LandingZoneRole role) {
        return new HazardAnchor(HazardAnchorType.LANDING_ZONE, role, Double.NaN, null);
    }

    public static HazardAnchor routeDistance(double distance) {
        return new HazardAnchor(HazardAnchorType.ROUTE_DISTANCE, null, distance, null);
    }

    public static HazardAnchor doglegCorner() {
        return new HazardAnchor(HazardAnchorType.DOGLEG_CORNER, null, Double.NaN, null);
    }

    public static HazardAnchor greenSide(GreenSide side) {
        return new HazardAnchor(HazardAnchorType.GREEN_SIDE, null, Double.NaN, side);
    }
}
