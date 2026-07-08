package com.progolf.sim.tournament;

/**
 * A Tournament's prestige tier (REQ-086). In this change it is an inline label used for eligibility and
 * prestige only; the full Tour hierarchy (membership, promotion/relegation) is a separate domain.
 */
public enum Tier {
    DEVELOPMENT,
    STANDARD,
    PREMIER,
    MAJOR
}
