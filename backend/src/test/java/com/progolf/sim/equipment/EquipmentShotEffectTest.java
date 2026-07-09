package com.progolf.sim.equipment;

import static org.assertj.core.api.Assertions.assertThat;

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
 * equipment-influence / shot-resolution spec: a stronger bag tightens dispersion and extends reach, while
 * neutral (standard) equipment reproduces prior behaviour (REQ-206/207).
 */
class EquipmentShotEffectTest {

    private static final int SHOTS = 400;
    private static final double TARGET = 320.0; // beyond neutral driver reach, so power binds

    private static ShotOutcome driver(GolferState state, int i) {
        ShotContext ctx = new ShotContext(
                Attributes.uniform(55), state, Environment.calm(), TARGET, Fixtures.standardProfile(),
                ShotDecision.straight(Club.DRIVER, TARGET, Strategy.BALANCED), Fixtures.coordinate().withShot(i));
        return ShotResolver.resolveShot(ctx);
    }

    @Test
    void neutralEquipmentMatchesNoEquipment() {
        // A zero-bonus profile is bit-identical to fresh(): the equipment channel is exactly neutral.
        for (int i = 1; i <= 50; i++) {
            ShotOutcome plain = driver(GolferState.fresh(), i);
            ShotOutcome neutral = driver(new GolferState(0, 0, 0.0, 0.0), i);
            assertThat(neutral.carry()).isEqualTo(plain.carry());
            assertThat(neutral.lateral()).isEqualTo(plain.lateral());
        }
    }

    @Test
    void strongerEquipmentTightensDispersionAndExtendsReach() {
        GolferState neutral = new GolferState(0, 0, 0.0, 0.0);
        GolferState strong = new GolferState(0, 0, 0.10, 0.07); // realistic full-upgrade bonuses

        double neutralCarry = 0;
        double strongCarry = 0;
        double neutralSpread = 0;
        double strongSpread = 0;
        for (int i = 1; i <= SHOTS; i++) {
            ShotOutcome n = driver(neutral, i);
            ShotOutcome s = driver(strong, i);
            neutralCarry += n.carry();
            strongCarry += s.carry();
            neutralSpread += Math.abs(n.lateral());
            strongSpread += Math.abs(s.lateral());
        }
        assertThat(strongCarry).isGreaterThan(neutralCarry);   // power extends reach
        assertThat(strongSpread).isLessThan(neutralSpread);    // forgiveness tightens dispersion
    }
}
