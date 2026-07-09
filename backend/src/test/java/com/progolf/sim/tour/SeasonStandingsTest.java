package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tour-movement spec: the season-points curve, accumulation, reset, and deterministic ordering. */
class SeasonStandingsTest {

    @Test
    void pointsCurveDecreasesAndFloorsAtZero() {
        assertThat(SeasonStandings.pointsFor(1)).isGreaterThan(SeasonStandings.pointsFor(2));
        assertThat(SeasonStandings.pointsFor(2)).isGreaterThan(SeasonStandings.pointsFor(10));
        assertThat(SeasonStandings.pointsFor(1000)).isEqualTo(0);
    }

    @Test
    void pointsAccumulateAndReset() {
        SeasonStandings s = new SeasonStandings();
        s.award("g", 1);
        s.award("g", 10);
        assertThat(s.pointsOf("g")).isEqualTo(SeasonStandings.pointsFor(1) + SeasonStandings.pointsFor(10));
        s.reset();
        assertThat(s.pointsOf("g")).isZero();
    }

    @Test
    void rankedOrdersByPointsWithDeterministicTieBreak() {
        SeasonStandings s = new SeasonStandings();
        s.award("b", 1); // 100
        s.award("a", 1); // 100 (tie with b)
        s.award("c", 5); // fewer
        List<String> ranked = s.ranked(List.of("c", "b", "a"));
        // Tie between a and b resolved by natural id order (a before b); c last.
        assertThat(ranked).containsExactly("a", "b", "c");
    }
}
