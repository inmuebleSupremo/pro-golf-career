package com.progolf.app.api.dto;

/** GraphQL projection of the authoritative post-contact ground-response endpoint. */
public record ShotTraceRollDto(PositionDto from, PositionDto to) { }
