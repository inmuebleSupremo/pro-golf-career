package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

/** shot-resolution (lay-up vs go-for-it): a long par-5 approach is a lay-up-or-go decision by disposition
 *  and by the golfer's reach (playing to a long hitter's strength). */
class LayUpStrategyTest {

    private static final double PAR5_APPROACH = 250.0; // a long second shot, beyond comfortable iron range
    private static final Attributes SHORT_HITTER = Attributes.uniform(50).with(Attribute.DRIVING_DISTANCE, 30);
    private static final Attributes LONG_HITTER = Attributes.uniform(50).with(Attribute.DRIVING_DISTANCE, 95);

    private static ShotDecision decide(Strategy s, Attributes attrs, int par) {
        return new StrategyPolicy(s).decide(PAR5_APPROACH, Surface.FAIRWAY, 0.0, attrs, par);
    }

    @Test
    void aggressionGoesForTheGreenConservatismLaysUp() {
        // Aggressive fires at the green with the long club; conservative lays up to a shorter club/target.
        ShotDecision attack = decide(Strategy.AGGRESSIVE, SHORT_HITTER, 5);
        ShotDecision layup = decide(Strategy.CONSERVATIVE, SHORT_HITTER, 5);
        assertThat(attack.club()).isEqualTo(Club.FAIRWAY_WOOD);       // going for it
        assertThat(attack.targetDistance()).isGreaterThan(200.0);
        assertThat(layup.club()).isEqualTo(Club.IRON);                // laid up
        assertThat(layup.targetDistance()).isLessThan(PAR5_APPROACH - 50); // well short — leaves a wedge
    }

    @Test
    void aLongHitterGoesForItEvenWhenConservative() {
        // Playing to strength: a bomber can reach comfortably, so even a conservative one goes for it;
        // a short hitter with the same disposition lays up.
        assertThat(decide(Strategy.CONSERVATIVE, LONG_HITTER, 5).club()).isEqualTo(Club.FAIRWAY_WOOD);
        assertThat(decide(Strategy.CONSERVATIVE, SHORT_HITTER, 5).club()).isEqualTo(Club.IRON);
    }

    @Test
    void onlyPar5sOfferALayUp() {
        // A par 4 has no stroke to spare: the same long approach goes for the green whatever the disposition.
        assertThat(decide(Strategy.CONSERVATIVE, SHORT_HITTER, 4).club()).isEqualTo(Club.FAIRWAY_WOOD);
    }

    @Test
    void aTeeShotNeverLaysUp() {
        // From the tee (a par-5 drive or a long par-3) the golfer always goes.
        ShotDecision tee = new StrategyPolicy(Strategy.CONSERVATIVE)
                .decide(PAR5_APPROACH, Surface.TEE_BOX, 0.0, SHORT_HITTER, 5);
        assertThat(tee.club()).isEqualTo(Club.FAIRWAY_WOOD);
    }
}
