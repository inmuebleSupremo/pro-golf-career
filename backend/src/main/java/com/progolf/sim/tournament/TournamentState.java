package com.progolf.sim.tournament;

/**
 * The tournament lifecycle states in canonical order (REQ-087). States advance one at a time and are
 * never skipped, except that PLAYOFF is entered only when the final round ends in a tie for the lead.
 */
public enum TournamentState {
    SCHEDULED,
    REGISTRATION_OPEN,
    FIELD_CONFIRMED,
    ROUND_1,
    ROUND_2,
    CUT,
    ROUND_3,
    ROUND_4,
    PLAYOFF,
    COMPLETED
}
