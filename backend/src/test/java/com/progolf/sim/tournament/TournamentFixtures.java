package com.progolf.sim.tournament;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import java.time.LocalDate;
import java.util.List;

/** Shared, deterministic fixtures for tournament tests. */
final class TournamentFixtures {

    static final long WORLD = 0xA11CE5F00DL; // fixed nonzero world seed

    private TournamentFixtures() {
    }

    static Course course() {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), EnvironmentClassification.PARKLAND);
    }

    static List<ProfessionalGolfer> field(int n) {
        return PopulationGenerator.generate(new SeedCoordinate(WORLD, 2, 1, 0, 0, 0, 0), n);
    }

    static TournamentDefinition definition(Course course, long tournamentId, TournamentFormat format, EntryRequirements req) {
        return new TournamentDefinition("Test Open", course, Tier.STANDARD, req,
                PrizeStructure.standard(), format, LocalDate.of(2000, 6, 1), WORLD, 1, tournamentId);
    }

    static TournamentDefinition standardDefinition(long tournamentId) {
        return definition(course(), tournamentId, TournamentFormat.standard(), EntryRequirements.standard());
    }

    static Tournament openAndRegister(TournamentDefinition def, List<ProfessionalGolfer> golfers) {
        Tournament t = new Tournament(def);
        t.openRegistration();
        for (ProfessionalGolfer g : golfers) {
            t.register(g);
        }
        t.confirmField();
        return t;
    }
}
