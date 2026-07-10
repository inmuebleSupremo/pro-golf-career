package com.progolf.sim.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.Tier;
import org.junit.jupiter.api.Test;

/** world-ranking (modified): ranking points scale with event prestige — a major is worth the most. */
class RankingPrestigeTest {

    @Test
    void higherPrestigeAwardsMorePointsForTheSameFinish() {
        double regular = RankingPoints.award(Tier.ELITE, EventPrestige.REGULAR, 1, 1.0);
        double signature = RankingPoints.award(Tier.ELITE, EventPrestige.SIGNATURE, 1, 1.0);
        double major = RankingPoints.award(Tier.ELITE, EventPrestige.MAJOR, 1, 1.0);

        assertThat(major).isGreaterThan(signature);
        assertThat(signature).isGreaterThan(regular);
    }

    @Test
    void regularPrestigeMatchesTheUnweightedAward() {
        assertThat(RankingPoints.award(Tier.PREMIER, EventPrestige.REGULAR, 3, 1.2))
                .isEqualTo(RankingPoints.award(Tier.PREMIER, 3, 1.2));
    }

    @Test
    void aMajorOutweighsAHigherFinishInARegularEvent() {
        // Winning a major is worth more than winning a regular event of the same tier — the pinnacle.
        double majorWin = RankingPoints.award(Tier.ELITE, EventPrestige.MAJOR, 1, 1.0);
        double regularWin = RankingPoints.award(Tier.ELITE, EventPrestige.REGULAR, 1, 1.0);
        assertThat(majorWin).isEqualTo(regularWin * EventPrestige.MAJOR.rankingWeight());
    }
}
