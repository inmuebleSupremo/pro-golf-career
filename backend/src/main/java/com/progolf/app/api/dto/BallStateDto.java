package com.progolf.app.api.dto;

/** API projection of the legal origin for the next shot. */
public record BallStateDto(PositionDto position, String lie) { }
