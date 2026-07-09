package com.progolf.sim.economy;

/**
 * The kind of a {@link SponsorshipObjective} (spec: sponsorship, REQ-180). Each aligns with the golfer's
 * competitive career and is evaluated against a {@link PerformanceSnapshot}. The specification does not
 * prescribe additional types; V1 covers the listed examples.
 */
public enum ObjectiveType {
    /** Play at least {@code target} events in the season. */
    PARTICIPATION,
    /** Finish the season ranked at position {@code target} or better (lower is better). */
    RANKING,
    /** Win at least {@code target} tournaments in the season. */
    WINS,
    /** Make the cut in at least a {@code target} fraction of events played. */
    CONSISTENCY,
    /** Reach at least {@code target} career wins (a career milestone). */
    MILESTONE
}
