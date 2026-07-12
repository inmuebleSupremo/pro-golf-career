package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.support.Fixtures;
import org.junit.jupiter.api.Test;

/** shot-resolution injury-impairment: a recovering-injury handicap degrades shots and is physical (not soothed by the psychologist). */
class InjuryImpairmentShotTest {

    private static final Attributes ATTRS = Attributes.uniform(60);

    /** A neutral 250y driver shot at shot n with the given player condition. */
    private static ShotContext shot(GolferState state, int n) {
        return new ShotContext(ATTRS, state, Environment.calm(), 250.0, Fixtures.standardProfile(),
                ShotDecision.straight(Club.DRIVER, 250.0, Strategy.BALANCED),
                Fixtures.coordinate().withShot(n));
    }

    private static GolferState impaired(double impairment, double mentalSupport) {
        return new GolferState(0.0, 0.0, 0.0, 0.0, mentalSupport, 0.0, 0.0, 0.0, impairment);
    }

    /** Mean distance-remaining to the pin over many shots — a proxy for how far shots stray. */
    private static double meanDistanceRemaining(double impairment) {
        double total = 0;
        int n = 400;
        for (int i = 1; i <= n; i++) {
            total += ShotResolver.resolveShot(shot(impaired(impairment, 0.0), i)).distanceRemaining();
        }
        return total / n;
    }

    @Test
    void higherImpairmentStraysFurtherFromTheTarget() {
        // Impairment magnitudes mirror InjurySeverity MINOR (0.05) and SEVERE (0.30); literal to keep this a
        // pure shot-domain test with no health-domain import.
        double healthy = meanDistanceRemaining(0.0);
        double minor = meanDistanceRemaining(0.05);
        double severe = meanDistanceRemaining(0.30);
        // Monotonic degradation: a recovering-severe golfer misses by more than a minor, who misses more than healthy.
        assertThat(minor).isGreaterThan(healthy);
        assertThat(severe).isGreaterThan(minor);
    }

    @Test
    void mentalSupportDoesNotRelieveInjuryImpairment() {
        // At zero fatigue/pressure, mental support has nothing else to act on, so the injured outcome must be
        // identical with or without it — the impairment is physical, not mental.
        ShotOutcome noSupport = ShotResolver.resolveShot(shot(impaired(0.30, 0.0), 3));
        ShotOutcome maxSupport = ShotResolver.resolveShot(shot(impaired(0.30, 1.0), 3));
        assertThat(maxSupport).isEqualTo(noSupport);
    }

    @Test
    void zeroImpairmentReproducesFreshPlay() {
        // A zero-impairment condition is byte-identical to fresh play (neutrality of the new input).
        ShotOutcome impairedZero = ShotResolver.resolveShot(shot(impaired(0.0, 0.0), 9));
        ShotOutcome fresh = ShotResolver.resolveShot(shot(GolferState.fresh(), 9));
        assertThat(impairedZero).isEqualTo(fresh);
    }
}
