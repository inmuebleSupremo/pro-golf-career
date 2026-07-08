package com.progolf.sim.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Player-entity spec: career-status state machine. */
class CareerStatusTest {

    private static Player player() {
        Identity id = new Identity("Leo", "Meyer", Nationality.GER, LocalDate.of(2000, 1, 1), Archetype.FUTURE_PRODIGY);
        return new Player("p", id, Attributes.uniform(55));
    }

    @Test
    void validTransitionsAreAccepted() {
        Player p = player();
        assertThat(p.status()).isEqualTo(CareerStatus.CREATED);
        p.activate();
        assertThat(p.status()).isEqualTo(CareerStatus.ACTIVE);
        p.transitionTo(CareerStatus.RETIRED);
        assertThat(p.status()).isEqualTo(CareerStatus.RETIRED);
    }

    @Test
    void invalidTransitionsAreRejected() {
        Player p = player();
        p.activate();
        p.transitionTo(CareerStatus.RETIRED);
        assertThatThrownBy(() -> p.transitionTo(CareerStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
        assertThat(p.status()).isEqualTo(CareerStatus.RETIRED);
    }

    @Test
    void terminalStatesAreTerminal() {
        assertThat(CareerStatus.RETIRED.isTerminal()).isTrue();
        assertThat(CareerStatus.DECEASED.isTerminal()).isTrue();
        assertThat(CareerStatus.ACTIVE.isTerminal()).isFalse();
        assertThat(CareerStatus.DECEASED.canTransitionTo(CareerStatus.ACTIVE)).isFalse();
    }
}
