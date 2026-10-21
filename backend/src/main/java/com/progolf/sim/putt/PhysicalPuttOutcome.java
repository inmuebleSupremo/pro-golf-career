package com.progolf.sim.putt;

import com.progolf.sim.course.Position2d;
import java.util.List;
import java.util.Objects;

/** Authoritative result from the isolated physical-putting model. */
public record PhysicalPuttOutcome(PhysicalPuttModelVersion modelVersion, PhysicalPuttExecution execution,
                                  List<PhysicalPuttTrajectoryPoint> trajectory, Position2d finalPosition,
                                  boolean holed, PhysicalPuttTerminalCondition terminalCondition) {
    public PhysicalPuttOutcome {
        Objects.requireNonNull(modelVersion, "modelVersion");
        Objects.requireNonNull(execution, "execution");
        trajectory = List.copyOf(Objects.requireNonNull(trajectory, "trajectory"));
        Objects.requireNonNull(finalPosition, "finalPosition");
        Objects.requireNonNull(terminalCondition, "terminalCondition");
        if (trajectory.isEmpty() || holed != (terminalCondition == PhysicalPuttTerminalCondition.CAPTURED)) {
            throw new IllegalArgumentException("physical putting outcome must have a coherent terminal state");
        }
    }
}
