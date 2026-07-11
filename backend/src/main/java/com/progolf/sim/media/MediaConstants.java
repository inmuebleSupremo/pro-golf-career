package com.progolf.sim.media;

/**
 * The single tunables surface for the Media, News &amp; World Narrative domain (spec: news-generation /
 * world-narrative / career-narrative). Prominence values order and gate the feed; the narrative thresholds
 * separate the descriptive career-narrative cases. All values live here so the feed's feel can be
 * calibrated in one place.
 */
public final class MediaConstants {

    private MediaConstants() {
    }

    // --- Prominence (0-100) per news type ---
    public static final int PROMINENCE_HALL_OF_FAME = 99; // induction is the pinnacle of a career's legacy
    public static final int PROMINENCE_MAJOR_VICTORY = 98; // a major win is the biggest news in the world
    public static final int PROMINENCE_WORLD_NUMBER_ONE = 95;
    public static final int PROMINENCE_GOAL_ACHIEVED = 80; // a self-chosen career goal reached (discoverable)
    public static final int PROMINENCE_MAJOR_UPSET = 85;
    public static final int PROMINENCE_RETIREMENT = 75;
    public static final int PROMINENCE_CAREER_MILESTONE = 70;
    public static final int PROMINENCE_SEVERE_WEATHER = 65;
    public static final int PROMINENCE_TOURNAMENT_VICTORY = 60;
    public static final int PROMINENCE_COMEBACK = 60;
    public static final int PROMINENCE_PROMOTION = 55;
    public static final int PROMINENCE_INJURY = 55;
    public static final int PROMINENCE_RISING_PROSPECT = 50;

    /** News at or above this prominence is "historically significant" and stays discoverable (REQ-246). */
    public static final int SIGNIFICANCE_THRESHOLD = 70;

    // --- News classification thresholds ---
    /** A winner ranked outside the top N (or unranked) is a major upset. */
    public static final int UPSET_RANKING_THRESHOLD = 25;

    // --- Career-narrative thresholds ---
    public static final int VETERAN_AGE = 35;
    public static final int PROSPECT_MAX_AGE = 25;
    public static final int PROSPECT_RANKING = 60;      // a young golfer ranked within this is a prospect
    public static final int DOMINANT_RANKING = 3;       // top-3 in the world
    public static final int DOMINANT_WINS = 10;         // career wins for a dominant champion
    public static final int BREAKTHROUGH_MAX_SEASONS = 3;
    public static final int DROUGHT_SEASONS = 4;        // seasons since last win to be "in a drought"
    public static final int CONTENDER_MIN_SEASONS = 4;
    public static final int CONTENDER_RANKING = 40;     // a steady, winless, well-ranked pro
    public static final int RESURGENCE_MAX_SEASONS_SINCE_WIN = 1; // an old golfer who has just won again
}
