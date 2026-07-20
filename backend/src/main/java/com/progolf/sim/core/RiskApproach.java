package com.progolf.sim.core;

/**
 * The risk approach a player chooses for their golfer's rounds (spec: player-control): how boldly they go at
 * pins and take on trouble. A layer-neutral choice that lives in {@code core} so the World and the player's
 * control state can carry it without reaching into the shot engine; the shot engine's concrete strategy is
 * mapped from it where that dependency is allowed. Ordered from safest to boldest.
 */
public enum RiskApproach {
    /** Play the percentages: protect position, take the safe line. Lower variance. */
    CONSERVATIVE,
    /** A measured game — the default many golfers settle into. */
    BALANCED,
    /** Hunt pins and take on trouble: more birdies and more blow-ups. The play that wins tournaments. */
    AGGRESSIVE
}
