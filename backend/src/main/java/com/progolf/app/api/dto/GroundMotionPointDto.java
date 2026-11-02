package com.progolf.app.api.dto;

/** Resolver-authored bounce or ground stop in canonical yards. */
public record GroundMotionPointDto(String kind, PositionDto position, double heightYards) { }
