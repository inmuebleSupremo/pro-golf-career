package com.progolf.sim.course;

import com.progolf.sim.shot.Strategy;
import java.util.List;
import java.util.Objects;

/** V5 semantic architecture retained beside, but never instead of, canonical terrain. */
public record HoleArchitecturePlan(RoutingForm routingForm, ArchitectureRoute route, List<FairwayWidthStation> widthStations,
                                   List<Position2d> greenFootprint, Position2d approachDirection,
                                   double structuralSignature) {
    public HoleArchitecturePlan {
        Objects.requireNonNull(routingForm, "routingForm");
        Objects.requireNonNull(route, "route");
        widthStations = List.copyOf(Objects.requireNonNull(widthStations, "widthStations"));
        greenFootprint = List.copyOf(Objects.requireNonNull(greenFootprint, "greenFootprint"));
        Objects.requireNonNull(approachDirection, "approachDirection");
        if (widthStations.size() < 3 || greenFootprint.size() < 5 || !Double.isFinite(structuralSignature)) {
            throw new IllegalArgumentException("V5 plan needs width rhythm, a simple green footprint and signature");
        }
        if (routingForm == RoutingForm.STRAIGHT && !route.intermediateAnchors().isEmpty()
                || routingForm == RoutingForm.GENTLE && route.intermediateAnchors().size() != 1
                || routingForm == RoutingForm.DOGLEG && route.intermediateAnchors().size() != 1
                || routingForm == RoutingForm.DOUBLE_DOGLEG && route.intermediateAnchors().size() != 2) {
            throw new IllegalArgumentException("V5 routing form must match bounded route anchors");
        }
        double prior = -1.0;
        for (FairwayWidthStation station : widthStations) {
            if (station.routeDistance() < prior || station.routeDistance() > route.length()) {
                throw new IllegalArgumentException("V5 width stations must be route ordered");
            }
            prior = station.routeDistance();
        }
    }

    /** Current compatible target: a normal long-club landing distance on the connected centreline. */
    public Position2d progressionTarget(Position2d ball, Strategy strategy) {
        if (ball.distanceTo(route.greenCenter()) <= 340.0) return route.greenCenter();
        double progress = route.projectDistance(ball);
        // The existing policy selects a normal long-club carry from remaining distance. A nearby turn-point target
        // would make that policy fly far beyond the intended corner; target the viable route corridor at its normal
        // landing distance instead. This remains advisory geometry, not a new forced-route rule.
        return route.pointAt(Math.min(route.length() - 54.0, progress + 270.0));
    }
}
