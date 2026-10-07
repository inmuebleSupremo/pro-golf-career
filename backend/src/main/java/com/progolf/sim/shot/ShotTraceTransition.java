package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** A rules-driven, non-flight move from contact to the next legal playable position. */
public record ShotTraceTransition(RecoveryKind kind, Position2d from, Position2d to) {
    public ShotTraceTransition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (kind == RecoveryKind.NONE) {
            throw new IllegalArgumentException("trace transition requires a recovery kind");
        }
    }
}
