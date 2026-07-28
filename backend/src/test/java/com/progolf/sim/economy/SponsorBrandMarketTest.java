package com.progolf.sim.economy;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SplitMix64Rng;
import org.junit.jupiter.api.Test;

/** sponsorship: offers are real non-golf brands, matched to standing, with money scaled by reputation × luxury. */
class SponsorBrandMarketTest {

    private final SponsorshipMarket market = new SponsorshipMarket();

    @Test
    void offersAreNamedNonGolfBrandsWithAnIndustry() {
        var offers = market.generateOffers(new CommercialReputation(0.5, ReputationTier.NATIONAL), 2,
                new SplitMix64Rng(1L));
        assertThat(offers).isNotEmpty().allSatisfy(o -> {
            assertThat(o.agreement().sponsor()).isNotBlank().doesNotContain("Sponsor-"); // not a placeholder
            assertThat(o.agreement().industry()).isNotBlank();
        });
    }

    @Test
    void aJourneymanGetsModestBrandsAndASuperstarLuxuryOnes() {
        // Only own-and-just-below tiers court a golfer, so luxury status tracks standing.
        assertThat(SponsorBrand.eligibleFor(ReputationTier.REGIONAL))
                .isNotEmpty().noneMatch(b -> b.luxury() == ReputationTier.ELITE);
        assertThat(SponsorBrand.eligibleFor(ReputationTier.ELITE))
                .anyMatch(b -> b.luxury() == ReputationTier.ELITE)
                .noneMatch(b -> b.luxury() == ReputationTier.REGIONAL);
    }

    @Test
    void moneyScalesWithReputationAndLuxury() {
        double bestModest = market.generateOffers(new CommercialReputation(0.25, ReputationTier.REGIONAL), 2,
                        new SplitMix64Rng(7L)).stream()
                .mapToDouble(o -> o.agreement().perSeasonPayment()).max().orElse(0);
        double bestLuxury = market.generateOffers(new CommercialReputation(0.95, ReputationTier.ELITE), 2,
                        new SplitMix64Rng(7L)).stream()
                .mapToDouble(o -> o.agreement().perSeasonPayment()).max().orElse(0);
        assertThat(bestLuxury).as("a superstar's luxury deal dwarfs a journeyman's").isGreaterThan(bestModest * 3);
    }
}
