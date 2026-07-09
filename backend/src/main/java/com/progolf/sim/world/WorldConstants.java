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
    public static final int FIELD_SIZE = 40;
    public static final int INITIAL_POPULATION = 160;
    public static final int COURSE_POOL_SIZE = 12;
    public static final int BASE_YEAR = 2000;

    // --- Initial tier distribution (fractions of the population; remainder goes to Development) ---
    public static final double ELITE_FRACTION = 0.08;
    public static final double PRIMARY_FRACTION = 0.17;
    public static final double SECONDARY_FRACTION = 0.30;
}
