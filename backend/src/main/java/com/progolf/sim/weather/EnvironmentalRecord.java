package com.progolf.sim.weather;

/**
 * A preserved piece of significant environmental history (spec: tournament-weather, REQ-235): the context
 * of a Tournament played under severe or record-setting conditions. Immutable; the World appends these to
 * its history for World continuity.
 */
public record EnvironmentalRecord(long seasonId, long tournamentId, double severity, String summary) {

    public EnvironmentalRecord {
        if (summary == null) {
            throw new IllegalArgumentException("summary must not be null");
        }
        if (!Double.isFinite(severity)) {
            throw new IllegalArgumentException("severity must be finite: " + severity);
        }
    }
}
