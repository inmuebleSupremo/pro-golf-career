package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.player.CareerStatus;
import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-movement spec: promotion/relegation, tier-edge behaviour, determinism, movement records, boundary. */
class TourReviewTest {

    private static void registerAll(TourSystem system, List<ProfessionalGolfer> golfers, TourTier tier) {
        golfers.forEach(g -> system.register(g.player().id(), tier));
    }

    @Test
    void topPerformersPromotedAndBottomRelegated() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        registerAll(system, field, TourTier.SECONDARY);
        system.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.SECONDARY);

        SeasonReviewResult review = system.reviewSeasonEnd();

        assertThat(system.membershipOf(field.get(0).player().id())).contains(TourTier.PRIMARY);    // top -> up
        assertThat(system.membershipOf(field.get(20).player().id())).contains(TourTier.SECONDARY); // middle stays
        assertThat(system.membershipOf(field.get(39).player().id())).contains(TourTier.DEVELOPMENT); // bottom -> down
        assertThat(system.currentSeason()).isEqualTo(2); // advanced

        // The tour above was empty, so it fills to the number of cards it carries — no more, however many
        // golfers below would like one.
        assertThat(review.promotions()).hasSize(TourConstants.targetSize(TourTier.PRIMARY, field.size()));
        assertThat(system.membershipOf(field.get(0).player().id())).contains(TourTier.PRIMARY);
        assertThat(review.relegations()).hasSize(TourConstants.RELEGATE_COUNT);
    }

    @Test
    void aTourThinnedByDeparturesRefillsFromTheTourBelow() {
        // Elite has lost most of its members (retirement); Primary is full. The review must pull golfers up
        // to restore Elite's card count rather than leave the ladder hollow at the top.
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> all = TourFixtures.golfers(100);
        List<ProfessionalGolfer> elite = all.subList(0, 2);
        List<ProfessionalGolfer> primary = all.subList(2, 100);
        registerAll(system, elite, TourTier.ELITE);
        registerAll(system, primary, TourTier.PRIMARY);
        system.recordResult(TourFixtures.resultInOrder("P", primary), TourTier.PRIMARY);

        SeasonReviewResult review = system.reviewSeasonEnd();

        int eliteCards = TourConstants.targetSize(TourTier.ELITE, all.size());
        long onElite = all.stream().filter(g -> system.membershipOf(g.player().id())
                .filter(t -> t == TourTier.ELITE).isPresent()).count();
        assertThat(onElite).isEqualTo(eliteCards);
        assertThat(review.promotions()).allMatch(m -> m.toTier() == TourTier.ELITE);
        // ...and it is the tour below's best who are pulled up.
        assertThat(system.membershipOf(primary.get(0).player().id())).contains(TourTier.ELITE);
    }

    @Test
    void highestTierIsNeverPromotedAndLowestIsNeverRelegated() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> all = TourFixtures.golfers(40);
        List<ProfessionalGolfer> elite = all.subList(0, 20);
        List<ProfessionalGolfer> dev = all.subList(20, 40);
        registerAll(system, elite, TourTier.ELITE);
        registerAll(system, dev, TourTier.DEVELOPMENT);
        system.recordResult(TourFixtures.resultInOrder("E", elite), TourTier.ELITE);
        system.recordResult(TourFixtures.resultInOrder("D", dev), TourTier.DEVELOPMENT);

        SeasonReviewResult review = system.reviewSeasonEnd();

        assertThat(review.movements()).noneSatisfy(m -> {
            assertThat(m.fromTier()).isEqualTo(TourTier.ELITE);
            assertThat(m.type()).isEqualTo(MovementType.PROMOTION);
        });
        assertThat(review.movements()).noneSatisfy(m -> {
            assertThat(m.fromTier()).isEqualTo(TourTier.DEVELOPMENT);
            assertThat(m.type()).isEqualTo(MovementType.RELEGATION);
        });
    }

    @Test
    void aSmallTierIsNeverEmptiedByAReview() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> three = TourFixtures.golfers(3);
        registerAll(system, three, TourTier.SECONDARY);
        system.recordResult(TourFixtures.resultInOrder("Q", three), TourTier.SECONDARY);
        system.reviewSeasonEnd();

        long remaining = three.stream().filter(g -> system.membershipOf(g.player().id()).orElseThrow() == TourTier.SECONDARY).count();
        assertThat(remaining).isGreaterThanOrEqualTo(1); // at most one up, one down, one stays
    }

    @Test
    void reviewIsDeterministic() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem a = new TourSystem();
        TourSystem b = new TourSystem();
        registerAll(a, field, TourTier.PRIMARY);
        registerAll(b, field, TourTier.PRIMARY);
        a.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.PRIMARY);
        b.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.PRIMARY);

        assertThat(a.reviewSeasonEnd().movements()).isEqualTo(b.reviewSeasonEnd().movements());
    }

    @Test
    void tourDomainMutatesNoPlayerOrResult() {
        TourSystem system = new TourSystem();
        ProfessionalGolfer g = TourFixtures.golfers(1).get(0);
        int drivingBefore = g.player().attributes().get(Attribute.DRIVING_ACCURACY);
        CareerStatus statusBefore = g.player().status();

        system.register(g.player().id(), TourTier.SECONDARY);
        var result = TourFixtures.winFor(g);
        system.recordResult(result, TourTier.SECONDARY);

        assertThat(g.player().attributes().get(Attribute.DRIVING_ACCURACY)).isEqualTo(drivingBefore);
        assertThat(g.player().status()).isEqualTo(statusBefore);
        assertThat(result.finishingOrder()).hasSize(1); // result unchanged
    }
}
