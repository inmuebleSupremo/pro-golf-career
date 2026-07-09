package com.progolf.sim.media;

import java.util.Objects;
import java.util.Optional;

/**
 * A single published news item (spec: news-generation, REQ-241/242): the season it occurred, its
 * {@link NewsType}, the subject golfer (empty for world-level news such as severe weather), a
 * deterministic headline referencing real data, and a prominence in [0,100]. Immutable. Every News Event
 * originates from a real World outcome — none are fabricated.
 */
public record NewsEvent(int season, NewsType type, Optional<String> subjectGolferId, String headline, int prominence) {

    public NewsEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(subjectGolferId, "subjectGolferId");
        Objects.requireNonNull(headline, "headline");
        if (prominence < 0 || prominence > 100) {
            throw new IllegalArgumentException("prominence must be in [0,100]: " + prominence);
        }
    }

    /** Whether this event is historically significant and stays discoverable (REQ-246). */
    public boolean isSignificant() {
        return prominence >= MediaConstants.SIGNIFICANCE_THRESHOLD;
    }
}
