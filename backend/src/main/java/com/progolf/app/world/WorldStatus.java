package com.progolf.app.world;

/**
 * A read-only status view of a world session (spec: world-session), reflecting the engine's current state.
 * The application's provisional HTTP surface returns this; it exposes no gameplay mutation.
 */
public record WorldStatus(String id, int season, int week, int activePopulation) {
}
