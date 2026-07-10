package com.progolf.sim.control;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import java.util.List;
import org.junit.jupiter.api.Test;

/** player-control spec: the player's standing decisions are held and updatable. */
class PlayerControlTest {

    @Test
    void holdsAndUpdatesTheStandingDecisions() {
        PlayerControl control = new PlayerControl("g1");
        assertThat(control.golferId()).isEqualTo("g1");
        assertThat(control.developmentFocus()).isEmpty();
        assertThat(control.isResting()).isFalse();

        control.setDevelopmentFocus(List.of(Attribute.PUTTING_ACCURACY, Attribute.WEDGES));
        control.setResting(true);
        assertThat(control.developmentFocus()).containsExactly(Attribute.PUTTING_ACCURACY, Attribute.WEDGES);
        assertThat(control.isResting()).isTrue();

        control.setDevelopmentFocus(null); // clearing falls back to automatic development
        assertThat(control.developmentFocus()).isEmpty();
    }
}
