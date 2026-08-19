package com.progolf.sim.media;

import java.util.Optional;

/**
 * Pure generators for {@link NewsEvent}s (spec: news-generation, REQ-241/242). Each takes real event
 * primitives (ids, names, tiers, positions, seasons) and returns a deterministic News Event with a
 * headline that references that real data and a prominence from {@link MediaConstants}. No randomness.
 */
public final class NewsFactory {

    private NewsFactory() {
    }

    public static NewsEvent tournamentVictory(int season, String golferId, String golferName, String tournamentName) {
        return of(season, NewsType.TOURNAMENT_VICTORY, golferId,
                golferName + " wins the " + tournamentName, MediaConstants.PROMINENCE_TOURNAMENT_VICTORY);
    }

    public static NewsEvent goalAchieved(int season, String golferId, String golferName, String description) {
        return of(season, NewsType.GOAL_ACHIEVED, golferId,
                golferName + " achieves a career goal: " + description, MediaConstants.PROMINENCE_GOAL_ACHIEVED);
    }

    public static NewsEvent achievementUnlocked(int season, String golferId, String golferName, String title) {
        return of(season, NewsType.ACHIEVEMENT_UNLOCKED, golferId,
                golferName + " unlocks an achievement: " + title, MediaConstants.PROMINENCE_ACHIEVEMENT_UNLOCKED);
    }

    public static NewsEvent majorVictory(int season, String golferId, String golferName, String tournamentName) {
        return of(season, NewsType.MAJOR_VICTORY, golferId,
                golferName + " wins the " + tournamentName + " — a major championship",
                MediaConstants.PROMINENCE_MAJOR_VICTORY);
    }

    public static NewsEvent maidenVictory(int season, String golferId, String golferName, String tournamentName) {
        return of(season, NewsType.CAREER_MILESTONE, golferId,
                golferName + " claims a maiden professional title at the " + tournamentName,
                MediaConstants.PROMINENCE_CAREER_MILESTONE);
    }

    public static NewsEvent majorUpset(int season, String golferId, String golferName, String tournamentName, int rankingPosition) {
        String rank = rankingPosition == Integer.MAX_VALUE ? "an unranked" : "world number " + rankingPosition;
        return of(season, NewsType.MAJOR_UPSET, golferId,
                "Upset: " + rank + " " + golferName + " storms to victory at the " + tournamentName,
                MediaConstants.PROMINENCE_MAJOR_UPSET);
    }

    public static NewsEvent hallOfFameInduction(int season, String golferId, String golferName) {
        return of(season, NewsType.HALL_OF_FAME_INDUCTION, golferId,
                golferName + " is inducted into the Hall of Fame", MediaConstants.PROMINENCE_HALL_OF_FAME);
    }

    public static NewsEvent worldNumberOne(int season, String golferId, String golferName) {
        return of(season, NewsType.WORLD_NUMBER_ONE, golferId,
                golferName + " rises to world number one", MediaConstants.PROMINENCE_WORLD_NUMBER_ONE);
    }

    public static NewsEvent promotion(int season, String golferId, String golferName, String toTier) {
        return of(season, NewsType.PROMOTION, golferId,
                golferName + " earns promotion to the " + toTier + " tour", MediaConstants.PROMINENCE_PROMOTION);
    }

    public static NewsEvent retirement(int season, String golferId, String golferName, int careerWins) {
        int prominence = Math.min(
                MediaConstants.PROMINENCE_RETIREMENT_MAX,
                MediaConstants.PROMINENCE_RETIREMENT_BASE
                        + Math.max(0, careerWins) * MediaConstants.PROMINENCE_RETIREMENT_PER_WIN);
        return of(season, NewsType.RETIREMENT, golferId,
                golferName + " retires after a career of " + careerWins + " win" + (careerWins == 1 ? "" : "s"),
                prominence);
    }

    public static NewsEvent injury(int season, String golferId, String golferName, String description) {
        return of(season, NewsType.INJURY, golferId,
                golferName + " sidelined: " + description, MediaConstants.PROMINENCE_INJURY);
    }

    public static NewsEvent comeback(int season, String golferId, String golferName, String description) {
        return of(season, NewsType.COMEBACK, golferId,
                golferName + " returns to competition — " + description, MediaConstants.PROMINENCE_COMEBACK);
    }

    public static NewsEvent risingProspect(int season, String golferId, String golferName, int rankingPosition) {
        return of(season, NewsType.RISING_PROSPECT, golferId,
                "Rising star: " + golferName + " breaks into the world top " + rankingPosition,
                MediaConstants.PROMINENCE_RISING_PROSPECT);
    }

    /** World-level news with no golfer subject. */
    public static NewsEvent severeWeather(int season, String tournamentName) {
        return new NewsEvent(season, NewsType.SEVERE_WEATHER, Optional.empty(),
                "Brutal conditions batter the " + tournamentName, MediaConstants.PROMINENCE_SEVERE_WEATHER);
    }

    private static NewsEvent of(int season, NewsType type, String golferId, String headline, int prominence) {
        return new NewsEvent(season, type, Optional.of(golferId), headline, prominence);
    }
}
