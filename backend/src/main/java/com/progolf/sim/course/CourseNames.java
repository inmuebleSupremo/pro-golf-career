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
    private static final String[] STYLE = {
            "Classic", "Championship", "Heathland", "Strategic", "Penal", "Resort"};

    /**
     * Region pools keyed to the course's environment, so a generated course's place evokes the same geography
     * as its style (a desert course sits in "Sonora", a links course in "Ayrshire") — this is what the
     * Development tour shows as an event's location, so the two must agree. Names are deliberately kept clear
     * of the real US states / countries the play-screen scene mapping keys on (Arizona, Scotland, Florida, …),
     * so a Development location never contradicts the classification-driven backdrop it resolves to.
     */
    private static String[] regionsFor(EnvironmentClassification classification) {
        return switch (classification) {
            case LINKS -> new String[] {"Ayrshire", "Fife", "East Lothian", "County Down", "Northumbria", "Connemara"};
            case PARKLAND -> new String[] {"Carolina", "Surrey", "Berkshire", "Hudson Valley", "Ohio Valley", "Westchester"};
            case DESERT -> new String[] {"Sonora", "Mojave", "Coachella", "Sedona", "Rio Grande", "High Desert"};
            case MOUNTAIN -> new String[] {"Cascades", "Rockies", "Sierra", "Blue Ridge", "Aspen", "Tahoe"};
            case COASTAL -> new String[] {"Algarve", "Monterey", "Amalfi", "Costa Brava", "The Cape", "Cabo"};
            case WOODLAND -> new String[] {"Pine Barrens", "Carolina Pines", "Black Forest", "Sherwood", "Ardennes", "Pinelands"};
        };
    }

    private CourseNames() {
    }

    static CourseIdentity generate(long courseSeed, EnvironmentClassification classification) {
        Rng rng = new com.progolf.sim.core.SplitMix64Rng(courseSeed);
        String name = pick(PREFIX, rng) + " " + pick(CORE, rng) + " " + pick(SUFFIX, rng);
        String region = pick(regionsFor(classification), rng);
        String style = pick(STYLE, rng);
        String id = "course-" + Long.toUnsignedString(courseSeed, 16);
        return new CourseIdentity(id, name, region, classification, style);
    }

    private static String pick(String[] options, Rng rng) {
        int index = (int) Math.floorMod(rng.nextLong(), options.length);
        return options[index];
    }
}
