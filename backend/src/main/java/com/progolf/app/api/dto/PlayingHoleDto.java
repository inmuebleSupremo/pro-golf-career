package com.progolf.app.api.dto;

/**
 * The GraphQL view of the geometry of a hole being played (capability graphql-api, spec: web-hole-visualization):
 * the load-bearing facts a client needs to render the hole faithfully. Dimensions are in yards. Projects
 * {@code sim.course.GeneratedHole} plus the active round's {@code PinPosition}; no simulation record crosses the
 * API edge.
 *
 * <p>{@code pinLateral} and {@code pinDepth} are the active round's pin offsets (the pin's side is load-bearing;
 * a back pin plays longer). {@code courseType} is the event's canonical scene token — the same token the scene
 * backdrop uses — which the client maps to a biome style, so the hole illustration always agrees with the
 * event's name, place, and photo. {@code layoutSeed} is the hole's stable
 * seed as a string (to survive JS 64-bit limits), from which the client may synthesize reproducible cosmetic
 * decoration only. Gameplay terrain — including hazards, fairway shape, and green/fringe — comes exclusively
 * from {@code geometry}.
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
        String layoutSeed,
        PlayingGeometryDto geometry,
        BallStateDto ball,
        EffectiveWindDto effectiveWind) {
}
