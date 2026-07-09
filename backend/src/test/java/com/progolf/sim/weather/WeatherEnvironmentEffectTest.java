package com.progolf.sim.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.support.Fixtures;
import org.junit.jupiter.api.Test;

/**
 * tournament-weather spec (REQ-233): conditions measurably affect resolution, so adapting to them
 * matters — and the shot engine's wind resistance (tied to driving distance) is rewarded under wind.
 */
class WeatherEnvironmentEffectTest {

    private static final int SHOTS = 400;
    // Pure headwind on hole 1: its synthesized bearing equals this wind direction, so cos(theta)=1.
    private static final int HOLE = 1;
    private static final double WIND_DIRECTION = (HOLE * WeatherConstants.BEARING_STEP_DEGREES) % 360.0;

    private static double meanCarry(Attributes attrs, Environment env) {
        double total = 0;
        for (int i = 1; i <= SHOTS; i++) {
            ShotContext ctx = new ShotContext(
                    attrs, GolferState.fresh(), env, 250.0, Fixtures.standardProfile(),
                    ShotDecision.straight(Club.DRIVER, 250.0, Strategy.BALANCED),
                    Fixtures.coordinate().withShot(i));
            ShotOutcome out = ShotResolver.resolveShot(ctx);
            total += out.carry();
        }
        return total / SHOTS;
    }

    @Test
    void headwindReducesCarry() {
        Attributes mid = Attributes.uniform(55);
        Environment calm = Environment.calm();
        Environment head = PlayingConditions.of(28.0, WIND_DIRECTION, 0.0, 58.0, 0.4).environmentForHole(HOLE, 1.0);
        assertThat(head.headWind()).isGreaterThan(5.0); // a genuine headwind
        assertThat(meanCarry(mid, head)).isLessThan(meanCarry(mid, calm));
    }

    @Test
    void windResistanceIsRewarded() {
        Attributes strong = Attributes.uniform(55).with(Attribute.DRIVING_DISTANCE, 90);
        Attributes weak = Attributes.uniform(55).with(Attribute.DRIVING_DISTANCE, 20);
        Environment calm = Environment.calm();
        Environment head = PlayingConditions.of(28.0, WIND_DIRECTION, 0.0, 58.0, 0.4).environmentForHole(HOLE, 1.0);

        double penaltyStrong = meanCarry(strong, calm) - meanCarry(strong, head);
        double penaltyWeak = meanCarry(weak, calm) - meanCarry(weak, head);

        // Both lose carry to the wind, but the wind-resistant (high driving distance) golfer loses less.
        assertThat(penaltyStrong).isGreaterThan(0);
        assertThat(penaltyWeak).isGreaterThan(0);
        assertThat(penaltyStrong).isLessThan(penaltyWeak);
    }
}
