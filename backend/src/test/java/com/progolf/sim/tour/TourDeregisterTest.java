package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-membership (modified): deregistration removes membership, excludes from review, keeps history. */
class TourDeregisterTest {

    @Test
    void deregisteredGolferLeavesMembershipAndStandingsButKeepsHistory() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> field = TourFixtures.golfers(12);
        field.forEach(g -> system.register(g.player().id(), TourTier.SECONDARY));
        system.recordResult(TourFixtures.resultInOrder("Q", field), TourTier.SECONDARY);

        // Give the top golfer a recorded movement first (qualification), then deregister them.
        String leaver = field.get(0).player().id();
        system.grantMembership(leaver, TourTier.PRIMARY, "promo");
        system.deregister(leaver);

        assertThat(system.membershipOf(leaver)).isEmpty();
        assertThat(system.standings(TourTier.PRIMARY)).doesNotContain(leaver);
        assertThat(system.standings(TourTier.SECONDARY)).doesNotContain(leaver);

        // A review does not move the departed golfer.
        SeasonReviewResult review = system.reviewSeasonEnd();
        assertThat(review.movements()).noneSatisfy(m -> assertThat(m.golferId()).isEqualTo(leaver));

        // History of prior movements is preserved.
        assertThat(system.movementHistory()).anySatisfy(m -> assertThat(m.golferId()).isEqualTo(leaver));
    }
}
