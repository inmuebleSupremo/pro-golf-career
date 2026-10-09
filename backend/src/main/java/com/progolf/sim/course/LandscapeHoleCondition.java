package com.progolf.sim.course;

/** Compact deterministic local samples passed to V6's retained bounded candidate planner. */
record LandscapeHoleCondition(LandscapeRelationship relationship, double sideBias, double enclosure, double openness) {
    LandscapeHoleCondition {
        if (!Double.isFinite(sideBias) || !Double.isFinite(enclosure) || !Double.isFinite(openness)) {
            throw new IllegalArgumentException("landscape samples must be finite");
        }
    }

    static LandscapeHoleCondition from(LandscapeRelationship relationship) {
        return switch (relationship) {
            case SHORELINE_RUN -> new LandscapeHoleCondition(relationship, 1.0, .05, .84);
            case BAY_APPROACH -> new LandscapeHoleCondition(relationship, -1.0, .12, .72);
            case INLAND_TURN -> new LandscapeHoleCondition(relationship, .62, .18, .68);
            case WOODLAND_CORRIDOR -> new LandscapeHoleCondition(relationship, -.48, .88, .25);
            case CLEARING_LANDING -> new LandscapeHoleCondition(relationship, .44, .70, .50);
            case PARKLAND_FIELD -> new LandscapeHoleCondition(relationship, .10, .30, .76);
            case COPSE_EDGE -> new LandscapeHoleCondition(relationship, -.58, .52, .58);
            case LINKS_EXPOSED -> new LandscapeHoleCondition(relationship, .30, .08, .94);
            case DUNE_EDGE -> new LandscapeHoleCondition(relationship, -.32, .16, .78);
            case OPEN_GROUND -> new LandscapeHoleCondition(relationship, 0.0, .10, .84);
        };
    }
}
