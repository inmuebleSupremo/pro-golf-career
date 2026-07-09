package com.progolf.sim.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.tournament.EntryRequirements;
import com.progolf.sim.tournament.PrizeStructure;
import com.progolf.sim.tournament.Tier;
import com.progolf.sim.tournament.Tournament;
import com.progolf.sim.tournament.TournamentDefinition;
import com.progolf.sim.tournament.TournamentFormat;
import com.progolf.sim.tournament.TournamentResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** tournament-play (modified): rounds resolve under the tournament's Playing Conditions; calm is default. */
class TournamentWeatherIntegrationTest {

    private static final long WORLD = 0xC0FFEEL;

    private static TournamentDefinition definition() {
        Course course = CourseGenerator.generate(
                new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
        return new TournamentDefinition("Weather Open", course, Tier.STANDARD, EntryRequirements.standard(),
                PrizeStructure.standard(), TournamentFormat.standard(), LocalDate.of(2000, 6, 1), WORLD, 1, 1);
    }

    private static List<ProfessionalGolfer> field() {
        return PopulationGenerator.generate(new SeedCoordinate(WORLD, 2, 1, 0, 0, 0, 0), 24);
    }

    private static TournamentResult play(Tournament t, List<ProfessionalGolfer> field) {
        t.openRegistration();
        for (ProfessionalGolfer g : field) {
            t.register(g);
        }
        t.confirmField();
        return t.playToCompletion();
    }

    /** A comparable (playerId, score) view of the finishing order. */
    private static List<String> scoreLine(TournamentResult r) {
        return r.finishingOrder().stream()
                .map(f -> f.golfer().player().id() + ":" + f.score())
                .toList();
    }

    @Test
    void noWeatherConstructorReproducesExplicitCalm() {
        TournamentDefinition def = definition();
        TournamentResult defaulted = play(new Tournament(def), field());
        TournamentResult explicitCalm = play(new Tournament(def, TournamentWeather.calm()), field());

        assertThat(defaulted.winner().player().id()).isEqualTo(explicitCalm.winner().player().id());
        assertThat(scoreLine(defaulted)).isEqualTo(scoreLine(explicitCalm));
    }

    @Test
    void weatherChangesOutcomesVersusCalm() {
        TournamentDefinition def = definition();
        PlayingConditions storm = PlayingConditions.of(30.0, 15.0, 0.5, 52.0, 0.7);
        TournamentWeather windy = new TournamentWeather(List.of(storm), new Forecast(storm));

        TournamentResult calm = play(new Tournament(def, TournamentWeather.calm()), field());
        TournamentResult stormy = play(new Tournament(def, windy), field());

        // Same field and seed, different conditions: scoring differs under weather.
        assertThat(scoreLine(stormy)).isNotEqualTo(scoreLine(calm));
    }
}
