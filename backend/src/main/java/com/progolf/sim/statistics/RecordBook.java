package com.progolf.sim.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The world Record Book (spec: records-archive, REQ-254/255): the current holder of each {@link RecordType}
 * plus an append-only progression log. Records are only ever set by {@link #challenge} from an observed
 * outcome; when one is surpassed the new holder becomes current and the previous holder remains in the
 * progression — nothing is fabricated or removed.
 */
public final class RecordBook {

    private final Map<RecordType, RecordHolder> current = new EnumMap<>(RecordType.class);
    private final Map<RecordType, List<RecordHolder>> progression = new EnumMap<>(RecordType.class);

    /**
     * Offers a candidate value for a record. If there is no current holder or the candidate surpasses it,
     * the candidate becomes the current holder and is appended to the progression. Returns whether it set a
     * new record.
     */
    public boolean challenge(RecordType type, String golferId, double value, int season) {
        RecordHolder existing = current.get(type);
        if (existing != null && !type.surpasses(value, existing.value())) {
            return false;
        }
        RecordHolder holder = new RecordHolder(type, golferId, value, season);
        current.put(type, holder);
        progression.computeIfAbsent(type, k -> new ArrayList<>()).add(holder);
        return true;
    }

    /** The current holder of a record, if any has been set. */
    public Optional<RecordHolder> current(RecordType type) {
        return Optional.ofNullable(current.get(type));
    }

    /** The full progression of a record (each successive holder), oldest first. */
    public List<RecordHolder> progression(RecordType type) {
        return Collections.unmodifiableList(new ArrayList<>(progression.getOrDefault(type, List.of())));
    }

    /** A snapshot of every record's current holder. */
    public Map<RecordType, RecordHolder> currentRecords() {
        return Collections.unmodifiableMap(new EnumMap<>(current));
    }

    /** An immutable capture of the record book (spec: world-snapshot): current holders + full progression. */
    public record Snapshot(Map<RecordType, RecordHolder> current, Map<RecordType, List<RecordHolder>> progression) {
    }

    Snapshot snapshot() {
        Map<RecordType, List<RecordHolder>> prog = new EnumMap<>(RecordType.class);
        progression.forEach((k, v) -> prog.put(k, List.copyOf(v)));
        return new Snapshot(Map.copyOf(current), prog);
    }

    /** Restores the record book in place (spec: world-snapshot); RecordBook is a final field of its owner. */
    void restoreFrom(Snapshot s) {
        current.clear();
        progression.clear();
        current.putAll(s.current());
        s.progression().forEach((k, v) -> progression.put(k, new ArrayList<>(v)));
    }
}
