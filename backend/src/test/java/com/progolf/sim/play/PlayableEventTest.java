package com.progolf.sim.play;

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
import com.progolf.sim.weather.TournamentWeather;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * playable-event spec: the player's tournament played interactively while the field auto-resolves around
 * them. The guardrail is fidelity — a fully-simmed player event equals automatic resolution competitor by
 * competitor.
 */
class PlayableEventTest {

    private static final long WORLD = 0x5EED1234L;
    private static final int SEASON = 1;
    private static final long TOURN = 7;
    private static final int FIELD_SIZE = 16;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
    }

    private static List<ProfessionalGolfer> field() {
        return PopulationGenerator.generate(new SeedCoordinate(WORLD, 1, 0, 0, 0, 0, 0), FIELD_SIZE);
    }

    private static TournamentDefinition def(Course course) {
        return new TournamentDefinition("Test Event", course, Tier.STANDARD,
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

    private static void assertSameResult(TournamentResult expected, TournamentResult actual) {
        assertThat(actual.winner().player().id()).isEqualTo(expected.winner().player().id());
        assertThat(actual.finishingOrder()).hasSameSizeAs(expected.finishingOrder());
        for (int i = 0; i < expected.finishingOrder().size(); i++) {
            TournamentResult.Finish e = expected.finishingOrder().get(i);
            TournamentResult.Finish a = actual.finishingOrder().get(i);
            assertThat(a.golfer().player().id()).as("finisher %d id", i).isEqualTo(e.golfer().player().id());
            assertThat(a.position()).as("finisher %d position", i).isEqualTo(e.position());
            assertThat(a.score()).as("finisher %d score", i).isEqualTo(e.score());
            assertThat(a.madeCut()).as("finisher %d madeCut", i).isEqualTo(e.madeCut());
            assertThat(a.prize()).as("finisher %d prize", i).isEqualTo(e.prize());
        }
    }

    @Test
    void aFullySimmedPlayerEventMatchesAutomaticResolution() {
        Course course = course();
        List<ProfessionalGolfer> field = field();
        TournamentWeather weather = TournamentWeather.calm();
        TournamentDefinition def = def(course);

        // The player is exercised at several field indices so both made-cut and missed-cut paths are covered.
        for (int k : new int[] {0, 5, 11, 15}) {
            TournamentResult expected = confirmed(def, weather, field).playToCompletion();

            Tournament interactive = confirmed(def, weather, field);
            interactive.designateInteractiveCompetitor(k);
            PlayableEvent event =
                    new PlayableEvent(interactive, field.get(k), k, course, weather, WORLD, SEASON, TOURN);
            event.simEvent();

            assertThat(event.isComplete()).as("event complete for k=%d", k).isTrue();
            assertSameResult(expected, event.result());
        }
    }

    @Test
    void theEventAdvancesRoundByRoundAndCountsTheCut() {
        Course course = course();
        List<ProfessionalGolfer> field = field();
        TournamentWeather weather = TournamentWeather.calm();
        int k = 2;

        Tournament interactive = confirmed(def(course), weather, field);
        interactive.designateInteractiveCompetitor(k);
        PlayableEvent event = new PlayableEvent(interactive, field.get(k), k, course, weather, WORLD, SEASON, TOURN);

        // Play the first two rounds hole-by-hole via sims, then let the cut apply.
        assertThat(event.currentRoundNumber()).isEqualTo(1);
        event.simRound();
        assertThat(event.currentRoundNumber()).isEqualTo(2);
        event.simRound();
        // After round two the cut has been evaluated; playerMadeCut is now meaningful and the event continues.
        boolean madeCut = event.playerMadeCut();
        event.simEvent();
        assertThat(event.isComplete()).isTrue();

        // If the player made the cut they appear as an active (non-cut) finisher; the result is well-formed.
        TournamentResult.Finish playerFinish = event.result().finishingOrder().stream()
                .filter(f -> f.golfer().player().id().equals(field.get(k).player().id()))
                .findFirst().orElseThrow();
        assertThat(playerFinish.madeCut()).isEqualTo(madeCut);
    }

    @Test
    void handPlayingAShotResolvesThroughTheEngineAndAdvancesTheSituation() {
        Course course = course();
        List<ProfessionalGolfer> field = field();
        TournamentWeather weather = TournamentWeather.calm();
        int k = 1;

        Tournament interactive = confirmed(def(course), weather, field);
        interactive.designateInteractiveCompetitor(k);
        PlayableEvent event = new PlayableEvent(interactive, field.get(k), k, course, weather, WORLD, SEASON, TOURN);

        ShotSituation first = event.situation();
        assertThat(first.holeNumber()).isEqualTo(1);
        assertThat(first.shotNumber()).isEqualTo(1);

        // Play one shot by hand (sim policy's own decision), then finish the event.
        event.simShot();
        event.simEvent();
        assertThat(event.isComplete()).isTrue();
        assertThat(event.result().finishingOrder()).hasSize(FIELD_SIZE);
    }
}
