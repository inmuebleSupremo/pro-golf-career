package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** Separates reported contact from the legal state that follows it. */
public record ShotSettlement(ShotContact contact, Position2d recoveryPosition, RecoveryKind recoveryKind,
                             BallState ball) {
    public ShotSettlement {
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(recoveryKind, "recoveryKind");
        Objects.requireNonNull(ball, "ball");
        if (recoveryKind == RecoveryKind.NONE && recoveryPosition != null) {
            throw new IllegalArgumentException("normal settlement has no recovery position");
        }
    }
}
