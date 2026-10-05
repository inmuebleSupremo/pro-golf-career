package com.progolf.sim.shot;

/** How a contact became the next legal ball state. */
public enum RecoveryKind {
    NONE,
    WATER_DROP,
    STROKE_AND_DISTANCE_FALLBACK,
    OUT_OF_BOUNDS_REPLAY
}
