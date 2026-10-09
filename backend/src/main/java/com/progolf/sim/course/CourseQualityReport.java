package com.progolf.sim.course;

import java.util.HashSet;
import java.util.Set;

/** Compact deterministic structural evidence for V4/V5 complete-course comparison. */
public record CourseQualityReport(int holes, int straightHoles, int gentleHoles, int doglegHoles, int twoTurnHoles,
                                  int asymmetricWidthStations, int bunkerRegions, int treeRegions, int distinctSignatureBuckets,
                                  double meanRouteChordRatio) {
    public static CourseQualityReport inspect(Course course) {
        int straight = 0;
        int gentle = 0;
        int dogleg = 0;
        int twoTurns = 0;
        int asymmetric = 0;
        int bunkers = 0;
        int trees = 0;
        double routeChord = 0.0;
        Set<Long> buckets = new HashSet<>();
        for (GeneratedHole hole : course.holes()) {
            if (hole.architecturePlan() != null) {
                HoleArchitecturePlan plan = hole.architecturePlan();
                switch (plan.routingForm()) {
                    case STRAIGHT -> straight++;
                    case GENTLE -> gentle++;
                    case DOGLEG -> dogleg++;
                    case DOUBLE_DOGLEG -> twoTurns++;
                }
                asymmetric += (int) plan.widthStations().stream()
                        .filter(station -> Math.abs(station.leftHalfWidth() - station.rightHalfWidth()) > 0.5).count();
                routeChord += plan.route().length() / plan.route().teeOrigin().distanceTo(plan.route().greenCenter());
                buckets.add(Math.round(plan.structuralSignature() * 100.0));
            } else if (hole.spatialPlan() != null) {
                int turns = hole.spatialPlan().route().intermediateAnchors().size();
                if (turns == 0) straight++;
                else dogleg++;
                HoleRoute route = hole.spatialPlan().route();
                routeChord += (route.length() + route.approachAnchor().distanceTo(hole.spatialPlan().greenCenter()))
                        / route.teeOrigin().distanceTo(hole.spatialPlan().greenCenter());
                buckets.add(Math.round(hole.spatialPlan().route().length()));
            } else {
                straight++;
                routeChord += 1.0;
                buckets.add(Math.round(hole.length()));
            }
            bunkers += (int) hole.geometry().regions().stream().filter(region -> region.surface() == com.progolf.sim.spatial.Surface.BUNKER).count();
            trees += (int) hole.geometry().regions().stream().filter(region -> region.surface() == com.progolf.sim.spatial.Surface.TREES).count();
        }
        return new CourseQualityReport(course.holes().size(), straight, gentle, dogleg, twoTurns, asymmetric, bunkers, trees, buckets.size(),
                routeChord / course.holes().size());
    }
}
