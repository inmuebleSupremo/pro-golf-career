package com.progolf.sim.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.tournament.Tier;
import com.progolf.sim.tournament.TournamentResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** World-ranking spec: awards, ranking value/order, decay, field strength, eligibility, reproducibility. */
class WorldRankingTest {

    private static final LocalDate DAY = LocalDate.of(2001, 5, 1);

    @Test
    void recordingProducesAValueOrderedRankingWithUniquePositions() {
        List<ProfessionalGolfer> field = RankingFixtures.golfers(20);
        WorldRanking wr = new WorldRanking();
        wr.record(RankingFixtures.result("Open", field), Tier.STANDARD, DAY, 1);

        var snapshot = wr.rankingAsOf(DAY);
        assertThat(snapshot.size()).isGreaterThan(0);
        // Winner earns the most points and stands at #1.
        assertThat(snapshot.leader()).isPresent();
        assertThat(snapshot.leader().get().golferId()).isEqualTo(field.get(0).player().id());
        // Positions are unique 1..n and values are non-increasing.
        for (int i = 0; i < snapshot.standings().size(); i++) {
            assertThat(snapshot.standings().get(i).position()).isEqualTo(i + 1);
            if (i > 0) {
                assertThat(snapshot.standings().get(i).rankingValue())
                        .isLessThanOrEqualTo(snapshot.standings().get(i - 1).rankingValue());
            }
        }
    }

    @Test
    void rankingValueDecaysOverTimeAndExpires() {
        List<ProfessionalGolfer> field = RankingFixtures.golfers(10);
        WorldRanking wr = new WorldRanking();
        wr.record(RankingFixtures.result("Open", field), Tier.PREMIER, DAY, 1);
        String winner = field.get(0).player().id();

        double atEvent = wr.rankingValue(winner, DAY);
        double later = wr.rankingValue(winner, DAY.plusDays(300));
        double expired = wr.rankingValue(winner, DAY.plusDays(RankingConstants.WINDOW_DAYS + 1));

        assertThat(atEvent).isGreaterThan(0);
        assertThat(later).isLessThan(atEvent).isGreaterThan(0);
        assertThat(expired).isEqualTo(0.0);
    }

    @Test
    void strongerFieldAwardsMorePointsForTheSameFinish() {
        List<ProfessionalGolfer> golfers = RankingFixtures.golfers(6);
        WorldRanking wr = new WorldRanking();
        // Seed the first three with prior ranking value so their field is "strong".
        for (int i = 0; i < 3; i++) {
            wr.ledger().add(new RankingAward(golfers.get(i).player().id(), DAY.minusDays(30), 200.0, 99));
        }
        List<ProfessionalGolfer> strong = golfers.subList(0, 3);
        List<ProfessionalGolfer> weak = golfers.subList(3, 6);

        wr.record(RankingFixtures.result("Strong", strong), Tier.STANDARD, DAY, 10);
        wr.record(RankingFixtures.result("Weak", weak), Tier.STANDARD, DAY, 11);

        double strongWinnerPoints = pointsFor(wr, strong.get(0).player().id(), 10);
        double weakWinnerPoints = pointsFor(wr, weak.get(0).player().id(), 11);
        assertThat(strongWinnerPoints).isGreaterThan(weakWinnerPoints);
    }

    @Test
    void ineligibleGolferLeavesActiveRankingButKeepsHistory() {
        List<ProfessionalGolfer> field = RankingFixtures.golfers(8);
        WorldRanking wr = new WorldRanking();
        wr.record(RankingFixtures.result("Open", field), Tier.STANDARD, DAY, 1);
        String winner = field.get(0).player().id();

        assertThat(wr.rankingAsOf(DAY).positionOf(winner)).isPresent();
        wr.markIneligible(winner);
        assertThat(wr.rankingAsOf(DAY).positionOf(winner)).isEmpty();
        // History preserved: the value is still computable from the ledger.
        assertThat(wr.rankingValue(winner, DAY)).isGreaterThan(0);
    }

    @Test
    void rankingIsReproducibleFromTheSameResults() {
        List<ProfessionalGolfer> field = RankingFixtures.golfers(30);
        TournamentResult r = RankingFixtures.result("Open", field);

        WorldRanking a = new WorldRanking();
        a.record(r, Tier.PREMIER, DAY, 1);
        WorldRanking b = new WorldRanking();
        b.record(r, Tier.PREMIER, DAY, 1);

        assertThat(a.rankingAsOf(DAY).standings()).isEqualTo(b.rankingAsOf(DAY).standings());
    }

    private static double pointsFor(WorldRanking wr, String golferId, long tournamentId) {
        return wr.ledger().awards().stream()
                .filter(x -> x.golferId().equals(golferId) && x.tournamentId() == tournamentId)
                .mapToDouble(RankingAward::points).findFirst().orElseThrow();
    }
}
