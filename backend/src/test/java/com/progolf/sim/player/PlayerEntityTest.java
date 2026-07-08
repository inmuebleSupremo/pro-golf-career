package com.progolf.sim.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.shot.GolferState;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Player-entity spec: identity validation/immutability, ownership, derived stats, state isolation. */
class PlayerEntityTest {

    private static Identity identity() {
        return new Identity("Sam", "Frost", Nationality.GBR, LocalDate.of(1998, 5, 4), Archetype.TOP_COLLEGE_GRADUATE);
    }

    private static Player player() {
        return new Player("p-1", identity(), Attributes.uniform(50));
    }

    @Test
    void identityRejectsInvalidNames() {
        assertThatThrownBy(() -> new Identity("", "Frost", Nationality.GBR, LocalDate.of(1998, 5, 4), Archetype.FUTURE_PRODIGY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Identity("A".repeat(51), "Frost", Nationality.GBR, LocalDate.of(1998, 5, 4), Archetype.FUTURE_PRODIGY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void playerReferencesTheSameAttributesInstanceItWasGiven() {
        Attributes attrs = Attributes.uniform(60);
        Player p = new Player("p-2", identity(), attrs);
        // Single source of truth: the Player holds the given Attributes by reference, not a copy.
        assertThat(p.attributes()).isSameAs(attrs);
    }

    @Test
    void stateChangesNeverMutatePermanentAttributes() {
        Player p = player();
        p.activate();
        p.state().setFatigue(0.9);
        p.state().recordPerformance(-20);
        p.applyInjury(Injury.of(Injury.InjuryType.WRIST, Injury.Severity.MODERATE));
        for (Attribute a : Attribute.values()) {
            assertThat(p.attributes().get(a)).isEqualTo(50);
        }
    }

    @Test
    void derivedFormRatingIsRecalculatedFromCurrentState() {
        Player p = player();
        double fresh = p.deriveFormRating();
        p.state().setFatigue(1.0);
        double fatigued = p.deriveFormRating();
        // Recomputed on demand: fatigue lowers form without being stored anywhere.
        assertThat(fatigued).isLessThan(fresh);
    }

    @Test
    void toGolferStateCarriesFatigueAndSituationalPressure() {
        Player p = player();
        p.state().setFatigue(0.4);
        GolferState gs = p.toGolferState(0.7);
        assertThat(gs.fatigue()).isEqualTo(0.4);
        assertThat(gs.pressure()).isEqualTo(0.7);
    }
}
