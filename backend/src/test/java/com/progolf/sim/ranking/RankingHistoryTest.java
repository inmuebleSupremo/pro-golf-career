package com.progolf.sim.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ranking-history & recognition specs: career-high, weeks at #1, movement, snapshots, milestones. */
class RankingHistoryTest {

    private static RankingSnapshot snapshot(LocalDate date, String... golferIdsInOrder) {
        List<RankingStanding> standings = new ArrayList<>();
        for (int i = 0; i < golferIdsInOrder.length; i++) {
            standings.add(new RankingStanding(i + 1, golferIdsInOrder[i], 100.0 - i));
        }
        return new RankingSnapshot(date, standings);
    }

    @Test
    void careerHighIsPreservedAfterDecline() {
        List<RankingSnapshot> series = List.of(
                snapshot(LocalDate.of(2001, 1, 1), "b", "a", "c"), // a is #2
                snapshot(LocalDate.of(2001, 6, 1), "a", "b", "c"), // a is #1
                snapshot(LocalDate.of(2001, 12, 1), "b", "c", "a") // a drops to #3
        );
        assertThat(RankingHistory.careerHighPosition("a", series)).contains(1);
    }

    @Test
    void weeksAtNumberOneAccumulateAndNeverDecrease() {
        List<RankingSnapshot> series = List.of(
                snapshot(LocalDate.of(2001, 1, 1), "a", "b"),
                snapshot(LocalDate.of(2001, 2, 1), "a", "b"),
                snapshot(LocalDate.of(2001, 3, 1), "b", "a"));
        assertThat(RankingHistory.weeksAtNumberOne("a", series)).isEqualTo(2);
        assertThat(RankingHistory.weeksAtNumberOne("b", series)).isEqualTo(1);
    }

    @Test
    void movementBetweenSnapshotsReflectsImprovement() {
        RankingSnapshot from = snapshot(LocalDate.of(2001, 1, 1), "x", "y", "z", "a");
        RankingSnapshot to = snapshot(LocalDate.of(2001, 2, 1), "a", "x", "y", "z");
        RankingMovement m = RankingHistory.movementBetween("a", from, to).orElseThrow();
        assertThat(m.improved()).isTrue();
        assertThat(m.placesGained()).isEqualTo(3); // 4th -> 1st
    }

    @Test
    void snapshotIsImmutableAgainstLaterListMutation() {
        List<RankingStanding> live = new ArrayList<>();
        live.add(new RankingStanding(1, "a", 100));
        RankingSnapshot snap = new RankingSnapshot(LocalDate.of(2001, 1, 1), live);
        live.clear(); // mutate the source after construction
        assertThat(snap.size()).isEqualTo(1);
        assertThat(snap.leader().orElseThrow().golferId()).isEqualTo("a");
    }

    @Test
    void milestoneForPositionMatchesThresholds() {
        assertThat(RankingMilestone.forPosition(1)).isEqualTo(RankingMilestone.WORLD_NUMBER_ONE);
        assertThat(RankingMilestone.forPosition(10)).isEqualTo(RankingMilestone.TOP_10);
        assertThat(RankingMilestone.forPosition(11)).isEqualTo(RankingMilestone.TOP_50);
        assertThat(RankingMilestone.forPosition(51)).isEqualTo(RankingMilestone.TOP_100);
        assertThat(RankingMilestone.forPosition(101)).isNull();
    }

    @Test
    void bestMilestoneReachedReflectsCareerHigh() {
        List<RankingSnapshot> series = List.of(
                snapshot(LocalDate.of(2001, 1, 1), "x", "y", "z", "a"), // a #4
                snapshot(LocalDate.of(2001, 6, 1), "x", "y", "a", "z")); // a #3
        assertThat(RankingHistory.bestMilestoneReached("a", series)).contains(RankingMilestone.TOP_10);
    }
}
