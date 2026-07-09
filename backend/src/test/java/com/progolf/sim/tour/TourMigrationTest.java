package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-movement spec: a consistently strong golfer migrates upward through the tiers over seasons. */
class TourMigrationTest {

    @Test
    void aConsistentWinnerRisesFromDevelopmentToElite() {
        TourSystem system = new TourSystem();
        List<ProfessionalGolfer> field = TourFixtures.golfers(30);
        field.forEach(g -> system.register(g.player().id(), TourTier.DEVELOPMENT));
        ProfessionalGolfer strong = field.get(0);
        String strongId = strong.player().id();

        // Each season the strong golfer wins their tour, tops the standings, and is promoted.
        for (int s = 0; s < 3; s++) {
            TourTier tier = system.membershipOf(strongId).orElseThrow();
            system.recordResult(TourFixtures.winFor(strong), tier);
            system.reviewSeasonEnd();
        }

        assertThat(system.membershipOf(strongId)).contains(TourTier.ELITE);
        // The rise is recorded as promotions in history.
        long promotions = system.movementHistory().stream()
                .filter(m -> m.golferId().equals(strongId) && m.type() == MovementType.PROMOTION)
                .count();
        assertThat(promotions).isEqualTo(3);
    }
}
