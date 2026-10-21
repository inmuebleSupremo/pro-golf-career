package com.progolf.app.api.dto;

import java.util.List;

/** One level-set polyline sampled directly from the physical green height field. */
public record GreenContourDto(double elevation, List<PositionDto> points) {
}
