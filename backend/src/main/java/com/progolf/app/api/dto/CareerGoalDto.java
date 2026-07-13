package com.progolf.app.api.dto;

/**
 * The GraphQL view of the player's progress toward one career goal (capability graphql-api): the goal
 * {@code type} (as its enum name), the target value, the live current value, and whether it is achieved.
 * Projects {@code sim.world.CareerGoalProgress}. Purely observational — goals never gate play.
 */
public record CareerGoalDto(String type, long target, long current, boolean achieved) {
}
