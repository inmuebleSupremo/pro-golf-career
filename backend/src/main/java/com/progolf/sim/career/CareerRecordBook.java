package com.progolf.sim.career;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The player's append-only ledger of rich per-event records (spec: career-records). It accumulates one
 * {@link CareerEventRecord} for every event the human player competes in, preserving the full detail the
 * career-records surface needs — victory venues, finishing scores, round cards, and performance metrics.
 *
 * <p>Player-scoped and analytical: the World appends a record as each of the player's events completes, and
 * the book only ever grows. It exposes the raw, chronologically-ordered ledger; grouping (per recurring
 * event) and summarising are left to the read/projection layer, keeping this a pure store.
 */
public final class CareerRecordBook {

    private final List<CareerEventRecord> records = new ArrayList<>();

    /** Appends one completed event's record for the player. */
    public void record(CareerEventRecord record) {
        records.add(Objects.requireNonNull(record, "record"));
    }

    /** Every recorded event, in the order it was recorded (chronological). */
    public List<CareerEventRecord> all() {
        return List.copyOf(records);
    }

    /** An immutable capture of the ledger (spec: world-snapshot). */
    public record Snapshot(List<CareerEventRecord> records) {
        public Snapshot {
            records = List.copyOf(records);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(records);
    }

    /** Restores a ledger from a snapshot (spec: world-snapshot). */
    public static CareerRecordBook restore(Snapshot s) {
        CareerRecordBook book = new CareerRecordBook();
        book.records.addAll(s.records());
        return book;
    }
}
