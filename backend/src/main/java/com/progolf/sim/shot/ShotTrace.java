package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/**
 * The authoritative spatial facts of one observable shot. It intentionally describes endpoints and a
 * rules transition, plus an optional calculated endpoint-only ground response. It does not claim uncomputed
 * ballistic flight, bounce, or timing.
 */
public record ShotTrace(ClubId clubId, Position2d origin, AimPoint intendedAimPoint, ShotContact contact,
                        java.util.List<AirbornePoint> airbornePath, ShotTraceRoll roll, ShotTraceTransition transition, Position2d finalPoint) {
    public ShotTrace {
        Objects.requireNonNull(clubId, "clubId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(intendedAimPoint, "intendedAimPoint");
        Objects.requireNonNull(contact, "contact");
        airbornePath = airbornePath == null ? java.util.List.of() : java.util.List.copyOf(airbornePath);
        Objects.requireNonNull(finalPoint, "finalPoint");
        if (roll != null && transition != null) throw new IllegalArgumentException("roll and recovery are exclusive");
        if (roll != null && !roll.from().equals(contact.position())) throw new IllegalArgumentException("roll starts at contact");
        if (transition == null && roll == null && !finalPoint.equals(contact.position())) {
            throw new IllegalArgumentException("a moved final point requires a roll or trace transition");
        }
        if (roll != null && !finalPoint.equals(roll.to())) throw new IllegalArgumentException("roll endpoint is final point");
        if (transition != null && !finalPoint.equals(transition.to())) {
            throw new IllegalArgumentException("recovery endpoint is final point");
        }
    }

    public ShotTrace(ClubId clubId, Position2d origin, AimPoint intendedAimPoint, ShotContact contact,
                     ShotTraceTransition transition, Position2d finalPoint) {
        this(clubId, origin, intendedAimPoint, contact, java.util.List.of(), null, transition, finalPoint);
    }
}
