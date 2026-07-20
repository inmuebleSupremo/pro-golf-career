package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards realistic tournament scoring (spec: shot-resolution / course-setup). Real four-round winning scores
 * are roughly -10 to -22; greens-in-regulation for a strong golfer is ~65-70%. This exists because the game
 * spent several versions with winners at -35 to -40 every week — no one in golf history shoots four straight
 * rounds of nine under, yet it happened every event — which made "I shot ten under and missed the cut" a
 * routine, immersion-breaking result. If the dispersion or setup calibration drifts back to machine-tight
 * golfers, this fails.
 */
class WorldScoringRealismTest {

    @Test
    void fourRoundWinningScoresAreRealisticAcrossTheTours() {
        World world = World.create(7L);
        for (int s = 1; s <= 10; s++) {
            world.advanceSeason();
        }
        Map<String, List<Double>> winners = new java.util.TreeMap<>();
        for (var archive : world.archives()) {
            Map<Long, ScheduledTournament> byId = new java.util.HashMap<>();
            archive.schedule().forEach(e -> byId.put(e.tournamentId(), e));
            for (var r : archive.results()) {
                ScheduledTournament e = byId.get(r.tournamentId());
                if (e == null || e.prestige().isMajor()) {
                    continue; // majors draw a cross-tour field; judge the regular tour events
                }
                winners.computeIfAbsent(e.tier().name(), k -> new ArrayList<>())
                        .add((double) r.finishingOrder().get(0).score());
            }
        }
        assertThat(winners).isNotEmpty();
        winners.forEach((tier, scores) -> {
            double meanWinner = scores.stream().mapToDouble(d -> d).average().orElse(0);
            // Not absurdly low (the -35..-40 regression) and not so hard that the best never break par.
            assertThat(meanWinner)
                    .as("%s mean four-round winning score to par", tier)
                    .isGreaterThan(-24.0)
                    .isLessThan(-5.0);
        });
    }
}
