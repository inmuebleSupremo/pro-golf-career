package com.progolf.sim.health;

import java.util.Objects;

/**
 * A significant health moment preserved for career narrative (spec: injury-recovery, REQ-219/223): a
 * notable injury sustained or a comeback from one. Immutable; the World appends these to its health
 * history.
 */
public record HealthEvent(int season, String golferId, Type type, String summary) {

    public enum Type {
        INJURY, COMEBACK
    }

    public HealthEvent {
        Objects.requireNonNull(golferId, "golferId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(summary, "summary");
    }
}
