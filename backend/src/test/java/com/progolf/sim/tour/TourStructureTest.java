package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import org.junit.jupiter.api.Test;

/** Tour-structure spec: tier ladder with a complete pathway, tours per tier, membership eligibility. */
class TourStructureTest {

    @Test
    void tierLadderIsOrderedWithNeighboursAndACompletePathway() {
        assertThat(TourTier.DEVELOPMENT.rank()).isEqualTo(0);
        assertThat(TourTier.PRO.rank()).isEqualTo(TourTier.values().length - 1);
        assertThat(TourTier.DEVELOPMENT.below()).isEmpty();
        assertThat(TourTier.DEVELOPMENT.above()).contains(TourTier.PRO);
        assertThat(TourTier.PRO.above()).isEmpty();
        assertThat(TourTier.PRO.below()).contains(TourTier.DEVELOPMENT);

        // Continuous path from lowest to highest.
        TourTier t = TourTier.DEVELOPMENT;
        int steps = 0;
        while (t.above().isPresent()) {
            t = t.above().get();
            steps++;
        }
        assertThat(t).isEqualTo(TourTier.PRO);
        assertThat(steps).isEqualTo(TourTier.values().length - 1);
    }

    @Test
    void oneTourPerTierWithDistinctIdentity() {
        TourSystem system = new TourSystem();
        for (TourTier tier : TourTier.values()) {
            assertThat(system.tour(tier).tier()).isEqualTo(tier);
        }
        assertThat(system.tour(TourTier.PRO).id()).isNotEqualTo(system.tour(TourTier.DEVELOPMENT).id());
    }

    @Test
    void eligibilityDerivesFromMembershipWithInvitationException() {
        TourSystem system = new TourSystem();
        ProfessionalGolfer g = TourFixtures.golfers(1).get(0);
        String id = g.player().id();
        system.register(id, TourTier.DEVELOPMENT);

        assertThat(system.isEligible(id, TourTier.DEVELOPMENT, false)).isTrue();
        assertThat(system.isEligible(id, TourTier.PRO, false)).isFalse();
        assertThat(system.isEligible(id, TourTier.PRO, true)).isTrue(); // invitation exception
    }
}
