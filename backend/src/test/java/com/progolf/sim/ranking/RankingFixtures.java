package com.progolf.sim.ranking;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.tournament.CutResult;
import com.progolf.sim.tournament.TournamentResult;
import java.util.ArrayList;
import java.util.List;

/** Shared fixtures for ranking tests. */
final class RankingFixtures {

    static final long WORLD = 0x5EED_1234L;

    private RankingFixtures() {
    }

    static List<ProfessionalGolfer> golfers(int n) {
        return PopulationGenerator.generate(new SeedCoordinate(WORLD, 3, 1, 0, 0, 0, 0), n);
    }

    /** A tournament result with the given golfers in finishing order (index 0 wins). */
    static TournamentResult result(String name, List<ProfessionalGolfer> order) {
        List<TournamentResult.Finish> finishes = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            finishes.add(new TournamentResult.Finish(order.get(i), i + 1, -(order.size() - i), true, false, 0.0));
        }
        return new TournamentResult(name, finishes, order.get(0), new CutResult(false, 0, order.size()));
    }
}
