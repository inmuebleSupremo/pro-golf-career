package com.progolf.sim.putt;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** Complete stable input to the isolated physical-putting solver. */
public record PhysicalPuttRequest(PhysicalPuttModelVersion modelVersion, GreenSurface surface, Position2d start,
                                  Position2d cup, PhysicalPuttIntent intent, Attributes attributes,
                                  GolferState state, long executionSeed, PhysicalPuttExecutionMode executionMode,
                                  CourseGeometry fringeGeometry) {
    /** Green starts and practice callers keep their original solver inputs and behaviour. */
    public PhysicalPuttRequest(PhysicalPuttModelVersion modelVersion, GreenSurface surface, Position2d start,
                               Position2d cup, PhysicalPuttIntent intent, Attributes attributes,
                               GolferState state, long executionSeed, PhysicalPuttExecutionMode executionMode) {
        this(modelVersion, surface, start, cup, intent, attributes, state, executionSeed, executionMode, null);
    }

    public PhysicalPuttRequest {
        Objects.requireNonNull(modelVersion, "modelVersion");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(cup, "cup");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(executionMode, "executionMode");
        if (!surface.contains(cup) || !surface.contains(start)
                && (fringeGeometry == null || fringeGeometry.surfaceAt(start) != Surface.FRINGE)) {
            throw new IllegalArgumentException("physical putt start must be on green or actual fringe; cup must be on green");
        }
    }
}
