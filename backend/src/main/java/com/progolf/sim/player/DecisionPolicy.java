package com.progolf.sim.player;

import com.progolf.sim.shot.Strategy;

/**
 * The seam through which a simulation-controlled golfer makes decisions without human input (REQ-117).
 * This capability defines the interface only; richer decision-making (tournament entry, withdrawal,
 * per-shot strategy given full context) arrives with the domains that provide that context.
 */
public interface DecisionPolicy {

    /** The golfer's default shot strategy when no richer context is available. */
    Strategy defaultStrategy();
}
