package com.progolf.sim.economy;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SplitMix64Rng;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** sponsorship spec: objectives, agreements, reputation-gated offers, continuity, commercial reputation. */
class SponsorshipTest {

    private static final LocalDate DAY = LocalDate.of(2000, 1, 1);

    private static PerformanceSnapshot snapshot(int events, int wins, int cuts, int best, int rank, int careerWins) {
        return new PerformanceSnapshot(events, wins, cuts, best, rank, careerWins);
    }

    @Test
    void objectivesEvaluateAgainstPerformance() {
        assertThat(new SponsorshipObjective(ObjectiveType.PARTICIPATION, 3, 1000)
                .isMet(snapshot(4, 0, 2, 8, 30, 0))).isTrue();
        assertThat(new SponsorshipObjective(ObjectiveType.WINS, 2, 1000)
                .isMet(snapshot(10, 1, 6, 1, 5, 4))).isFalse();
        assertThat(new SponsorshipObjective(ObjectiveType.RANKING, 10, 1000)
                .isMet(snapshot(10, 0, 6, 4, 6, 0))).isTrue();
        assertThat(new SponsorshipObjective(ObjectiveType.CONSISTENCY, 0.5, 1000)
                .isMet(snapshot(4, 0, 3, 8, 30, 0))).isTrue();
        assertThat(new SponsorshipObjective(ObjectiveType.MILESTONE, 5, 1000)
                .isMet(snapshot(4, 0, 2, 8, 30, 3))).isFalse();
    }

    @Test
    void agreementEvaluatePaysMetObjectivesAndReportsRenewal() {
        SponsorshipAgreement a = new SponsorshipAgreement("Acme", 80_000, 40_000, 1, 3, List.of(
                new SponsorshipObjective(ObjectiveType.PARTICIPATION, 3, 10_000),   // met
                new SponsorshipObjective(ObjectiveType.WINS, 5, 10_000)));          // not met
        AgreementReview review = a.evaluate(snapshot(4, 1, 3, 2, 20, 1));
        assertThat(review.bonusEarned()).isEqualTo(10_000.0);
        assertThat(review.objectivesMet()).isEqualTo(1);
        assertThat(review.objectivesTotal()).isEqualTo(2);
        assertThat(review.renewable()).isTrue(); // 1/2 >= 0.5
    }

    @Test
    void agreementActivityWindowIsInclusive() {
        SponsorshipAgreement a = new SponsorshipAgreement("Acme", 80_000, 0, 2, 3, List.of());
        assertThat(a.lastActiveSeason()).isEqualTo(4);
        assertThat(a.isActiveIn(1)).isFalse();
        assertThat(a.isActiveIn(2)).isTrue();
        assertThat(a.isActiveIn(4)).isTrue();
        assertThat(a.isActiveIn(5)).isFalse();
    }

    @Test
    void agreementsConcludeIntoHistory() {
        FinancialAccount account = new FinancialAccount("g", 50_000, DAY);
        account.signSponsorship(new SponsorshipAgreement("Acme", 80_000, 0, 1, 2, List.of()), DAY);
        assertThat(account.activeAgreements(1)).hasSize(1);
        account.concludeExpiredAgreements(2); // lastActive = 2
        assertThat(account.activeAgreements(3)).isEmpty();
        assertThat(account.concludedAgreements()).hasSize(1);
        assertThat(account.sponsorshipHistory()).hasSize(1);
    }

    @Test
    void offersAreReputationGated() {
        SponsorshipMarket market = new SponsorshipMarket();
        CommercialReputation high = new CommercialReputation(0.9, ReputationTier.ELITE);
        CommercialReputation low = new CommercialReputation(0.1, ReputationTier.UNKNOWN);

        List<SponsorshipOffer> highOffers = market.generateOffers(high, 2, new SplitMix64Rng(1L));
        List<SponsorshipOffer> lowOffers = market.generateOffers(low, 2, new SplitMix64Rng(1L));

        assertThat(highOffers.size()).isGreaterThan(lowOffers.size());
        double highValue = highOffers.stream().mapToDouble(SponsorshipOffer::grossValue).sum();
        double lowValue = lowOffers.stream().mapToDouble(SponsorshipOffer::grossValue).sum();
        assertThat(highValue).isGreaterThan(lowValue);
    }

    @Test
    void offerGenerationIsDeterministic() {
        SponsorshipMarket market = new SponsorshipMarket();
        CommercialReputation rep = new CommercialReputation(0.7, ReputationTier.INTERNATIONAL);
        assertThat(market.generateOffers(rep, 3, new SplitMix64Rng(42L)))
                .isEqualTo(market.generateOffers(rep, 3, new SplitMix64Rng(42L)));
    }

    @Test
    void commercialReputationTracksCompetitiveWithoutBeingIdentical() {
        // Correlated: higher competitive ⇒ higher commercial on average.
        double highAvg = 0;
        double lowAvg = 0;
        int n = 200;
        for (int i = 0; i < n; i++) {
            highAvg += CommercialReputation.fromCompetitive(0.8, new SplitMix64Rng(i)).score();
            lowAvg += CommercialReputation.fromCompetitive(0.2, new SplitMix64Rng(i)).score();
        }
        assertThat(highAvg / n).isGreaterThan(lowAvg / n);

        // Not identical: the perturbation moves the score off the competitive input.
        Rng rng = new SplitMix64Rng(5L);
        CommercialReputation rep = CommercialReputation.fromCompetitive(0.5, rng);
        assertThat(rep.score()).isBetween(0.0, 1.0).isNotEqualTo(0.5);
    }
}
