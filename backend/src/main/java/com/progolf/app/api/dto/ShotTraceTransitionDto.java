package com.progolf.app.api.dto;

/** GraphQL projection of a rules recovery/replay, not physical ball flight. */
public record ShotTraceTransitionDto(String kind, PositionDto from, PositionDto to) { }
