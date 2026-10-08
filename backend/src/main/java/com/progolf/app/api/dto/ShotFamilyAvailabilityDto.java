package com.progolf.app.api.dto;

/** Server-authored availability for one technique and currently selected club/lie. */
public record ShotFamilyAvailabilityDto(String family, boolean available, String reason,
                                        java.util.List<ShotShapeAvailabilityDto> shapes) { }
