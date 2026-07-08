package com.progolf.sim.career;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.tournament.CutResult;
import com.progolf.sim.tournament.TournamentResult;
import java.util.ArrayList;
import java.util.List;

/** Shared fixtures for career tests. */
final class CareerFixtures {

    static final long WORLD = 0xCA1EEDL;

    private CareerFixtures() {
    }

    /** A simulation golfer whose Player is ACTIVE (population golfers are activated on generation). */
    static ProfessionalGolfer golfer(int index) {
        return PopulationGenerator.generateOne(new SeedCoordinate(WORLD, 4, 1, 0, 0, 0, 0), index);
    }

    /**
     * A tournament result placing {@code subject} at {@code position}, with a {@code filler} occupying the
     * complementary spot so there is always a winner. The Career only reads the subject's finish.
     */
    static TournamentResult result(String name, ProfessionalGolfer subject, ProfessionalGolfer filler,
                                   int position, boolean madeCut, boolean withdrawn, double prize) {
        List<TournamentResult.Finish> finishes = new ArrayList<>();
        if (position == 1) {
            finishes.add(new TournamentResult.Finish(subject, 1, -5, true, false, prize));
            finishes.add(new TournamentResult.Finish(filler, 2, -3, true, false, 0.0));
            return new TournamentResult(name, finishes, subject, new CutResult(false, 0, 2));
        }
        finishes.add(new TournamentResult.Finish(filler, 1, -5, true, false, 0.0));
        finishes.add(new TournamentResult.Finish(subject, position, 0, madeCut, withdrawn, prize));
        return new TournamentResult(name, finishes, filler, new CutResult(false, 0, 2));
    }
}
