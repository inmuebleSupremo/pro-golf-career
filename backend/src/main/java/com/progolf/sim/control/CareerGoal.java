package com.progolf.sim.control;

import java.util.Objects;

/**
 * A lightweight, self-chosen career ambition (spec: career-goals): a {@link GoalType} and, for targeted
 * goals, a numeric target (a career-wins count, an earnings amount, or a majors count). Boolean-style goals
 * (reach the top tour, world number one, Hall of Fame) use a target of 1. Immutable and value-keyed, so an
 * achieved goal is announced once. Framework-free — the player's own framing, never a gate.
 */
public record CareerGoal(GoalType type, long target) {

    public CareerGoal {
        Objects.requireNonNull(type, "type");
        if (target < 1) {
            throw new IllegalArgumentException("target must be >= 1: " + target);
        }
    }

    /** A boolean-style goal (target 1). */
    public static CareerGoal of(GoalType type) {
        return new CareerGoal(type, 1);
    }

    /** A targeted goal (e.g., win {@code target} tournaments, earn {@code target}). */
    public static CareerGoal of(GoalType type, long target) {
        return new CareerGoal(type, target);
    }
}
