package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** Resolver-authored post-contact sample in canonical yards; first contact is ShotTrace.contact. */
public record GroundMotionPoint(Kind kind, Position2d position, double heightYards) {
    public enum Kind { BOUNCE_APEX, BOUNCE_LANDING, ROLL_STOP, BOUNDARY_STOP }

    public GroundMotionPoint {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(position, "position");
        if (!Double.isFinite(heightYards) || heightYards < 0) {
            throw new IllegalArgumentException("ground motion height must be finite and nonnegative");
        }
    }
}
