package com.progolf.sim.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.shot.Environment;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** playing-conditions spec: the conditions model, internal consistency, course interaction, mapping. */
class PlayingConditionsTest {

    @Test
    void exposesTheFullEnvironmentalState() {
        PlayingConditions pc = PlayingConditions.of(12.0, 200.0, 0.3, 62.0, 0.5);
        assertThat(pc.windSpeed()).isGreaterThanOrEqualTo(0);
        assertThat(pc.windDirection()).isBetween(0.0, 360.0);
        assertThat(pc.rain()).isBetween(0.0, 1.0);
        assertThat(pc.humidity()).isBetween(0.0, 1.0);
        assertThat(pc.groundFirmness()).isBetween(0.0, 1.0);
        assertThat(pc.greenSpeed()).isBetween(0.0, 1.0);
        assertThat(pc.visibility()).isBetween(0.0, 1.0);
    }

    @Test
    void coupledFieldsCohere_rainSoftensSlowsAndDims() {
        PlayingConditions wet = PlayingConditions.of(6.0, 90.0, 0.9, 55.0, 0.9);
        PlayingConditions dry = PlayingConditions.of(6.0, 90.0, 0.0, 55.0, 0.3);

        // Rain softens the ground, slows the greens, and lowers visibility (REQ-229/230/237).
        assertThat(wet.groundFirmness()).isLessThan(dry.groundFirmness());
        assertThat(wet.greenSpeed()).isLessThan(dry.greenSpeed());
        assertThat(wet.visibility()).isLessThan(dry.visibility());
    }

    @Test
    void calmMapsExactlyToTheCalmShotEnvironment() {
        // Chosen so a weather-free tournament reproduces prior calm behaviour bit-for-bit.
        for (int hole = 1; hole <= 18; hole++) {
            for (double exposure : new double[] {0.3, 0.9, 1.0}) {
                Environment env = PlayingConditions.calm().environmentForHole(hole, exposure);
                assertThat(env).isEqualTo(Environment.calm());
            }
        }
    }

    @Test
    void windDecomposesDifferentlyAcrossHoles() {
        // A single wind bearing produces head/cross that vary by hole (some into wind, some downwind).
        PlayingConditions windy = PlayingConditions.of(25.0, 60.0, 0.0, 60.0, 0.4);
        Set<Double> heads = new HashSet<>();
        boolean sawInto = false;
        boolean sawDownwind = false;
        for (int hole = 1; hole <= 18; hole++) {
            Environment env = windy.environmentForHole(hole, 1.0);
            heads.add(env.headWind());
            sawInto = sawInto || env.headWind() > 1.0;
            sawDownwind = sawDownwind || env.headWind() < -1.0;
            assertThat(env.crossWind()).isGreaterThanOrEqualTo(0.0);
        }
        assertThat(heads.size()).isGreaterThan(1);
        assertThat(sawInto).isTrue();
        assertThat(sawDownwind).isTrue();
    }

    @Test
    void severityRisesWithWindAndRain() {
        PlayingConditions harsh = PlayingConditions.of(30.0, 0.0, 0.6, 50.0, 0.8);
        PlayingConditions benign = PlayingConditions.of(4.0, 0.0, 0.0, 68.0, 0.3);
        assertThat(harsh.severity()).isGreaterThan(benign.severity());
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new PlayingConditions(5, 400, 0.2, 60, 0.5, 0.6, 0.6, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlayingConditions(5, 90, 1.4, 60, 0.5, 0.6, 0.6, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlayingConditions(-1, 90, 0.2, 60, 0.5, 0.6, 0.6, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
