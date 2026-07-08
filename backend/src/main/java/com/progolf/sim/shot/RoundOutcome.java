package com.progolf.sim.shot;

import java.util.List;
import java.util.Objects;

/**
 * The result of resolving a full hole for a golfer: the ordered shots played and the total strokes
 * (including penalties). Produced by {@link RoundResolver} for AI/abstracted resolution.
 */
public record RoundOutcome(List<ShotOutcome> shots, int totalStrokes) {

    public RoundOutcome {
        Objects.requireNonNull(shots, "shots");
        shots = List.copyOf(shots);
    }
}
