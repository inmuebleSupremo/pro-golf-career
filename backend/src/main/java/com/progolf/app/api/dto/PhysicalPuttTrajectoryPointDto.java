package com.progolf.app.api.dto;

/** One chronological, solver-authored physical-putt sample for client playback. */
public record PhysicalPuttTrajectoryPointDto(double timeSeconds, PositionDto position,
                                             double speedYardsPerSecond) {
}
