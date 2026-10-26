package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.TournamentResult;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** One ordinary Generator 8 world week exercises field selection, weather, setup and four-round play. */
class WorldEventScoringTest {
    @Test
    void strongerProFieldCanWinUnderParWhileWetDevelopmentEventRemainsCompetitive() {
        World world = World.create(7L, new WorldConfig(160, 6, 3, 40, 4, 0, 0));
        var opening = world.snapshot();
        Map<String, Double> skill = opening.golfers().stream().collect(Collectors.toMap(g -> g.player().id(),
                g -> java.util.Arrays.stream(Attribute.values())
                        .mapToInt(a -> g.player().attributes().get(a)).average().orElseThrow()));
        world.advanceWeek();

        var development = opening.schedule().stream().filter(e -> e.tier() == TourTier.DEVELOPMENT)
                .map(e -> world.resultOf(e.tournamentId())).filter(java.util.Optional::isPresent)
                .map(java.util.Optional::orElseThrow).findFirst().orElseThrow();
        var pro = opening.schedule().stream().filter(e -> e.tier() == TourTier.PRO)
                .map(e -> world.resultOf(e.tournamentId())).filter(java.util.Optional::isPresent)
                .map(java.util.Optional::orElseThrow).findFirst().orElseThrow();

        double developmentSkill = meanSkill(development, skill);
        double proSkill = meanSkill(pro, skill);
        assertThat(proSkill).isGreaterThan(developmentSkill + 10.0);
        assertThat(development.finishingOrder().getFirst().score()).isBetween(-15, 10);
        assertThat(pro.finishingOrder().getFirst().score()).isBetween(-24, -5);
        assertThat(pro.finishingOrder().getFirst().position()).isEqualTo(1);
        assertThat(development.finishingOrder().getFirst().position()).isEqualTo(1);
        for (TournamentResult result : java.util.List.of(development, pro)) {
            assertThat(result.finishingOrder()).isNotEmpty();
            assertThat(result.finishingOrder().stream().filter(TournamentResult.Finish::madeCut).count())
                    .isEqualTo(result.cutResult().madeCutCount());
            assertThat(result.finishingOrder().stream().filter(TournamentResult.Finish::madeCut)
                    .mapToInt(TournamentResult.Finish::score).min().orElseThrow())
                    .isEqualTo(result.finishingOrder().getFirst().score());
        }
    }

    private static double meanSkill(TournamentResult result, Map<String, Double> skill) {
        return result.finishingOrder().stream()
                .mapToDouble(f -> skill.get(f.golfer().player().id())).average().orElseThrow();
    }
}
