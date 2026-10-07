package com.progolf.sim.shot;

/** Marker for deliberate shot decisions. Future putt intent is intentionally a separate contract. */
public sealed interface ShotIntent permits BallStrikeIntent {
}
