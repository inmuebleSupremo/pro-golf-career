package com.progolf.sim.economy;

import java.util.Objects;

/**
 * A single performance objective attached to a {@link SponsorshipAgreement} (spec: sponsorship, REQ-180):
 * a target aligned with the golfer's competitive career, paying {@code reward} when met. Immutable and
 * self-evaluating against a {@link PerformanceSnapshot}.
 */
public record SponsorshipObjective(ObjectiveType type, double target, double reward) {

    public SponsorshipObjective {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(target) || !Double.isFinite(reward) || reward < 0) {
            throw new IllegalArgumentException("objective target/reward must be finite and reward >= 0");
        }
    }

    /** Whether this objective is satisfied by the season's performance. */
    public boolean isMet(PerformanceSnapshot snapshot) {
        return switch (type) {
            case PARTICIPATION -> snapshot.seasonEvents() >= target;
            case WINS -> snapshot.seasonWins() >= target;
            case RANKING -> snapshot.seasonRankingPosition() <= target;
            case CONSISTENCY -> snapshot.consistency() >= target;
            case MILESTONE -> snapshot.careerWins() >= target;
        };
    }
}
