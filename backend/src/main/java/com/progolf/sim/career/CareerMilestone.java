package com.progolf.sim.career;

/**
 * Objective career milestones recorded on their first occurrence (REQ-031). Duplicate recording of the
 * same milestone is prevented by the Career. The list is extensible without changing the fold logic.
 */
public enum CareerMilestone {
    FIRST_EVENT,
    FIRST_MADE_CUT,
    FIRST_TOP_10,
    FIRST_WIN
}
