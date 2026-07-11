package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.economy.EconomyConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/** financial-strategy (modified): in the lived world, money is scarce and results-dependent. */
class WorldEconomyStakesTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void moneyIsScarceAndResultsDependent() {
        World world = World.create(4242L, small());
        for (int i = 0; i < 6; i++) {
            world.advanceSeason();
        }

        // Competing no longer guarantees profit: at least one golfer has spent more on entry/travel/staff
        // than they have won in prize money (impossible in the old flat-purse world where everyone cashed).
        boolean someoneCompetesAtALoss = world.activeGolferIds().stream()
                .map(world::financialAccountOf)
                .anyMatch(a -> a.careerExpenses() > a.tournamentEarnings());
        assertThat(someoneCompetesAtALoss).as("prize money alone should not sustain every golfer").isTrue();

        // ...while the top of the game is far ahead — a wide, results-driven earnings spread.
        List<Double> funds = world.activeGolferIds().stream()
                .map(id -> world.financialAccountOf(id).availableFunds())
                .toList();
        double max = funds.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        double min = funds.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        assertThat(max).isGreaterThan(EconomyConstants.STARTING_FUNDS);
        assertThat(max - min).isGreaterThan(EconomyConstants.STARTING_FUNDS);
    }

    @Test
    void financialStateStaysReproducible() {
        World a = World.create(4343L, small());
        World b = World.create(4343L, small());
        for (int i = 0; i < 3; i++) {
            a.advanceSeason();
            b.advanceSeason();
        }
        List<String> sa = a.activeGolferIds().stream().sorted()
                .map(id -> id + "|" + a.financialAccountOf(id).availableFunds()).toList();
        List<String> sb = b.activeGolferIds().stream().sorted()
                .map(id -> id + "|" + b.financialAccountOf(id).availableFunds()).toList();
        assertThat(sa).isEqualTo(sb);
    }
}
