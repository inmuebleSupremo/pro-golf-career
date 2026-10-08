package com.progolf.sim.course;

import java.util.Objects;

/** The presentation intent and authoritative resolved cup for one round. */
public record PinPlacement(PinPosition pin, Position2d cup) {
    public PinPlacement {
        Objects.requireNonNull(pin, "pin");
        Objects.requireNonNull(cup, "cup");
    }
}
