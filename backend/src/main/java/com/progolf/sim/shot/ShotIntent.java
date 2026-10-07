package com.progolf.sim.shot;

/** Marker for deliberate shot decisions. Putting remains a separate non-spatial contract. */
public sealed interface ShotIntent permits BallStrikeIntent, PuttIntent {
}
