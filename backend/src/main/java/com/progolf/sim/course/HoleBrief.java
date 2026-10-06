package com.progolf.sim.course;

import java.util.Objects;

/** Lightweight pre-geometry instruction for one V2 hole. */
public record HoleBrief(int number, int par, LengthBand lengthBand, StrategicArchetype archetype,
                        RecoverySeverity recoverySeverity) {
    public HoleBrief {
        if (number < 1 || number > 18) throw new IllegalArgumentException("Hole number must be 1..18");
        if (par < 3 || par > 5) throw new IllegalArgumentException("Par must be 3..5");
        Objects.requireNonNull(lengthBand, "lengthBand");
        Objects.requireNonNull(archetype, "archetype");
        Objects.requireNonNull(recoverySeverity, "recoverySeverity");
    }
}
