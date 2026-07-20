package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-movement spec: a golfer promoted last season holds a card for the tier they earned. */
class TourExemptionTest {

    private static void registerAll(TourSystem system, List<ProfessionalGolfer> golfers, TourTier tier) {
        golfers.forEach(g -> system.register(g.player().id(), tier));
    }

    /** A system whose season-1 review promotes the top of Development into the Pro tour. */
    private static TourSystem promotedFromDevelopment(List<ProfessionalGolfer> field) {
        TourSystem system = new TourSystem();
        registerAll(system, field, TourTier.DEVELOPMENT);
        system.recordResult(TourFixtures.resultInOrder("D", field), TourTier.DEVELOPMENT);
        system.reviewSeasonEnd();
        return system;
    }

    @Test
    void promotedGolfersAreExemptTheFollowingSeason() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem system = promotedFromDevelopment(field);

        String promoted = field.get(0).player().id();
        assertThat(system.membershipOf(promoted)).contains(TourTier.PRO);
        assertThat(system.isExempt(promoted)).isTrue();
    }

    @Test
    void golfersWhoDidNotMoveAreNotExempt() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem system = promotedFromDevelopment(field);

        // A mid-table golfer who stayed on Development holds no card.
        assertThat(system.isExempt(field.get(20).player().id())).isFalse();
    }

    @Test
    void anExemptionLastsExactlyOneSeason() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem system = promotedFromDevelopment(field);
        String promoted = field.get(0).player().id();
        assertThat(system.isExempt(promoted)).isTrue();

        system.reviewSeasonEnd(); // a season passes without another promotion for them

        assertThat(system.isExempt(promoted)).isFalse();
    }

    @Test
    void anExemptionDoesNotSurviveAMoveOffTheTierItWasEarnedOn() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem system = promotedFromDevelopment(field);
        String promoted = field.get(0).player().id();

        // A qualification pathway moves them elsewhere; the Pro card no longer applies.
        system.grantMembership(promoted, TourTier.DEVELOPMENT, "sponsor exemption");

        assertThat(system.isExempt(promoted)).isFalse();
    }

    @Test
    void exemptionsAreRestoredWithTheSystem() {
        List<ProfessionalGolfer> field = TourFixtures.golfers(40);
        TourSystem system = promotedFromDevelopment(field);
        String promoted = field.get(0).player().id();

        TourSystem restored = TourSystem.restore(system.snapshot());

        assertThat(restored.isExempt(promoted)).isTrue();
    }
}
