package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.weather.TournamentWeather;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** tournament-play: over a played field, the closing-round strategies bend to the scoreboard. */
class ScoreboardTournamentTest {

    private static final long WORLD = 0x5C0AB0A2DL;
    private static final int FIELD = 40;

    private static Tournament playedThroughRound3() {
        Course course = CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
        List<ProfessionalGolfer> field = PopulationGenerator.generate(new SeedCoordinate(WORLD, 2, 1, 0, 0, 0, 0), FIELD);
        TournamentDefinition def = new TournamentDefinition("Event", course, Tier.ELITE, EventPrestige.REGULAR,
                new EntryRequirements(FIELD, true), PrizeStructure.standard(), TournamentFormat.standard(),
                LocalDate.of(2026, 6, 1), WORLD, 1, 7);
        Tournament t = new Tournament(def, TournamentWeather.calm());
        t.openRegistration();
        field.forEach(t::register);
        t.confirmField();
        t.advance(); // round 1
        t.advance(); // round 2
        t.advance(); // cut
        t.advance(); // round 3
        return t;
    }

    private static long countAggressive(Tournament t, int roundNo) {
        long n = 0;
        for (int i = 0; i < FIELD; i++) {
            if (t.roundStrategyFor(i, roundNo) == Strategy.AGGRESSIVE) {
                n++;
            }
        }
        return n;
    }

    @Test
    void chasersPressOnTheFinalRound() {
        Tournament t = playedThroughRound3();
        // On the final round the chasers behind the lead press, so more of the field plays aggressively
        // than in an opening round (which uses only innate dispositions).
        long opening = countAggressive(t, 2);   // round 2 < closing round -> dispositions only
        long closing = countAggressive(t, 4);   // round 4 -> dispositions + chasers pressing
        assertThat(closing).isGreaterThan(opening);
    }
}
