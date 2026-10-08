package com.progolf.app.api.dto;

/** GraphQL projection of the authoritative spatial facts for one observable shot. */
public record ShotTraceDto(String club, PositionDto origin, AimPointDto aimPoint, ShotContactDto contact,
                           java.util.List<AirbornePointDto> airbornePath, ShotTraceRollDto roll,
                           ShotTraceTransitionDto transition, PositionDto finalPoint) { }
