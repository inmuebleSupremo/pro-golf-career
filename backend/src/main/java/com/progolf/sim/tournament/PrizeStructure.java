package com.progolf.sim.tournament;

/**
 * The payout curve of a Tournament (REQ-086). Maps a 1-based finishing position to a prize amount; the
 * amount is non-increasing with position. This records amounts only — no financial ledger (the Economy
 * domain consumes these later).
 */
public record PrizeStructure(double topPrize, double decay, int paidPositions) {

    public PrizeStructure {
        if (!(topPrize >= 0) || !Double.isFinite(topPrize)) {
            throw new IllegalArgumentException("topPrize must be finite and >= 0");
        }
        if (!(decay > 0) || decay > 1) {
            throw new IllegalArgumentException("decay must be in (0,1]: " + decay);
        }
        if (paidPositions < 0) {
            throw new IllegalArgumentException("paidPositions must be >= 0");
        }
    }

    /** The default decreasing curve from {@link TournamentConstants}. */
    public static PrizeStructure standard() {
        return new PrizeStructure(TournamentConstants.TOP_PRIZE, TournamentConstants.PRIZE_DECAY,
                TournamentConstants.PAID_POSITIONS);
    }

    /** Prize amount for a 1-based finishing position (0 beyond the paid positions). */
    public double amountForPosition(int position) {
        if (position < 1 || position > paidPositions) {
            return 0.0;
        }
        return topPrize * Math.pow(decay, position - 1);
    }
}
