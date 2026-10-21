package com.progolf.sim.putt;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.shot.GolferState;
import java.util.Objects;

/** Complete stable input to the isolated physical-putting solver. */
public record PhysicalPuttRequest(PhysicalPuttModelVersion modelVersion, GreenSurface surface, Position2d start,
                                  Position2d cup, PhysicalPuttIntent intent, Attributes attributes,
                                  GolferState state, long executionSeed, PhysicalPuttExecutionMode executionMode) {
    public PhysicalPuttRequest {
        Objects.requireNonNull(modelVersion, "modelVersion");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(cup, "cup");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(executionMode, "executionMode");
        if (!surface.contains(start) || !surface.contains(cup)) {
            throw new IllegalArgumentException("physical putt start and cup must be on the supplied green surface");
        }
    }
}
