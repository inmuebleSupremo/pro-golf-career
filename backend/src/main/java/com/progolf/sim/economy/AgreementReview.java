package com.progolf.sim.economy;

/**
 * The outcome of evaluating a {@link SponsorshipAgreement} against a season (spec: sponsorship, REQ-180):
 * the bonus earned from met objectives, how many objectives were met of the total, and whether the
 * relationship is renewable (enough objectives met). An agreement with no objectives is always renewable.
 */
public record AgreementReview(double bonusEarned, int objectivesMet, int objectivesTotal, boolean renewable) {

    /** Fraction of objectives met (1.0 when the agreement carries no objectives). */
    public double metFraction() {
        return objectivesTotal == 0 ? 1.0 : (double) objectivesMet / objectivesTotal;
    }
}
