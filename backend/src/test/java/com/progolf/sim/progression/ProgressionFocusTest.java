package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.List;
import org.junit.jupiter.api.Test;

/** player-development (modified): development may follow a player-chosen focus (spec: player-control). */
class ProgressionFocusTest {

    @Test
    void emptyFocusMatchesAutomaticDevelopment() {
        Attributes attrs = Attributes.uniform(50);
        Attributes focused = ProgressionEngine.develop(attrs, 20, 1.0, List.of());
        Attributes automatic = ProgressionEngine.develop(attrs, 20, 1.0);
        for (Attribute a : Attribute.values()) {
            assertThat(focused.get(a)).as(a.name()).isEqualTo(automatic.get(a));
        }
    }

    @Test
    void focusDirectsDevelopmentToTheChosenAttribute() {
        Attributes attrs = Attributes.uniform(50);
        Attributes developed = ProgressionEngine.develop(attrs, 20, 1.0, List.of(Attribute.DRIVING_DISTANCE));

        assertThat(developed.get(Attribute.DRIVING_DISTANCE)).isGreaterThan(50); // the focus got the points
        for (Attribute a : Attribute.values()) {
            if (a != Attribute.DRIVING_DISTANCE) {
                assertThat(developed.get(a)).as(a.name()).isEqualTo(50); // nothing else moved
            }
        }
    }
}
