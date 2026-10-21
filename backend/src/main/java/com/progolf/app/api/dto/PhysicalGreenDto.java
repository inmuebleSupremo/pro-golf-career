package com.progolf.app.api.dto;

import java.util.List;

/** Presentation-only samples taken directly from a physical event's authoritative GreenSurface. */
public record PhysicalGreenDto(List<GreenSlopeSampleDto> slopeSamples, List<GreenContourDto> contours) {
}
