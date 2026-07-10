package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.weather.TournamentWeather;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * tournament-play / playable-event spec: the interactive competitor's per-round score is supplied
 * externally, and a tie for the lead can be driven through an interactive sudden-death playoff. A tie is
 * forced deterministically by submitting the interactive player's round scores to match the field leader.
 */
class TournamentInteractivePlayoffTest {

    private static final long WORLD = 0x9A9A9AL;
    private static final int SEASON = 1;
    private static final long TOURN = 3;
    private static final int FIELD_SIZE = 16;
    private static final int PLAYER = 6;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
    }

    private static List<ProfessionalGolfer> field() {
        return PopulationGenerator.generate(new SeedCoordinate(WORLD, 1, 0, 0, 0, 0, 0), FIELD_SIZE);
    }

    private static TournamentDefinition def(Course course) {
        return new TournamentDefinition("Playoff Event", course, Tier.STANDARD,
                new EntryRequirements(FIELD_SIZE, true), PrizeStructure.standard(), TournamentFormat.standard(),
                LocalDate.of(2026, 6, 1), WORLD, SEASON, TOURN);
    }

    private static Tournament confirmed(TournamentDefinition def, TournamentWeather weather,
                                        List<ProfessionalGolfer> field) {
        Tournament t = new Tournament(def, weather);
        t.openRegistration();
        field.forEach(t::register);
        t.confirmField();
        return t;
    }

    /** Drives the interactive player through four rounds with scores that tie the field leader for the lead. */
    private static Tournament tiedForLead(Course course, List<ProfessionalGolfer> field, TournamentWeather weather) {
        int leaderTotal = confirmed(def(course), weather, field).playToCompletion()
                .finishingOrder().get(0).score(); // the winning (lowest) cumulative

        Tournament t = confirmed(def(course), weather, field);
        t.designateInteractiveCompetitor(PLAYER);
        // Front-load the whole total into round 1 so the player is comfortably inside the cut, then tie the
        // leader after 72 holes.
        t.submitInteractiveRoundScore(1, leaderTotal);
        t.advance(); // round 1
        t.submitInteractiveRoundScore(2, 0);
        t.advance(); // round 2
        t.advance(); // cut
        assertThat(t.interactiveCompetitorMadeCut()).isTrue();
        t.submitInteractiveRoundScore(3, 0);
        t.advance(); // round 3
        t.submitInteractiveRoundScore(4, 0);
        t.advance(); // round 4
        t.advance(); // ROUND_4 -> PLAYOFF (player tied with the leader) or COMPLETED
        return t;
    }

    @Test
    void aTiedPlayerEntersAndCompletesAnInteractivePlayoff() {
        Course course = course();
        List<ProfessionalGolfer> field = field();
        TournamentWeather weather = TournamentWeather.calm();

        Tournament t = tiedForLead(course, field, weather);
        assertThat(t.isPlayoff()).as("the player should tie for the lead").isTrue();

        t.beginInteractivePlayoff();
        assertThat(t.playoffContenders()).isNotEmpty();
        assertThat(t.playoffHoleNumber()).isEqualTo(1);
        assertThat(t.playoffRound()).isEqualTo(91L);

        int guard = 0;
        while (t.isPlayoff() && guard++ < 200) {
            t.advancePlayoffHole(null); // auto-resolve every contender (including the player)
        }
        assertThat(t.state()).isEqualTo(TournamentState.COMPLETED);
        assertThat(t.result().winner()).isNotNull();
    }

    @Test
    void theInteractivePlayersSuppliedStrokesDecideThePlayoff() {
        Course course = course();
        List<ProfessionalGolfer> field = field();
        TournamentWeather weather = TournamentWeather.calm();

        Tournament t = tiedForLead(course, field, weather);
        assertThat(t.isPlayoff()).isTrue();

        t.beginInteractivePlayoff();
        // A single stroke on the playoff hole cannot be matched by any rival: the player wins outright.
        t.advancePlayoffHole(1);

        assertThat(t.state()).isEqualTo(TournamentState.COMPLETED);
        assertThat(t.result().winner().player().id()).isEqualTo(field.get(PLAYER).player().id());
    }
}
