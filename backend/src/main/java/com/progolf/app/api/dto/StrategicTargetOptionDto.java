package com.progolf.app.api.dto;

/** Application projection of one advisory, backend-evaluated V4 landing option. */
public record StrategicTargetOptionDto(String role, AimPointDto aimPoint, String suggestedClub,
                                       String suggestedFamily, String routeSummary, String exposureSummary) { }
