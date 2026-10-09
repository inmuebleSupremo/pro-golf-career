package com.progolf.app.api.dto;

import java.util.List;

/** Read-only renderer context only; it is not a gameplay terrain surface. */
public record LandscapeContextFeatureDto(String id, String kind, List<PositionDto> boundary) { }
