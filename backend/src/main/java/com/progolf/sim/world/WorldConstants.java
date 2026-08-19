package com.progolf.sim.world;

/**
 * The single tunables surface for the World domain (mirrors {@code SimConstants}). All are placeholder
 * calibration — structural behaviour (events resolve, seams fire, world reproduces) is what matters now;
 * magnitudes are tuned later with the economy and objectives.
 */
public final class WorldConstants {

    private WorldConstants() {
    }

    public static final int WEEKS_PER_SEASON = 30;
    public static final int EVENTS_PER_TIER_PER_SEASON = 6;
    public static final int FIELD_SIZE = 120;    // realistic tour-event field (spec: add-world-scale)
    public static final int INITIAL_POPULATION = 640;
    public static final int COURSE_POOL_SIZE = 12;
    // The world opens in this calendar year. Must equal the population's reference year, since a seeded
    // golfer's age is measured as BASE_YEAR - dateOfBirth (population DOBs are set against that same year).
    public static final int BASE_YEAR = com.progolf.sim.population.PopulationConstants.REFERENCE_YEAR;

    // Event prestige (spec: event-prestige).
    /** Cross-tour majors per season — the marquee events (like the four real-world majors). */
    public static final int MAJORS_PER_SEASON = 4;
    /** Elevated signature events per tour tier each season. */
    public static final int SIGNATURE_EVENTS_PER_TIER = 1;

    // The initial tier distribution lives with the ladder that maintains it: see TourConstants#targetSize.
}
