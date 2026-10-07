package com.progolf.sim.shot;

/**
 * Deliberate use of the existing non-spatial putting model. Line, speed, break, and spatial putting
 * controls intentionally remain out of scope.
 */
public record PuttIntent() implements ShotIntent {
}
