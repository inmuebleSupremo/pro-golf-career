package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-movement spec: a consistently strong golfer rises from Development to the Pro tour and holds it. */
class TourMigrationTest {

    @Test
    void aConsistentWinnerRisesFromDevelopmentToProAndStaysThere() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> field = TourFixtures.golfers(30);
        field.forEach(g -> system.register(g.player().id(), TourTier.DEVELOPMENT));
        ProfessionalGolfer strong = field.get(0);
        String strongId = strong.player().id();

        // Each season the strong golfer wins their tour and tops the standings.
        for (int s = 0; s < 3; s++) {
            TourTier tier = system.membershipOf(strongId).orElseThrow();
            system.recordResult(TourFixtures.winFor(strong), tier);
            system.reviewSeasonEnd();
        }

        // They earn a Pro card and, winning there too, keep it — the top tour is the ceiling.
        assertThat(system.membershipOf(strongId)).contains(TourTier.PRO);
        long promotions = system.movementHistory().stream()
                .filter(m -> m.golferId().equals(strongId) && m.type() == MovementType.PROMOTION)
                .count();
        assertThat(promotions).isEqualTo(1); // one step: Development -> Pro
    }
}
