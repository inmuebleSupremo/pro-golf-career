package com.progolf.sim.course;

import com.progolf.sim.core.Rng;

/**
 * Deterministic procedural naming for courses. Cosmetic only — a simple seeded generator is sufficient
 * for Version 1. Same seed and classification always yield the same identity.
 */
final class CourseNames {

    private static final String[] PREFIX = {
            "Old", "Royal", "North", "West", "Silver", "Pine", "Cedar", "Stone", "Eagle", "Windy"};
    private static final String[] CORE = {
            "Ridge", "Valley", "Harbour", "Downs", "Moor", "Hollow", "Glen", "Bluff", "Point", "Meadow"};
    private static final String[] SUFFIX = {
            "Golf Club", "Links", "Golf Links", "Country Club", "National"};
    private static final String[] REGION = {
            "Ayrshire", "Carolina", "Algarve", "Highlands", "Cape", "Prairie", "Tuscany", "Cascades"};
    private static final String[] STYLE = {
            "Classic", "Championship", "Heathland", "Strategic", "Penal", "Resort"};

    private CourseNames() {
    }

    static CourseIdentity generate(long courseSeed, EnvironmentClassification classification) {
        Rng rng = new com.progolf.sim.core.SplitMix64Rng(courseSeed);
        String name = pick(PREFIX, rng) + " " + pick(CORE, rng) + " " + pick(SUFFIX, rng);
        String region = pick(REGION, rng);
        String style = pick(STYLE, rng);
        String id = "course-" + Long.toUnsignedString(courseSeed, 16);
        return new CourseIdentity(id, name, region, classification, style);
    }

    private static String pick(String[] options, Rng rng) {
        int index = (int) Math.floorMod(rng.nextLong(), options.length);
        return options[index];
    }
}
