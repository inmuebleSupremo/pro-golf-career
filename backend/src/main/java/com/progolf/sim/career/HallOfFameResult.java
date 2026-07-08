package com.progolf.sim.career;

import java.util.Objects;

/**
 * The recorded outcome of a Hall-of-Fame eligibility evaluation (REQ-036): whether the career is
 * eligible plus a short summary of why. Permanent once recorded at retirement.
 */
public record HallOfFameResult(boolean eligible, String summary) {

    public HallOfFameResult {
        Objects.requireNonNull(summary, "summary");
    }
}
