package com.progolf.sim.career;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One immutable entry in a Career's chronological history (REQ-033): a dated, described significant
 * event (a tournament participation or a milestone). Entries are append-only and never modified.
 */
public record CareerHistoryEntry(LocalDate date, Type type, String description) {

    public enum Type {
        TOURNAMENT,
        MILESTONE,
        RETIREMENT
    }

    public CareerHistoryEntry {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
    }
}
