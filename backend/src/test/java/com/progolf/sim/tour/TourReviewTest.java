package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.CareerStatus;
import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-movement spec: promotion to the Pro tour, relegation from it, determinism, and boundaries. */
class TourReviewTest {

    private static void registerAll(TourSystem system, List<ProfessionalGolfer> golfers, TourTier tier) {
        golfers.forEach(g -> system.register(g.player().id(), tier));
    }

    @Test
    void topOfDevelopmentEarnsProCardsToFillTheTour() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> field = TourFixtures.golfers(100);
        registerAll(system, field, TourTier.DEVELOPMENT);
        system.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.DEVELOPMENT);

        SeasonReviewResult review = system.reviewSeasonEnd();

        // The Pro tour was empty, so it fills to the number of cards it carries — no more, however many
        // developmental golfers would like one.
        int proCards = TourConstants.targetSize(TourTier.PRO, field.size());
        assertThat(review.promotions()).hasSize(proCards);
        assertThat(system.membershipOf(field.get(0).player().id())).contains(TourTier.PRO); // the best go up
        assertThat(system.membershipOf(field.get(99).player().id())).contains(TourTier.DEVELOPMENT); // the rest stay
        assertThat(system.currentSeason()).isEqualTo(2); // advanced
    }

    @Test
    void bottomOfTheProTourIsRelegatedToDevelopment() {
        TourSystem system = new TourSystem();
        // Enough golfers that the Pro tour is large enough for a full relegation cohort (the per-review move
        // fraction caps how many a small tour can shed).
        List<ProfessionalGolfer> all = TourFixtures.golfers(220);
        // A full Pro tour, plus a Development pool it can be refilled from.
        int proCards = TourConstants.targetSize(TourTier.PRO, all.size());
        List<ProfessionalGolfer> pro = all.subList(0, proCards);
        List<ProfessionalGolfer> dev = all.subList(proCards, all.size());
        registerAll(system, pro, TourTier.PRO);
        registerAll(system, dev, TourTier.DEVELOPMENT);
        system.recordResult(TourFixtures.resultInOrder("P", pro), TourTier.PRO);
        system.recordResult(TourFixtures.resultInOrder("D", dev), TourTier.DEVELOPMENT);

        SeasonReviewResult review = system.reviewSeasonEnd();

        assertThat(review.relegations()).hasSize(TourConstants.RELEGATE_COUNT);
        assertThat(review.relegations()).allMatch(m -> m.fromTier() == TourTier.PRO
                && m.toTier() == TourTier.DEVELOPMENT);
        // The Pro tour's worst finisher dropped; its best kept their card.
        assertThat(system.membershipOf(pro.get(proCards - 1).player().id())).contains(TourTier.DEVELOPMENT);
        assertThat(system.membershipOf(pro.get(0).player().id())).contains(TourTier.PRO);
    }

    @Test
    void aTourThinnedByDeparturesRefillsFromTheTourBelow() {
        // The Pro tour has lost most of its members (retirement); Development is full. The review must pull
        // golfers up to restore the Pro tour's card count rather than leave it hollow.
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> all = TourFixtures.golfers(100);
        List<ProfessionalGolfer> pro = all.subList(0, 2);
        List<ProfessionalGolfer> dev = all.subList(2, 100);
        registerAll(system, pro, TourTier.PRO);
        registerAll(system, dev, TourTier.DEVELOPMENT);
        system.recordResult(TourFixtures.resultInOrder("D", dev), TourTier.DEVELOPMENT);

        SeasonReviewResult review = system.reviewSeasonEnd();

        int proCards = TourConstants.targetSize(TourTier.PRO, all.size());
        long onPro = all.stream().filter(g -> system.membershipOf(g.player().id())
                .filter(t -> t == TourTier.PRO).isPresent()).count();
        assertThat(onPro).isEqualTo(proCards);
        assertThat(review.promotions()).allMatch(m -> m.toTier() == TourTier.PRO);
        // ...and it is Development's best who are pulled up.
        assertThat(system.membershipOf(dev.get(0).player().id())).contains(TourTier.PRO);
    }

    @Test
    void theTopTourIsNeverPromotedAndTheEntryTourIsNeverRelegated() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> all = TourFixtures.golfers(40);
        List<ProfessionalGolfer> pro = all.subList(0, 20);
        List<ProfessionalGolfer> dev = all.subList(20, 40);
        registerAll(system, pro, TourTier.PRO);
        registerAll(system, dev, TourTier.DEVELOPMENT);
        system.recordResult(TourFixtures.resultInOrder("P", pro), TourTier.PRO);
        system.recordResult(TourFixtures.resultInOrder("D", dev), TourTier.DEVELOPMENT);

        SeasonReviewResult review = system.reviewSeasonEnd();

        assertThat(review.movements()).noneSatisfy(m -> {
            assertThat(m.fromTier()).isEqualTo(TourTier.PRO);
            assertThat(m.type()).isEqualTo(MovementType.PROMOTION);
        });
        assertThat(review.movements()).noneSatisfy(m -> {
            assertThat(m.fromTier()).isEqualTo(TourTier.DEVELOPMENT);
            assertThat(m.type()).isEqualTo(MovementType.RELEGATION);
        });
    }

    @Test
    void aSmallProTourIsNeverEmptiedByAReview() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> three = TourFixtures.golfers(3);
        registerAll(system, three, TourTier.PRO);
        system.recordResult(TourFixtures.resultInOrder("P", three), TourTier.PRO);
        system.reviewSeasonEnd();

        long remaining = three.stream()
                .filter(g -> system.membershipOf(g.player().id()).orElseThrow() == TourTier.PRO).count();
        assertThat(remaining).isGreaterThanOrEqualTo(1); // the move fraction never empties a small tour
    }

    @Test
    void reviewIsDeterministic() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem a = new TourSystem();
        TourSystem b = new TourSystem();
        registerAll(a, field, TourTier.DEVELOPMENT);
        registerAll(b, field, TourTier.DEVELOPMENT);
        a.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.DEVELOPMENT);
        b.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.DEVELOPMENT);

        assertThat(a.reviewSeasonEnd().movements()).isEqualTo(b.reviewSeasonEnd().movements());
    }

    @Test
    void tourDomainMutatesNoPlayerOrResult() {
        TourSystem system = new TourSystem();
        ProfessionalGolfer g = TourFixtures.golfers(1).get(0);
        int drivingBefore = g.player().attributes().get(Attribute.DRIVING_ACCURACY);
        CareerStatus statusBefore = g.player().status();

        system.register(g.player().id(), TourTier.DEVELOPMENT);
        var result = TourFixtures.winFor(g);
        system.recordResult(result, TourTier.DEVELOPMENT);

        assertThat(g.player().attributes().get(Attribute.DRIVING_ACCURACY)).isEqualTo(drivingBefore);
        assertThat(g.player().status()).isEqualTo(statusBefore);
        assertThat(result.finishingOrder()).hasSize(1); // result unchanged
    }
}
