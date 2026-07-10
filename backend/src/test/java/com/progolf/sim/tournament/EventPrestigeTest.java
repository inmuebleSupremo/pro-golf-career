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

/** event-prestige spec: prestige weights rewards (purse) but never changes play. */
class EventPrestigeTest {

    private static final long WORLD = 0x9E571AEDL;

    private static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
    }

    private static TournamentDefinition def(Course course, EventPrestige prestige) {
        return new TournamentDefinition("Event", course, Tier.ELITE, prestige,
                new EntryRequirements(16, true), PrizeStructure.standard(prestige), TournamentFormat.standard(),
                LocalDate.of(2026, 6, 1), WORLD, 1, 7);
    }

    private static Tournament confirmed(TournamentDefinition def, List<ProfessionalGolfer> field) {
        Tournament t = new Tournament(def, TournamentWeather.calm());
        t.openRegistration();
        field.forEach(t::register);
        t.confirmField();
        return t;
    }

    @Test
    void prestigeMultipliersAreMonotone() {
        assertThat(EventPrestige.MAJOR.rankingWeight()).isGreaterThan(EventPrestige.SIGNATURE.rankingWeight());
        assertThat(EventPrestige.SIGNATURE.rankingWeight()).isGreaterThan(EventPrestige.REGULAR.rankingWeight());
        assertThat(EventPrestige.MAJOR.purseWeight()).isGreaterThan(EventPrestige.SIGNATURE.purseWeight());
        assertThat(EventPrestige.SIGNATURE.purseWeight()).isGreaterThan(EventPrestige.REGULAR.purseWeight());
        assertThat(EventPrestige.REGULAR.rankingWeight()).isEqualTo(1.0);
        assertThat(EventPrestige.MAJOR.isMajor()).isTrue();
        assertThat(EventPrestige.REGULAR.isMajor()).isFalse();
    }

    @Test
    void aBiggerPursePaysMore() {
        assertThat(PrizeStructure.standard(EventPrestige.MAJOR).amountForPosition(1))
                .isGreaterThan(PrizeStructure.standard(EventPrestige.SIGNATURE).amountForPosition(1))
                .isGreaterThan(PrizeStructure.standard(EventPrestige.REGULAR).amountForPosition(1));
        assertThat(PrizeStructure.standard(EventPrestige.REGULAR).amountForPosition(1))
                .isEqualTo(PrizeStructure.standard().amountForPosition(1)); // regular == the plain standard curve
    }

    @Test
    void prestigeDoesNotChangePlayOnlyRewards() {
        Course course = course();
        List<ProfessionalGolfer> field = PopulationGenerator.generate(
                new SeedCoordinate(WORLD, 1, 0, 0, 0, 0, 0), 16);

        TournamentResult regular = confirmed(def(course, EventPrestige.REGULAR), field).playToCompletion();
        TournamentResult major = confirmed(def(course, EventPrestige.MAJOR), field).playToCompletion();

        assertThat(major.winner().player().id()).isEqualTo(regular.winner().player().id());
        assertThat(major.finishingOrder()).hasSameSizeAs(regular.finishingOrder());
        for (int i = 0; i < regular.finishingOrder().size(); i++) {
            TournamentResult.Finish r = regular.finishingOrder().get(i);
            TournamentResult.Finish m = major.finishingOrder().get(i);
            assertThat(m.golfer().player().id()).isEqualTo(r.golfer().player().id());
            assertThat(m.position()).isEqualTo(r.position());
            assertThat(m.score()).isEqualTo(r.score()); // identical play
            if (r.prize() > 0) {
                assertThat(m.prize()).isGreaterThan(r.prize()); // the major pays more
            }
        }
    }
}
