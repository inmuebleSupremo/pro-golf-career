package com.progolf.sim.progression;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.util.List;
import org.junit.jupiter.api.Test;

/** player-development (modified): development may follow a player-chosen focus (spec: player-control). */
class ProgressionFocusTest {

    /** Ample headroom, so these tests exercise the focus rather than the potential ceiling. */
    private static final Attributes UNCAPPED = Attributes.uniform(100);

    @Test
    void emptyFocusMatchesAutomaticDevelopment() {
        Attributes attrs = Attributes.uniform(50);
        Attributes focused = ProgressionEngine.develop(attrs, UNCAPPED, 20, 1.0, List.of());
        Attributes automatic = ProgressionEngine.develop(attrs, UNCAPPED, 20, 1.0);
        for (Attribute a : Attribute.values()) {
            assertThat(focused.get(a)).as(a.name()).isEqualTo(automatic.get(a));
        }
    }

    @Test
    void focusDirectsDevelopmentToTheChosenAttribute() {
        Attributes attrs = Attributes.uniform(50);
        Attributes developed = ProgressionEngine.develop(attrs, UNCAPPED, 20, 1.0,
                List.of(Attribute.DRIVING_DISTANCE));

        assertThat(developed.get(Attribute.DRIVING_DISTANCE)).isGreaterThan(50); // the focus got the points
        for (Attribute a : Attribute.values()) {
            if (a != Attribute.DRIVING_DISTANCE) {
                assertThat(developed.get(a)).as(a.name()).isEqualTo(50); // nothing else moved
            }
        }
    }

    @Test
    void pointsThatWouldBeWastedOnAMaxedFocusRollOnToTheNextPriority() {
        // The first priority is already at its ceiling: its points must carry to the second rather than
        // vanish, so a stale focus never silently costs a golfer their season.
        Attributes potential = Attributes.uniform(100).with(Attribute.DRIVING_DISTANCE, 50);
        Attributes attrs = Attributes.uniform(50);

        Attributes developed = ProgressionEngine.develop(attrs, potential, 20, 1.0,
                List.of(Attribute.DRIVING_DISTANCE, Attribute.PUTTING_ACCURACY));

        assertThat(developed.get(Attribute.DRIVING_DISTANCE)).isEqualTo(50); // maxed: bought nothing
        assertThat(developed.get(Attribute.PUTTING_ACCURACY)).isGreaterThan(50); // got the points instead
    }
}
