package com.progolf.sim.putt;

import com.progolf.sim.course.Position2d;

/** One chronological authoritative ball sample for later frontend playback. */
public record PhysicalPuttTrajectoryPoint(double timeSeconds, Position2d position, double speedYardsPerSecond) {
}
