package com.progolf.sim.tournament;

/**
 * The prestige of a tournament (spec: event-prestige), orthogonal to its tour {@link Tier}. Regular tour
 * events, elevated Signature events within a tour, and cross-tour Majors — the marquee accomplishments.
 * Prestige weights the rewards a completed result feeds (ranking points and prize money) and its career
 * significance; it NEVER affects how shots or scores are resolved. Multipliers are monotone by construction
 * (major &gt; signature &gt; regular) and their magnitudes live in {@link TournamentConstants}.
 */
public enum EventPrestige {
    REGULAR(1.0, 1.0),
    SIGNATURE(TournamentConstants.SIGNATURE_RANKING_WEIGHT, TournamentConstants.SIGNATURE_PURSE_WEIGHT),
    TOUR_CHAMPIONSHIP(TournamentConstants.TOUR_CHAMPIONSHIP_RANKING_WEIGHT,
            TournamentConstants.TOUR_CHAMPIONSHIP_PURSE_WEIGHT),
    MAJOR(TournamentConstants.MAJOR_RANKING_WEIGHT, TournamentConstants.MAJOR_PURSE_WEIGHT);

    private final double rankingWeight;
    private final double purseWeight;

    EventPrestige(double rankingWeight, double purseWeight) {
        this.rankingWeight = rankingWeight;
        this.purseWeight = purseWeight;
    }

    /** Multiplier applied to ranking points earned in an event of this prestige. */
    public double rankingWeight() {
        return rankingWeight;
    }

    /** Multiplier applied to the event's purse (top prize) for an event of this prestige. */
    public double purseWeight() {
        return purseWeight;
    }

    /** Whether this is a major — the pinnacle, cross-tour marquee event. */
    public boolean isMajor() {
        return this == MAJOR;
    }
}
