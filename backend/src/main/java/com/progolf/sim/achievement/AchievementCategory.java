package com.progolf.sim.achievement;

/**
 * The thematic grouping an {@link Achievement} belongs to (spec: career-achievements). The order of the
 * constants is the intended display order — from the long career arc down to the incidental, fun feats.
 */
public enum AchievementCategory {

    /** The journey from unknown prospect to legendary pro (progression landmarks). */
    MILESTONE("Career Milestones"),
    /** Pure skill and shot-making on a single hole or round. */
    ON_COURSE("On-Course Feats"),
    /** High-stakes, competitive-pressure moments. */
    CLUTCH("Clutch"),
    /** Player, gear, and reputation progression (RPG-style). */
    PROGRESSION("Player Progression"),
    /** Lighthearted, situational feats that turn odd moments into fun. */
    QUIRKY("Quirky & Situational");

    private final String label;

    AchievementCategory(String label) {
        this.label = label;
    }

    /** The human-facing heading for this category. */
    public String label() {
        return label;
    }
}
