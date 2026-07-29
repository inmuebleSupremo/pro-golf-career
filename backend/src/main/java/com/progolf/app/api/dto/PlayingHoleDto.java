package com.progolf.app.api.dto;

/**
 * The GraphQL view of the geometry of a hole being played (capability graphql-api, spec: web-hole-visualization):
 * the load-bearing facts a client needs to render the hole faithfully. Dimensions are in yards. Projects
 * {@code sim.course.GeneratedHole} plus the active round's {@code PinPosition}; no simulation record crosses the
 * API edge.
 *
 * <p>{@code pinLateral} and {@code pinDepth} are the active round's pin offsets (the pin's side is load-bearing;
 * a back pin plays longer). {@code courseType} is the host course's {@code EnvironmentClassification} name (the
 * same token the scene uses), which the client maps to a biome style. {@code layoutSeed} is the hole's stable
 * seed as a string (to survive JS 64-bit limits), from which the client synthesizes reproducible cosmetic
 * placement (hazard flanks, dogleg, vegetation).
 */
public record PlayingHoleDto(
        int holeNumber,
        int par,
        double length,
        double fairwayHalfWidth,
        double greenHalfWidth,
        double greenDepth,
        double elevationDelta,
        boolean hasGreensideBunker,
        boolean hasWater,
        boolean hasTrees,
        double pinLateral,
        double pinDepth,
        String courseType,
        String layoutSeed) {
}
