package com.progolf.sim.tournament;

/**
 * A Tournament's tour tier (REQ-086) — the competitive level of the tour whose members contest it,
 * mirroring the {@code TourTier} ladder (Development / Standard / Premier / Elite). Distinct from a
 * Tournament's {@link EventPrestige} (regular / signature / major), which is an orthogonal reward weight.
 */
public enum Tier {
    DEVELOPMENT(TournamentConstants.PURSE_DEVELOPMENT),
    STANDARD(TournamentConstants.PURSE_STANDARD),
    PREMIER(TournamentConstants.PURSE_PREMIER),
    ELITE(TournamentConstants.PURSE_ELITE);

    private final double purseMultiplier;

    Tier(double purseMultiplier) {
        this.purseMultiplier = purseMultiplier;
    }

    /** The tour tier's purse multiplier (spec: financial-strategy): higher tiers pay larger purses. */
    public double purseMultiplier() {
        return purseMultiplier;
    }
}
