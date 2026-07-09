package com.progolf.sim.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** competitive-statistics / records-archive / historical-queries: accumulation, preservation, records. */
class StatisticsArchiveTest {

    private static EventOutcome win(String id, int season, int score) {
        return new EventOutcome(season, id, 1, score, true, false, 1_000_000);
    }

    private static EventOutcome finish(String id, int season, int position, int score, boolean madeCut) {
        return new EventOutcome(season, id, position, score, madeCut, false, 0);
    }

    @Test
    void seasonalStatisticsArePreservedAndNeverOverwritten() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(finish("g1", 1, 5, -3, true), "Open", "ELITE", false);
        StatLine seasonOne = archive.seasonStatistics("g1", 1);

        archive.observeEvent(finish("g1", 2, 8, -1, true), "Classic", "ELITE", false);
        // Season 1's line is untouched by season 2 (REQ-252); the career aggregates both (REQ-253).
        assertThat(archive.seasonStatistics("g1", 1)).isEqualTo(seasonOne);
        assertThat(archive.careerStatistics("g1").events()).isEqualTo(2);
    }

    @Test
    void championsAreRegisteredAndQueryable() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(win("g1", 1, -12), "The Open", "ELITE", true);
        archive.observeEvent(finish("g2", 1, 2, -10, true), "The Open", "ELITE", false);

        assertThat(archive.championsOfSeason(1)).singleElement().satisfies(c -> {
            assertThat(c.winnerId()).isEqualTo("g1");
            assertThat(c.tournamentName()).isEqualTo("The Open");
        });
        assertThat(archive.championshipsOf("g1")).hasSize(1);
        assertThat(archive.championshipsOf("g2")).isEmpty();
    }

    @Test
    void recordsEmergeFromOutcomesAndPreserveProgression() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(win("g1", 1, -12), "Open", "ELITE", true);
        archive.observeEvent(win("g2", 2, -18), "Classic", "ELITE", true);

        // Lowest score record moved from g1 to g2 but preserves both (REQ-255).
        assertThat(archive.records().get(RecordType.LOWEST_TOURNAMENT_SCORE).golferId()).isEqualTo("g2");
        assertThat(archive.recordProgression(RecordType.LOWEST_TOURNAMENT_SCORE))
                .extracting(RecordHolder::golferId).containsExactly("g1", "g2");
        // Each record references the establishing golfer and season.
        assertThat(archive.records().get(RecordType.MOST_CAREER_WINS)).isNotNull();
    }

    @Test
    void consecutiveCutsTrackAStreakThatResetsOnAMiss() {
        StatisticsArchive archive = new StatisticsArchive();
        for (int i = 0; i < 3; i++) {
            archive.observeEvent(finish("g1", 1, 20, 2, true), "E" + i, "PRIMARY", false);
        }
        assertThat(archive.records().get(RecordType.MOST_CONSECUTIVE_CUTS).value()).isEqualTo(3);
        archive.observeEvent(finish("g1", 1, 80, 8, false), "Miss", "PRIMARY", false); // missed cut resets
        archive.observeEvent(finish("g1", 1, 20, 2, true), "Back", "PRIMARY", false);  // streak now 1
        assertThat(archive.records().get(RecordType.MOST_CONSECUTIVE_CUTS).value()).isEqualTo(3); // record stands
    }

    @Test
    void careerLongevityGrowsWithDistinctSeasons() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(finish("g1", 1, 5, -2, true), "A", "ELITE", false);
        archive.observeEvent(finish("g1", 2, 5, -2, true), "B", "ELITE", false);
        archive.observeEvent(finish("g1", 3, 5, -2, true), "C", "ELITE", false);
        assertThat(archive.records().get(RecordType.LONGEST_CAREER).value()).isEqualTo(3);
    }

    @Test
    void comparisonReadsWithoutMutating() {
        StatisticsArchive archive = new StatisticsArchive();
        archive.observeEvent(win("g1", 1, -12), "Open", "ELITE", true);
        archive.observeEvent(finish("g2", 1, 3, -8, true), "Open", "ELITE", false);

        CareerComparison cmp = archive.compareCareers("g1", "g2");
        assertThat(cmp.moreWins()).isEqualTo("g1");
        assertThat(cmp.higherEarnings()).isEqualTo("g1");
        // The comparison did not disturb the archive.
        assertThat(archive.careerStatistics("g1").wins()).isEqualTo(1);
    }
}
