package com.progolf.sim.media;

/**
 * The category of a {@link NewsEvent} (spec: news-generation, REQ-241/248). The set spans diverse forms of
 * success and significance, so the feed is not dominated by championship winners alone.
 */
public enum NewsType {
    TOURNAMENT_VICTORY,
    MAJOR_VICTORY,
    MAJOR_UPSET,
    WORLD_NUMBER_ONE,
    PROMOTION,
    RETIREMENT,
    CAREER_MILESTONE,
    INJURY,
    COMEBACK,
    SEVERE_WEATHER,
    RISING_PROSPECT,
    GOAL_ACHIEVED
}
