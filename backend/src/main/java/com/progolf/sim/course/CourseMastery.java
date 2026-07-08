package com.progolf.sim.course;

import java.util.Objects;

/**
 * The familiarity a specific player has with a specific Course (REQ-081). Ownership lives in this
 * relationship, not in the Course — the Course never stores per-player mastery. Mastery is a value in
 * [0,1], never transfers between courses, and persists across seasons (it is a plain value that outlives
 * any single season by being stored on its own).
 */
public record CourseMastery(String playerId, String courseId, double mastery) {

    public CourseMastery {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(courseId, "courseId");
        if (!Double.isFinite(mastery) || mastery < 0.0 || mastery > 1.0) {
            throw new IllegalArgumentException("mastery must be in [0,1]: " + mastery);
        }
    }

    /** A fresh relationship at zero familiarity. */
    public static CourseMastery initial(String playerId, String courseId) {
        return new CourseMastery(playerId, courseId, 0.0);
    }

    /** Returns a copy with mastery increased by {@code delta}, clamped to [0,1]. */
    public CourseMastery increasedBy(double delta) {
        double next = Math.max(0.0, Math.min(1.0, mastery + delta));
        return new CourseMastery(playerId, courseId, next);
    }
}
