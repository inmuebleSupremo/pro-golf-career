package com.progolf.sim.putt;

import com.progolf.sim.course.Position2d;

/** The seeded struck line and pace actually handed to the rolling simulation. */
public record PhysicalPuttExecution(Position2d direction, double initialSpeedYardsPerSecond,
                                    double directionErrorDegrees, double paceMultiplier) {
}
