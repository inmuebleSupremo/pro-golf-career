package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.course.Course;
import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Completion spec: a tournament is reproducible from its seed and inputs. */
class TournamentReproducibilityTest {

    private static List<String> finishKeys(TournamentResult r) {
        return r.finishingOrder().stream()
                .map(f -> f.golfer().player().id() + ":" + f.position() + ":" + f.score() + ":" + f.prize())
                .collect(Collectors.toList());
    }

    @Test
    void sameSeedCourseAndFieldProduceIdenticalResults() {
        Course course = TournamentFixtures.course();
        List<ProfessionalGolfer> field = TournamentFixtures.field(40);
        TournamentDefinition def = TournamentFixtures.definition(course, 41, TournamentFormat.standard(), EntryRequirements.standard());

        TournamentResult a = TournamentFixtures.openAndRegister(def, field).playToCompletion();
        TournamentResult b = TournamentFixtures.openAndRegister(def, field).playToCompletion();

        assertThat(a.winner().player().id()).isEqualTo(b.winner().player().id());
        assertThat(a.cutResult()).isEqualTo(b.cutResult());
        assertThat(finishKeys(a)).isEqualTo(finishKeys(b));
    }
}
