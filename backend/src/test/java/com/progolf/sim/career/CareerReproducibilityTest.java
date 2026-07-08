package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Career-lifecycle spec: replaying the same ordered inputs yields an identical career record. */
class CareerReproducibilityTest {

    private record Event(String name, int position, boolean madeCut, boolean withdrawn, double prize, int daysOffset) {
    }

    private static final List<Event> SCRIPT = List.of(
            new Event("A", 1, true, false, 1000, 0),
            new Event("B", 12, true, false, 50, 7),
            new Event("C", 3, true, false, 400, 14),
            new Event("D", 30, false, true, 0, 21),
            new Event("E", 2, true, false, 600, 28));

    private static Career playScript(int subjectIndex) {
        ProfessionalGolfer subject = CareerFixtures.golfer(subjectIndex);
        ProfessionalGolfer filler = CareerFixtures.golfer(subjectIndex + 1000);
        Career c = new Career(subject.player(), 21);
        LocalDate base = LocalDate.of(2001, 1, 1);
        for (Event e : SCRIPT) {
            c.recordTournament(
                    CareerFixtures.result(e.name(), subject, filler, e.position(), e.madeCut(), e.withdrawn(), e.prize()),
                    base.plusDays(e.daysOffset()));
        }
        c.advanceSeason(base.plusDays(365));
        return c;
    }

    private static List<String> historyKeys(Career c) {
        return c.history().stream()
                .map(e -> e.date() + "|" + e.type() + "|" + e.description())
                .collect(Collectors.toList());
    }

    @Test
    void replayingTheSameScriptProducesAnIdenticalRecord() {
        Career a = playScript(1);
        Career b = playScript(2); // different golfer, identical positions/prizes -> identical record

        assertThat(a.age()).isEqualTo(b.age());
        assertThat(a.statistics().eventsPlayed()).isEqualTo(b.statistics().eventsPlayed());
        assertThat(a.statistics().wins()).isEqualTo(b.statistics().wins());
        assertThat(a.statistics().topTens()).isEqualTo(b.statistics().topTens());
        assertThat(a.statistics().totalEarnings()).isEqualTo(b.statistics().totalEarnings());
        assertThat(a.statistics().averageFinish()).isEqualTo(b.statistics().averageFinish());
        assertThat(historyKeys(a)).isEqualTo(historyKeys(b));
    }
}
