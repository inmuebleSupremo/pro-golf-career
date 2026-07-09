package com.progolf.sim.world;

/**
 * World sizing configuration. Defaults come from {@link WorldConstants}; tests can supply a smaller
 * config to build a fast world.
 */
public record WorldConfig(int populationSize, int weeksPerSeason, int eventsPerTierPerSeason, int fieldSize, int coursePoolSize) {

    public WorldConfig {
        if (populationSize < 4 || weeksPerSeason < 1 || eventsPerTierPerSeason < 1 || fieldSize < 1 || coursePoolSize < 1) {
            throw new IllegalArgumentException("World config values must be positive and populationSize >= 4");
        }
    }

    /** The default (full-size) world configuration. */
    public static WorldConfig defaults() {
        return new WorldConfig(WorldConstants.INITIAL_POPULATION, WorldConstants.WEEKS_PER_SEASON,
                WorldConstants.EVENTS_PER_TIER_PER_SEASON, WorldConstants.FIELD_SIZE, WorldConstants.COURSE_POOL_SIZE);
    }
}
