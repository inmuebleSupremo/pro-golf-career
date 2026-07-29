package com.progolf.app.api.dto;

/**
 * One lateral region of a reachable surface band (capability graphql-api, spec: web-hole-visualization): a
 * {@code surface} kind (the {@code Surface} enum name) occupying {@code |lateral| <= halfWidth} yards from the
 * centre outward. Regions within a band are ordered by increasing {@code halfWidth}; anything beyond the widest
 * region is out of bounds. Projects {@code sim.spatial.LateralRegion}.
 */
public record SurfaceRegionDto(String surface, double halfWidth) {
}
