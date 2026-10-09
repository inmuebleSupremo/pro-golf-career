package com.progolf.app.api.dto;

import java.util.List;

/** Stable local V6 landscape projection beside authoritative PlayingGeometry. */
public record LandscapeHoleContextDto(String courseIdentity, String relationship,
                                      List<LandscapeContextFeatureDto> features) { }
