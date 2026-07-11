package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.spatial.Surface;
import java.util.List;
import org.junit.jupiter.api.Test;

/** competitive-statistics: per-hole shot stats derived from the shots played. */
class HoleStatsTest {

    private static ShotOutcome shot(Surface surface, double remaining, boolean hazard, int strokes) {
        return new ShotOutcome(surface, 0, 0, remaining, hazard, hazard ? 1 : 0, strokes,
                new FactorBreakdown(0, 0, 0, 0));
    }

    /** A green shot that holes out. */
    private static ShotOutcome holed() {
        return shot(Surface.GREEN, 0.0, false, 1);
    }

    @Test
    void aRegulationParWithTwoPutts() {
        // Par 4: fairway drive, approach onto the green, two putts.
        HoleStats h = HoleStats.of(List.of(
                shot(Surface.FAIRWAY, 150, false, 1),
                shot(Surface.GREEN, 12, false, 1),
                shot(Surface.GREEN, 1, false, 1),
                holed()), 4);
        assertThat(h.fairwayEligible()).isTrue();
        assertThat(h.fairwayHit()).isTrue();
        assertThat(h.greenInRegulation()).isTrue(); // on the green in 2 (= par - 2)
        assertThat(h.putts()).isEqualTo(2);
    }

    @Test
    void aMissedFairwayStillCountsAsAnAttempt() {
        HoleStats h = HoleStats.of(List.of(
                shot(Surface.PRIMARY_ROUGH, 160, false, 1),
                shot(Surface.GREEN, 10, false, 1),
                shot(Surface.GREEN, 1, false, 1),
                holed()), 4);
        assertThat(h.fairwayEligible()).isTrue();
        assertThat(h.fairwayHit()).isFalse();
    }

    @Test
    void aPar3HasNoFairwayButCanHitTheGreenInRegulation() {
        HoleStats h = HoleStats.of(List.of(
                shot(Surface.GREEN, 15, false, 1), // tee shot onto the green
                shot(Surface.GREEN, 1, false, 1),
                holed()), 3);
        assertThat(h.fairwayEligible()).isFalse();
        assertThat(h.fairwayHit()).isFalse();
        assertThat(h.greenInRegulation()).isTrue(); // on the green in 1 (= par - 2)
        assertThat(h.putts()).isEqualTo(2);
    }

    @Test
    void aThreePuttCountsThreePutts() {
        HoleStats h = HoleStats.of(List.of(
                shot(Surface.FAIRWAY, 150, false, 1),
                shot(Surface.GREEN, 20, false, 1),
                shot(Surface.GREEN, 4, false, 1),
                shot(Surface.GREEN, 1, false, 1),
                holed()), 4);
        assertThat(h.putts()).isEqualTo(3);
    }

    @Test
    void aHazardRuinsRegulation() {
        // Drive into water (penalty), reload, reach the green late — no GIR.
        HoleStats h = HoleStats.of(List.of(
                shot(Surface.WATER, 150, true, 2),   // hazard: stroke-and-distance, no progress
                shot(Surface.FAIRWAY, 150, false, 1),
                shot(Surface.GREEN, 10, false, 1),
                shot(Surface.GREEN, 1, false, 1),
                holed()), 4);
        assertThat(h.greenInRegulation()).isFalse(); // reached the green in 4 strokes, not par-2
        assertThat(h.fairwayHit()).isFalse();        // the tee shot found water, not fairway
    }
}
