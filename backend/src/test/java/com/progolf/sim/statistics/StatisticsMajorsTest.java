package com.progolf.sim.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** records-archive (modified): championships record their prestige; most-major-championships is a record. */
class StatisticsMajorsTest {

    private static final String MAJOR = "MAJOR";
    private static final String REGULAR = "REGULAR";

    private static EventOutcome win(int season, String id) {
        return new EventOutcome(season, id, 1, -12, true, false, 1_000_000);
    }

    @Test
    void aMajorChampionshipIsRecordedWithItsPrestige() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(win(1, "alice"), "Major Championship", "ELITE", MAJOR, true);
        archive.observeEvent(win(1, "bob"), "Elite Event", "ELITE", REGULAR, true);

        assertThat(archive.majorsWonBy("alice")).hasSize(1);
        assertThat(archive.majorsWonBy("alice").get(0).isMajor()).isTrue();
        assertThat(archive.majorsWonBy("bob")).isEmpty(); // a regular win is not a major
        assertThat(archive.championshipsOf("bob")).hasSize(1); // ...but it is still a championship
    }

    @Test
    void mostMajorsIsARecordThatPreservesThePreviousHolder() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(win(1, "alice"), "Major A", "ELITE", MAJOR, true);
        assertThat(archive.records().get(RecordType.MOST_MAJOR_WINS).golferId()).isEqualTo("alice");

        // Bob wins two majors and takes the record; Alice remains preserved in the progression.
        archive.observeEvent(win(2, "bob"), "Major B", "ELITE", MAJOR, true);
        archive.observeEvent(win(3, "bob"), "Major C", "ELITE", MAJOR, true);

        assertThat(archive.records().get(RecordType.MOST_MAJOR_WINS).golferId()).isEqualTo("bob");
        assertThat(archive.records().get(RecordType.MOST_MAJOR_WINS).value()).isEqualTo(2.0);
        assertThat(archive.recordProgression(RecordType.MOST_MAJOR_WINS))
                .extracting(RecordHolder::golferId).containsExactly("alice", "bob");
    }
}
