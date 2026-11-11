package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.economy.EconomyConstants;
import com.progolf.sim.economy.FinancialAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
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

    @Test
    void seasonEndFinancialRankingMatchesPreOptimisationFixture() {
        // Captured on develop before ranking reuse. Includes equal ranking values and two seasons of award history.
        WorldConfig config = new WorldConfig(12, 2, 1, 6, 3, 0, 0);
        World world = World.create(6789L, config);
        world.advanceSeason();
        world.advanceSeason();
        World restored = World.restore(6789L, config, world.snapshot());
        assertThat(restored.snapshot()).isEqualTo(world.snapshot());

        List<String> ranking = world.rankingSnapshots().stream()
                .flatMap(snapshot -> snapshot.standings().stream()
                        .map(standing -> snapshot.asOf() + "|" + standing.position() + "|"
                                + standing.golferId() + "|" + standing.rankingValue()))
                .toList();
        List<String> finances = world.activeGolferIds().stream().sorted()
                .map(id -> {
                    var account = world.financialAccountOf(id).snapshot();
                    return id + "|" + account.availableFunds() + "|" + account.tournamentEarnings()
                            + "|" + account.sponsorshipIncome() + "|" + account.careerExpenses()
                            + "|" + account.commercialMomentum() + "|" + account.activeAgreements();
                })
                .toList();

        assertThat(world.rankingSnapshots()).extracting(snapshot -> snapshot.asOf())
                .containsExactly(LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 22));
        assertThat(world.rankingSnapshots().get(0).standings().stream()
                .filter(standing -> standing.rankingValue() == 20.4)
                .map(standing -> standing.golferId()).toList())
                .containsExactly("golfer-3602b73aace03848", "golfer-c0152c0555d48bfc");
        assertThat(fingerprint(ranking))
                .isEqualTo("04c430016b9e45bd6fc31e3cc989d73e1de1504f08ace23b7c5a6ba4c7bd3401");
        assertThat(fingerprint(finances))
                .isEqualTo("f59b9011bdb4c161eccb7d9ffa32e7ee767eb8c7e39de7ab126f83b7d6cb01f4");
    }

    private static String fingerprint(List<String> lines) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
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
