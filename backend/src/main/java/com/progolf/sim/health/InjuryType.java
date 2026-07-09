package com.progolf.sim.health;

/**
 * The body-area of an {@link Injury} (spec: injury-recovery, REQ-219). V1 covers the common golf injury
 * sites; the specification does not prescribe the set.
 */
public enum InjuryType {
    WRIST, BACK, SHOULDER, KNEE, ELBOW, HAND
}
