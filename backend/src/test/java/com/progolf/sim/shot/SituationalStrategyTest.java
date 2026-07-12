package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

/** shot-resolution (situational strategy): the policy adapts aim/strategy to the lie, pin, and distance. */
class SituationalStrategyTest {

    private static final double PIN = 6.0; // a pin tucked +6 from centre

    @Test
    void aDifficultLieForcesConservativePlay() {
        StrategyPolicy aggressive = new StrategyPolicy(Strategy.AGGRESSIVE);
        // From deep rough / bunker the golfer recovers conservatively, whatever their disposition.
        assertThat(aggressive.decide(150, Surface.DEEP_ROUGH, PIN).strategy()).isEqualTo(Strategy.CONSERVATIVE);
        assertThat(aggressive.decide(150, Surface.BUNKER, PIN).strategy()).isEqualTo(Strategy.CONSERVATIVE);
        // From a clean lie the golfer keeps their disposition.
        assertThat(aggressive.decide(150, Surface.FAIRWAY, PIN).strategy()).isEqualTo(Strategy.AGGRESSIVE);
    }

    @Test
    void aggressivePlayAimsAtThePinOnAScoringApproach() {
        // A short approach from a clean lie: aggressive aims toward the tucked pin, conservative at centre.
        double aggAim = new StrategyPolicy(Strategy.AGGRESSIVE).decide(110, Surface.FAIRWAY, PIN).targetLateral();
        double consAim = new StrategyPolicy(Strategy.CONSERVATIVE).decide(110, Surface.FAIRWAY, PIN).targetLateral();
        assertThat(aggAim).isGreaterThan(0.0);        // hunting the flag
        assertThat(aggAim).isLessThanOrEqualTo(PIN);  // never past the pin
        assertThat(consAim).isEqualTo(0.0);            // playing the safe centre
    }

    @Test
    void pinAttackFadesWithDistance() {
        StrategyPolicy aggressive = new StrategyPolicy(Strategy.AGGRESSIVE);
        double shortAim = aggressive.decide(110, Surface.FAIRWAY, PIN).targetLateral();
        double midAim = aggressive.decide(165, Surface.FAIRWAY, PIN).targetLateral();
        double longAim = aggressive.decide(260, Surface.FAIRWAY, PIN).targetLateral();
        assertThat(shortAim).isGreaterThan(midAim);   // commits more from wedge range
        assertThat(midAim).isGreaterThan(0.0);
        assertThat(longAim).isZero();                  // a long approach plays the centre
    }

    @Test
    void aimIsSignedTowardThePin() {
        // A pin tucked the other way is attacked the other way.
        double aim = new StrategyPolicy(Strategy.AGGRESSIVE).decide(110, Surface.FAIRWAY, -6.0).targetLateral();
        assertThat(aim).isLessThan(0.0);
    }
}
