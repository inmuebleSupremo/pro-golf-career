package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** One authoritative post-contact movement endpoint, including bounce travel and ground roll. */
public record ShotTraceRoll(Position2d from, Position2d to) {
    public ShotTraceRoll {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.equals(to)) throw new IllegalArgumentException("roll must move the ball");
    }
}
