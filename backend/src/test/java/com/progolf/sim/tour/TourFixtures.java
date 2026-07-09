package com.progolf.sim.tour;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.tournament.CutResult;
import com.progolf.sim.tournament.TournamentResult;
import java.util.ArrayList;
import java.util.List;

/** Shared fixtures for tour tests. */
final class TourFixtures {

    static final long WORLD = 0x70A12L;

    private TourFixtures() {
    }

    static List<ProfessionalGolfer> golfers(int n) {
        return PopulationGenerator.generate(new SeedCoordinate(WORLD, 5, 1, 0, 0, 0, 0), n);
    }

    /** A result with the golfers in the given finishing order (index 0 first). */
    static TournamentResult resultInOrder(String name, List<ProfessionalGolfer> order) {
        List<TournamentResult.Finish> finishes = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            finishes.add(new TournamentResult.Finish(order.get(i), i + 1, -(order.size() - i), true, false, 0.0));
        }
        return new TournamentResult(name, finishes, order.get(0), new CutResult(false, 0, order.size()));
    }

    /** A minimal result in which {@code winner} finishes first. */
    static TournamentResult winFor(ProfessionalGolfer winner) {
        List<TournamentResult.Finish> finishes = List.of(
                new TournamentResult.Finish(winner, 1, -5, true, false, 0.0));
        return new TournamentResult("Win", finishes, winner, new CutResult(false, 0, 1));
    }
}
