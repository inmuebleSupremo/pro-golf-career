package com.progolf.sim.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The deterministic policy by which a golfer decides which sponsorship offers to sign (spec:
 * financial-strategy, REQ-181). It is a genuine trade-off, not auto-optimisation: only a limited number
 * of concurrent agreements may be held, and offers laden with objectives the golfer may struggle to meet
 * are discounted. The same rules apply to every golfer, so decisions are reproducible.
 */
public final class AcceptancePolicy {

    private AcceptancePolicy() {
    }

    /**
     * Chooses which offers to accept, given the account's current agreements and the season the new
     * agreements would start. Returns at most the number of free agreement slots, best value first.
     */
    public static List<SponsorshipOffer> choose(FinancialAccount account, List<SponsorshipOffer> offers, int upcomingSeason) {
        int slots = EconomyConstants.MAX_CONCURRENT_AGREEMENTS - account.activeAgreementCount(upcomingSeason);
        if (slots <= 0 || offers.isEmpty()) {
            return List.of();
        }
        List<SponsorshipOffer> ranked = new ArrayList<>(offers);
        // Value net of objective difficulty; ties broken by sponsor name for determinism.
        ranked.sort(Comparator.comparingDouble(AcceptancePolicy::valuation).reversed()
                .thenComparing(o -> o.agreement().sponsor()));
        return List.copyOf(ranked.subList(0, Math.min(slots, ranked.size())));
    }

    /** An offer's gross value discounted by how many objectives it demands (risk of missing them). */
    private static double valuation(SponsorshipOffer offer) {
        int objectives = offer.agreement().objectives().size();
        double discount = 1.0 - EconomyConstants.OBJECTIVE_DIFFICULTY_DISCOUNT * objectives;
        return offer.grossValue() * Math.max(0.0, discount);
    }
}
