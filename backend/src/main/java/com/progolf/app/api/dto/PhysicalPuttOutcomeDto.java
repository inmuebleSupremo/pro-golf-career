package com.progolf.app.api.dto;

import java.util.List;

/** Observable, authoritative physical-putting result. */
public record PhysicalPuttOutcomeDto(List<PhysicalPuttTrajectoryPointDto> trajectory, PositionDto finalPosition,
                                     boolean holed, String terminalCondition) {
}
