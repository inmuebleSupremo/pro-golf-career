package com.progolf.sim.economy;

import java.util.Objects;

/**
 * A proposed {@link SponsorshipAgreement} presented to a golfer (spec: sponsorship / financial-strategy,
 * REQ-179/181). The golfer's decision policy chooses which offers to sign. Immutable.
 */
public record SponsorshipOffer(SponsorshipAgreement agreement) {

    public SponsorshipOffer {
        Objects.requireNonNull(agreement, "agreement");
    }

    /**
     * The gross value of the offer if fully realised: every season's payment, the signing bonus, and all
     * objective rewards. Used by the acceptance policy to compare offers.
     */
    public double grossValue() {
        double rewards = 0.0;
        for (SponsorshipObjective o : agreement.objectives()) {
            rewards += o.reward();
        }
        return agreement.perSeasonPayment() * agreement.durationSeasons() + agreement.signingBonus() + rewards;
    }
}
