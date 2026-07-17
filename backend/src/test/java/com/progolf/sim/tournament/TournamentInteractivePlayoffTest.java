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

    // The player's fixed opening submissions: a runaway lead (rounds 1-3) so the field is identical between
    // the probe and the real run. Situational pressure now couples the field to the leaderboard, so the tie
    // must be computed against the field as it actually plays WITH the player present (a two-pass fixed point).
    //
    // The opening must be far enough clear that the field CANNOT reach it, or the probe itself ends in a
    // playoff and never produces a result to read. It is deliberately well beyond any reachable score rather
    // than just beyond the current calibration's.
    private static final int R1 = -90, R2 = 0, R3 = 0, AFTER3 = R1 + R2 + R3;

    /**
     * Drives the interactive player to tie the field leader for the lead. Because contention-based pressure
     * makes the field's scores depend on the player's leaderboard position, this uses two passes with an
     * IDENTICAL opening (rounds 1-3), so the field plays identically in both: pass one reads the field's best
     * score, pass two submits a final-round score that lands the player exactly on it.
     */
    private static Tournament tiedForLead(Course course, List<ProfessionalGolfer> field, TournamentWeather weather) {
        String playerId = field.get(PLAYER).player().id();

        Tournament probe = playOpening(course, field, weather);
        probe.submitInteractiveRoundScore(4, 0);
        probe.advance(); // round 4
        probe.advance(); // complete
        int fieldBest = probe.result().finishingOrder().stream()
                .filter(f -> !f.golfer().player().id().equals(playerId))
                .mapToInt(TournamentResult.Finish::score)
                .min().orElseThrow();

        Tournament t = playOpening(course, field, weather); // identical rounds 1-3 => identical field
        t.submitInteractiveRoundScore(4, fieldBest - AFTER3); // land the player exactly on the field's best
        t.advance(); // round 4
        t.advance(); // ROUND_4 -> PLAYOFF (player tied for the lead) or COMPLETED
        return t;
    }

    /** Registers the field, designates the player, and plays the identical opening (rounds 1-3 + cut). */
    private static Tournament playOpening(Course course, List<ProfessionalGolfer> field, TournamentWeather weather) {
        Tournament t = confirmed(def(course), weather, field);
        t.designateInteractiveCompetitor(PLAYER);
        t.submitInteractiveRoundScore(1, R1);
        t.advance(); // round 1
        t.submitInteractiveRoundScore(2, R2);
        t.advance(); // round 2
        t.advance(); // cut
        assertThat(t.interactiveCompetitorMadeCut()).isTrue();
        t.submitInteractiveRoundScore(3, R3);
        t.advance(); // round 3
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
