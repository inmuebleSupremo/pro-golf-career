package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/**
 * The authoritative spatial facts of one observable shot. It intentionally describes endpoints and a
 * rules transition only; it does not claim uncomputed ballistic flight, bounce, roll, or timing.
 */
public record ShotTrace(ClubId clubId, Position2d origin, AimPoint intendedAimPoint, ShotContact contact,
                        ShotTraceTransition transition, Position2d finalPoint) {
    public ShotTrace {
        Objects.requireNonNull(clubId, "clubId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(intendedAimPoint, "intendedAimPoint");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(finalPoint, "finalPoint");
        if (transition == null && !finalPoint.equals(contact.position())) {
            throw new IllegalArgumentException("a moved final point requires a trace transition");
        }
    }
}
