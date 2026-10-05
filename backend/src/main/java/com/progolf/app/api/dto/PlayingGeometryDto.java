package com.progolf.app.api.dto;

import java.util.List;

/** The current hole's authoritative geometry, not SVG instructions. */
public record PlayingGeometryDto(PositionDto tee, PositionDto cup, List<PositionDto> playableBoundary,
                                 List<TerrainRegionDto> regions) { }
