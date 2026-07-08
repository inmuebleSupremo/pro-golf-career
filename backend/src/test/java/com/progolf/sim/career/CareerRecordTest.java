package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.player.ProfessionalGolfer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Career-record spec: statistics folding, first-occurrence milestones, chronological append-only history. */
class CareerRecordTest {

    private static final LocalDate D = LocalDate.of(2001, 3, 1);

    @Test
    void statisticsFoldFromResultsWithCorrectAverageFinish() {
        ProfessionalGolfer subject = CareerFixtures.golfer(1);
        ProfessionalGolfer filler = CareerFixtures.golfer(99);
        Career c = new Career(subject.player(), 20);

        c.recordTournament(CareerFixtures.result("T1", subject, filler, 1, true, false, 1000), D);
        c.recordTournament(CareerFixtures.result("T2", subject, filler, 5, true, false, 100), D.plusDays(7));
        c.recordTournament(CareerFixtures.result("T3", subject, filler, 40, true, false, 10), D.plusDays(14));
        c.recordTournament(CareerFixtures.result("T4", subject, filler, 2, true, false, 500), D.plusDays(21));
        c.recordTournament(CareerFixtures.result("T5", subject, filler, 30, false, true, 0), D.plusDays(28)); // withdrawn

        CareerStatistics s = c.statistics();
        assertThat(s.eventsPlayed()).isEqualTo(5);
        assertThat(s.wins()).isEqualTo(1);
        assertThat(s.runnerUps()).isEqualTo(1);
        assertThat(s.cutsMade()).isEqualTo(4);
        assertThat(s.topTens()).isEqualTo(3); // 1st, 5th, 2nd
        assertThat(s.totalEarnings()).isEqualTo(1610.0);
        assertThat(s.averageFinish()).isEqualTo((1 + 5 + 40 + 2) / 4.0); // withdrawal excluded
    }

    @Test
    void milestonesFireOnceOnFirstOccurrence() {
        ProfessionalGolfer subject = CareerFixtures.golfer(2);
        ProfessionalGolfer filler = CareerFixtures.golfer(98);
        Career c = new Career(subject.player(), 20);

        c.recordTournament(CareerFixtures.result("W1", subject, filler, 1, true, false, 1000), D);
        assertThat(c.hasMilestone(CareerMilestone.FIRST_EVENT)).isTrue();
        assertThat(c.hasMilestone(CareerMilestone.FIRST_MADE_CUT)).isTrue();
        assertThat(c.hasMilestone(CareerMilestone.FIRST_TOP_10)).isTrue();
        assertThat(c.hasMilestone(CareerMilestone.FIRST_WIN)).isTrue();

        c.recordTournament(CareerFixtures.result("W2", subject, filler, 1, true, false, 1000), D.plusDays(7));
        long firstWinEntries = c.history().stream()
                .filter(e -> e.type() == CareerHistoryEntry.Type.MILESTONE && e.description().equals("FIRST_WIN"))
                .count();
        assertThat(firstWinEntries).isEqualTo(1); // no duplicate milestone
    }

    @Test
    void historyIsChronologicalAndImmutable() {
        ProfessionalGolfer subject = CareerFixtures.golfer(3);
        ProfessionalGolfer filler = CareerFixtures.golfer(97);
        Career c = new Career(subject.player(), 20);

        // Record out of date order; history() must present chronologically.
        c.recordTournament(CareerFixtures.result("Late", subject, filler, 10, true, false, 0), D.plusDays(30));
        c.recordTournament(CareerFixtures.result("Early", subject, filler, 20, true, false, 0), D);

        var history = c.history();
        for (int i = 1; i < history.size(); i++) {
            assertThat(history.get(i).date()).isAfterOrEqualTo(history.get(i - 1).date());
        }
        // The returned view is immutable.
        assertThatThrownBy(() -> history.add(new CareerHistoryEntry(D, CareerHistoryEntry.Type.MILESTONE, "x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
