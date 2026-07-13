package com.progolf.app.api.dto;

/**
 * The GraphQL input for one self-chosen career goal (capability graphql-api): the goal {@code type} as its
 * enum name and an optional {@code target} (a wins count, earnings amount, or majors count). When target is
 * null a boolean-style goal is created (target 1). Maps to {@code sim.control.CareerGoal} at the edge.
 */
public record CareerGoalInput(String type, Long target) {
}
