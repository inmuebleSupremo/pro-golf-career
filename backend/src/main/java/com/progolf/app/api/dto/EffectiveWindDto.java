package com.progolf.app.api.dto;

/**
 * Read-only signed resolver-effective wind flow in canonical hole coordinates. {@code unit} is deliberately
 * explicit: weather mph has already been translated through exposure and the synthetic local orientation.
 */
public record EffectiveWindDto(double x, double y, double magnitude, String unit) {
}
