package com.progolf.sim.economy;

import com.progolf.sim.core.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates reputation-gated sponsorship offers (spec: sponsorship, REQ-186). Both the number and value
 * of offers scale with a golfer's {@link CommercialReputation}, so stronger competitive reputation
 * broadly unlocks stronger commercial opportunities. Stateless and deterministic — every draw comes from
 * the supplied {@link Rng}.
 */
public final class SponsorshipMarket {

    /**
     * Offers available to a golfer of the given reputation for agreements starting in {@code startSeason}.
     * Each offer is from a distinct non-golf {@link SponsorBrand} matched to the golfer's commercial standing;
     * the money scales with both the golfer's reputation and the brand's (hidden) luxury tier — so a bigger
     * star attracts more prestigious brands paying far more.
     */
    public List<SponsorshipOffer> generateOffers(CommercialReputation reputation, int startSeason, Rng rng) {
        int count = EconomyConstants.OFFER_BASE + (int) (reputation.score() * EconomyConstants.OFFER_REP_SPAN);
        List<SponsorBrand> brands = shuffled(SponsorBrand.eligibleFor(reputation.tier()), rng);
        List<SponsorshipOffer> offers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SponsorBrand brand = brands.get(i % brands.size());
            double noise = 1.0 + rng.nextGaussian() * EconomyConstants.OFFER_VALUE_NOISE;
            double payment = Math.max(EconomyConstants.MIN_PAYMENT,
                    EconomyConstants.BASE_PAYMENT
                            * (1.0 + reputation.score() * EconomyConstants.PAYMENT_REP_SCALE)
                            * brand.luxuryMultiplier()
                            * Math.max(0.25, noise));
            double signing = payment * EconomyConstants.SIGNING_FRACTION;
            int duration = EconomyConstants.MIN_DURATION
                    + (int) (rng.nextDouble() * (EconomyConstants.MAX_DURATION - EconomyConstants.MIN_DURATION + 1));
            List<SponsorshipObjective> objectives = generateObjectives(reputation, payment, rng);
            SponsorshipAgreement agreement = new SponsorshipAgreement(brand.displayName(), brand.industry().label(),
                    payment, signing, startSeason, duration, objectives);
            offers.add(new SponsorshipOffer(agreement));
        }
        return offers;
    }

    /** A deterministic shuffle so a season's offers are distinct brands (until the eligible pool is exhausted). */
    private static List<SponsorBrand> shuffled(List<SponsorBrand> source, Rng rng) {
        List<SponsorBrand> copy = new ArrayList<>(source);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = (int) Math.floorMod(rng.nextLong(), i + 1);
            SponsorBrand tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return copy;
    }

    /** Higher-reputation offers carry more (and tougher) objectives, each rewarding a slice of payment. */
    private List<SponsorshipObjective> generateObjectives(CommercialReputation reputation, double payment, Rng rng) {
        int max = Math.min(EconomyConstants.MAX_OBJECTIVES, reputation.tier().ordinal());
        if (max <= 0) {
            return List.of();
        }
        int n = 1 + (int) (rng.nextDouble() * max); // 1..max
        List<SponsorshipObjective> objectives = new ArrayList<>(n);
        double reward = payment * EconomyConstants.OBJECTIVE_REWARD_FRACTION;
        for (int i = 0; i < n; i++) {
            ObjectiveType type = objectiveTypeFor(reputation, rng);
            objectives.add(new SponsorshipObjective(type, targetFor(type, reputation), reward));
        }
        return objectives;
    }

    private ObjectiveType objectiveTypeFor(CommercialReputation reputation, Rng rng) {
        // Lower tiers ask for participation/consistency; higher tiers ask for ranking/wins/milestones.
        ObjectiveType[] easy = {ObjectiveType.PARTICIPATION, ObjectiveType.CONSISTENCY};
        ObjectiveType[] hard = {ObjectiveType.RANKING, ObjectiveType.WINS, ObjectiveType.MILESTONE};
        ObjectiveType[] pool = reputation.tier().ordinal() >= ReputationTier.INTERNATIONAL.ordinal() ? hard : easy;
        return pool[(int) (rng.nextDouble() * pool.length)];
    }

    private double targetFor(ObjectiveType type, CommercialReputation reputation) {
        double r = reputation.score();
        return switch (type) {
            case PARTICIPATION -> 3 + Math.round(r * 3);      // 3..6 events
            case CONSISTENCY -> 0.4 + r * 0.4;                // 40%..80% cuts made
            case RANKING -> Math.max(1, Math.round((1.0 - r) * 40)); // stronger rep ⇒ tougher (lower) target
            case WINS -> 1 + Math.round(r * 2);               // 1..3 wins
            case MILESTONE -> 1 + Math.round(r * 5);          // 1..6 career wins
        };
    }
}
