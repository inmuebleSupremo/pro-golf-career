package com.progolf.app.api.dto;

/**
 * The GraphQL view of one career achievement and the player's unlock state (capability graphql-api).
 * Projects the {@code sim.achievement.Achievement} catalogue plus the season it was earned. A still-locked
 * {@code secret} achievement withholds its {@code description} (null), so the UI can tease it without
 * spoiling it; every other field is always populated.
 *
 * @param id             stable catalogue id (the enum name)
 * @param category       category grouping (enum name)
 * @param categoryLabel  human-facing category heading
 * @param title          display title
 * @param description    one-line description; null for a still-locked secret achievement
 * @param secret         whether the description is withheld until unlocked
 * @param unlocked       whether this player has earned it
 * @param seasonUnlocked the season it was earned; null while locked
 */
public record AchievementDto(String id, String category, String categoryLabel, String title,
                             String description, boolean secret, boolean unlocked, Integer seasonUnlocked) {
}
