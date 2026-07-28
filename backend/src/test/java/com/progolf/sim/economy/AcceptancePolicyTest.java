package com.progolf.sim.economy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** financial-strategy spec: meaningful, deterministic financial decisions with trade-offs (REQ-181). */
class AcceptancePolicyTest {

    private static final LocalDate DAY = LocalDate.of(2000, 1, 1);

    private static SponsorshipOffer offer(String name, double payment) {
        return new SponsorshipOffer(new SponsorshipAgreement(name, "Watches", payment, 0, 2, 2, List.of()));
    }

    private static List<SponsorshipOffer> fiveOffers() {
        List<SponsorshipOffer> offers = new ArrayList<>();
        offers.add(offer("A", 60_000));
        offers.add(offer("B", 100_000));
        offers.add(offer("C", 80_000));
        offers.add(offer("D", 40_000));
        offers.add(offer("E", 120_000));
        return offers;
    }

    @Test
    void acceptsUpToTheCapAndNotEveryOffer() {
        FinancialAccount account = new FinancialAccount("g", 50_000, DAY);
        List<SponsorshipOffer> chosen = AcceptancePolicy.choose(account, fiveOffers(), 2);
        assertThat(chosen).hasSize(EconomyConstants.MAX_CONCURRENT_AGREEMENTS);
        assertThat(chosen.size()).isLessThan(5); // a trade-off, not accept-everything
    }

    @Test
    void prefersHigherValueOffers() {
        FinancialAccount account = new FinancialAccount("g", 50_000, DAY);
        List<SponsorshipOffer> chosen = AcceptancePolicy.choose(account, fiveOffers(), 2);
        List<String> sponsors = chosen.stream().map(o -> o.agreement().sponsor()).toList();
        assertThat(sponsors).containsExactlyInAnyOrder("E", "B", "C"); // top 3 by value
    }

    @Test
    void isDeterministic() {
        FinancialAccount account = new FinancialAccount("g", 50_000, DAY);
        assertThat(AcceptancePolicy.choose(account, fiveOffers(), 2))
                .isEqualTo(AcceptancePolicy.choose(account, fiveOffers(), 2));
    }

    @Test
    void acceptsNothingWhenSlotsAreFull() {
        FinancialAccount account = new FinancialAccount("g", 50_000, DAY);
        for (int i = 0; i < EconomyConstants.MAX_CONCURRENT_AGREEMENTS; i++) {
            account.signSponsorship(new SponsorshipAgreement("S" + i, "Watches", 50_000, 0, 2, 3, List.of()), DAY);
        }
        assertThat(AcceptancePolicy.choose(account, fiveOffers(), 2)).isEmpty();
    }
}
