package com.progolf.sim.career;

/**
 * A lightweight archived season boundary (REQ-029): the season index and the golfer's age at its end.
 * The Statistics/records domain can enrich this later; the Career only needs the boundary.
 */
public record SeasonRecord(int index, int endAge) {
}
