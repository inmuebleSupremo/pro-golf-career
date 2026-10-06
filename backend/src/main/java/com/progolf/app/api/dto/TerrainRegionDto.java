package com.progolf.app.api.dto;

import java.util.List;

/** One canonical polygon projected at the application edge. */
public record TerrainRegionDto(String surface, List<PositionDto> boundary) { }
