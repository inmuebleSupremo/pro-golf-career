package com.progolf.sim.course;

/** Deterministic course-scale evidence for V6 review; it does not participate in gameplay. */
public record CourseLandscapeQualityReport(int placements, int featureCount, int shorelineRelationships,
                                           int bayRelationships, int woodlandCorridors, int clearingLandings,
                                           int parklandFields, int copseEdges, int linksExposed, int duneEdges,
                                           double maxTransitionToNextTee, boolean placementFallback) {
    public static CourseLandscapeQualityReport inspect(Course course) {
        CourseLandscapePlan plan = course.landscapePlan();
        if (plan == null) return new CourseLandscapeQualityReport(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, false);
        int shore = 0, bay = 0, woods = 0, clearings = 0, fields = 0, copses = 0, links = 0, dunes = 0;
        double maxTransition = 0.0;
        for (HolePlacement placement : plan.placements()) {
            maxTransition = Math.max(maxTransition, placement.transitionToNextTee());
            switch (placement.relationship()) {
                case SHORELINE_RUN -> shore++;
                case BAY_APPROACH -> bay++;
                case WOODLAND_CORRIDOR -> woods++;
                case CLEARING_LANDING -> clearings++;
                case PARKLAND_FIELD -> fields++;
                case COPSE_EDGE -> copses++;
                case LINKS_EXPOSED -> links++;
                case DUNE_EDGE -> dunes++;
                default -> { }
            }
        }
        return new CourseLandscapeQualityReport(plan.placements().size(), plan.features().size(), shore, bay, woods,
                clearings, fields, copses, links, dunes, maxTransition, plan.placementFallback());
    }
}
