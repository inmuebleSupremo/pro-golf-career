package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** world-schedule (structured cadence): the standard 30-week tier-specific calendar. */
class SeasonCadenceTest {

    private List<SeasonCadence.Placement> tier(List<SeasonCadence.Placement> all, TourTier t) {
        return all.stream().filter(p -> p.tier() == t).toList();
    }

    private List<Integer> weeksOf(List<SeasonCadence.Placement> placements, EventPrestige prestige) {
        return placements.stream().filter(p -> p.prestige() == prestige).map(SeasonCadence.Placement::week).sorted()
                .toList();
    }

    @Test
    void majorsAnchorFixedChapterWeeksOnElite() {
        List<SeasonCadence.Placement> elite = tier(SeasonCadence.forSeason(30), TourTier.ELITE);
        assertThat(weeksOf(elite, EventPrestige.MAJOR)).containsExactly(7, 14, 21, 27);
    }

    @Test
    void eachTourEndsWithOneChampionshipAndDevelopmentFinishesFirst() {
        List<SeasonCadence.Placement> all = SeasonCadence.forSeason(30);
        for (TourTier t : TourTier.values()) {
            assertThat(weeksOf(tier(all, t), EventPrestige.TOUR_CHAMPIONSHIP)).as("championships for %s", t).hasSize(1);
        }
        int elite = weeksOf(tier(all, TourTier.ELITE), EventPrestige.TOUR_CHAMPIONSHIP).get(0);
        int dev = weeksOf(tier(all, TourTier.DEVELOPMENT), EventPrestige.TOUR_CHAMPIONSHIP).get(0);
        assertThat(elite).isEqualTo(30);
        assertThat(dev).isEqualTo(29);
        assertThat(dev).isLessThan(elite);
    }

    @Test
    void signaturesAreSpotlightedNotClusteredAtTheStart() {
        List<Integer> eliteSignatures = weeksOf(tier(SeasonCadence.forSeason(30), TourTier.ELITE),
                EventPrestige.SIGNATURE);
        assertThat(eliteSignatures).isNotEmpty();
        // At least one signature falls in the back half of the season (not all bunched at the opening).
        assertThat(eliteSignatures).anyMatch(w -> w > 15);
    }

    @Test
    void thereIsAMidSeasonEliteDevelopmentCollisionWeek() {
        List<SeasonCadence.Placement> all = SeasonCadence.forSeason(30);
        List<Integer> eliteSigs = weeksOf(tier(all, TourTier.ELITE), EventPrestige.SIGNATURE);
        List<Integer> devSigs = weeksOf(tier(all, TourTier.DEVELOPMENT), EventPrestige.SIGNATURE);
        List<Integer> collision = eliteSigs.stream().filter(devSigs::contains).toList();
        assertThat(collision).as("a week where both Elite and Development host a signature").isNotEmpty();
    }

    @Test
    void developmentSignaturesAvoidTheEliteMajorWeeks() {
        List<Integer> devSigs = weeksOf(tier(SeasonCadence.forSeason(30), TourTier.DEVELOPMENT),
                EventPrestige.SIGNATURE);
        assertThat(devSigs).doesNotContain(7, 14, 21, 27);
    }

    @Test
    void eachTierHitsTheTargetDensity() {
        List<SeasonCadence.Placement> all = SeasonCadence.forSeason(30);
        for (TourTier t : TourTier.values()) {
            assertThat(tier(all, t)).as("events for %s", t).hasSize(SeasonCadence.TARGET_EVENTS_PER_TIER);
        }
    }

    @Test
    void cadenceIsDeterministic() {
        assertThat(SeasonCadence.forSeason(30)).isEqualTo(SeasonCadence.forSeason(30));
    }

    @Test
    void structuredCadenceAppliesOnlyToTheStandardProfile() {
        assertThat(SeasonCadence.appliesTo(WorldConfig.defaults())).isTrue();
        assertThat(SeasonCadence.appliesTo(new WorldConfig(40, 6, 3, 20, 4))).isFalse(); // small test config
    }

    @Test
    void aTierNeverPlaysTwiceInOneWeek() {
        List<SeasonCadence.Placement> all = SeasonCadence.forSeason(30);
        for (TourTier t : TourTier.values()) {
            List<Integer> weeks = tier(all, t).stream().map(SeasonCadence.Placement::week).collect(Collectors.toList());
            assertThat(weeks).as("distinct weeks for %s", t).doesNotHaveDuplicates();
        }
    }
}
