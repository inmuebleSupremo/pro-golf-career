package com.progolf.sim.statistics;

import java.util.Objects;

/**
 * A single record entry (spec: records-archive, REQ-254/255): the record type, the golfer who holds it, the
 * value achieved, and the season it was established — referencing the gameplay from which it emerged.
 * Immutable; previous holders are preserved as record progression when surpassed.
 */
public record RecordHolder(RecordType type, String golferId, double value, int season) {

    public RecordHolder {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(golferId, "golferId");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite: " + value);
        }
    }
}
