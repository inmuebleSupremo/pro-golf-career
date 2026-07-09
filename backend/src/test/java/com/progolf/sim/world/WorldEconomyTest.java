package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.economy.EconomyConstants;
import com.progolf.sim.economy.FinancialAccount;
import java.util.List;
import org.junit.jupiter.api.Test;

/** world-progression (modified): events move finances; the seasonal financial cycle runs; reproducible. */
class WorldEconomyTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void everyGolferHasAFinancialIdentityFromTheStart() {
        World world = World.create(111L, small());
        for (String id : world.activeGolferIds()) {
            FinancialAccount account = world.financialAccountOf(id);
            assertThat(account).isNotNull();
            assertThat(account.availableFunds()).isEqualTo(EconomyConstants.STARTING_FUNDS);
        }
    }

    @Test
    void eventsAwardPrizeAndChargeExpenses() {
        World world = World.create(222L, small());
        world.advanceSeason();

        boolean anyPrize = world.activeGolferIds().stream()
                .map(world::financialAccountOf)
                .anyMatch(a -> a.tournamentEarnings() > 0);
        boolean anyExpense = world.activeGolferIds().stream()
                .map(world::financialAccountOf)
                .anyMatch(a -> a.careerExpenses() > 0);
        assertThat(anyPrize).as("some golfer earned prize money").isTrue();
        assertThat(anyExpense).as("competing incurred expenses").isTrue();
    }

    @Test
    void financialOutcomesReflectCompetitiveSuccess() {
        World world = World.create(333L, small());
        world.advanceSeason();
        world.advanceSeason();

        List<Double> funds = world.activeGolferIds().stream()
                .map(id -> world.financialAccountOf(id).availableFunds())
                .toList();
        double max = funds.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        double min = funds.stream().mapToDouble(Double::doubleValue).min().orElseThrow();

        // Success pays: the field ends with a wide earnings spread rather than everyone level (REQ-183).
        assertThat(max).isGreaterThan(EconomyConstants.STARTING_FUNDS);
        assertThat(max).isGreaterThan(min);
        assertThat(max - min).isGreaterThan(EconomyConstants.STARTING_FUNDS);
    }

    @Test
    void theSeasonalSponsorshipCycleRuns() {
        World world = World.create(444L, small());
        world.advanceSeason();
        world.advanceSeason();

        boolean anySponsorship = world.activeGolferIds().stream()
                .map(world::financialAccountOf)
                .anyMatch(a -> a.sponsorshipIncome() > 0);
        assertThat(anySponsorship).as("at least one golfer signed and earned from a sponsorship").isTrue();
    }

    @Test
    void financialStateIsReproducibleFromTheSeed() {
        World a = World.create(555L, small());
        World b = World.create(555L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(summary(a)).isEqualTo(summary(b));
    }

    private static List<String> summary(World w) {
        return w.activeGolferIds().stream().sorted()
                .map(id -> {
                    FinancialAccount a = w.financialAccountOf(id);
                    return id + "|" + a.availableFunds() + "|" + a.careerEarnings() + "|" + a.careerExpenses();
                })
                .toList();
    }
}
