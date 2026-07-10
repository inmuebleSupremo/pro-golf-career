package com.progolf.sim.world;

import com.progolf.sim.control.CareerGoal;
import java.util.Objects;

/**
 * The player's progress toward one {@link CareerGoal} (spec: career-goals), evaluated on read from the live
 * career state: the current value of the underlying metric, the target to reach, and whether the goal is
 * achieved ({@code current >= target}). Purely observational — goals never gate play. Immutable.
 */
public record CareerGoalProgress(CareerGoal goal, long current, long target, boolean achieved) {

    public CareerGoalProgress {
        Objects.requireNonNull(goal, "goal");
    }
}
