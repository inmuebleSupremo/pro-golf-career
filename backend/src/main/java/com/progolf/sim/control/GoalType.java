package com.progolf.sim.control;

/**
 * The kind of a self-chosen {@link CareerGoal} (spec: career-goals). Each maps to a live career metric the
 * World reads progress against. Boolean-style goals are achieved when their condition holds; the targeted
 * goals ({@link #WIN_A_MAJOR}, {@link #CAREER_WINS}, {@link #CAREER_EARNINGS}) are achieved when the metric
 * reaches the player's target. Goals never gate play — they only frame it.
 */
public enum GoalType {
    REACH_TOP_TOUR,
    WIN_A_MAJOR,
    WORLD_NUMBER_ONE,
    CAREER_WINS,
    CAREER_EARNINGS,
    HALL_OF_FAME
}
