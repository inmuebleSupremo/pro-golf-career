package com.progolf.sim.health;

/**
 * A golfer's current availability for competition (spec: availability, REQ-221), derived from Physical
 * State. Only {@link #AVAILABLE} golfers may enter events.
 */
public enum Availability {
    /** Fit to compete. */
    AVAILABLE,
    /** No injury, but too fatigued to compete — a forced rest (REQ-222). */
    RESTING,
    /** Injured but in the late stage of rehabilitation. */
    RECOVERING,
    /** Injured and unable to compete. */
    INJURED
}
