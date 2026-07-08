package com.progolf.sim.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Player-entity spec: zero-or-one injury, recovery clock, INJURED<->ACTIVE coordination. */
class InjuryStateTest {

    private static Player activePlayer() {
        Identity id = new Identity("Ben", "Cole", Nationality.USA, LocalDate.of(1995, 3, 9), Archetype.GRASS_ROOTS_TALENT);
        Player p = new Player("p", id, Attributes.uniform(50));
        p.activate();
        return p;
    }

    @Test
    void applyingInjuryMovesPlayerToInjured() {
        Player p = activePlayer();
        p.applyInjury(Injury.of(Injury.InjuryType.BACK, Injury.Severity.MODERATE));
        assertThat(p.status()).isEqualTo(CareerStatus.INJURED);
        assertThat(p.state().hasActiveInjury()).isTrue();
    }

    @Test
    void aSecondSimultaneousInjuryIsRejected() {
        Player p = activePlayer();
        p.applyInjury(Injury.of(Injury.InjuryType.BACK, Injury.Severity.MINOR));
        // Applying again through the guarded path is rejected (already INJURED, not ACTIVE).
        assertThatThrownBy(() -> p.applyInjury(Injury.of(Injury.InjuryType.WRIST, Injury.Severity.MINOR)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(p.state().hasActiveInjury()).isTrue();
    }

    @Test
    void recoveryClockDecrementsAndReturnsToActiveWhenHealed() {
        Player p = activePlayer();
        p.applyInjury(Injury.of(Injury.InjuryType.WRIST, Injury.Severity.MINOR)); // MINOR = 2 steps
        boolean healedAfterOne = p.advanceInjuryRecovery(1);
        assertThat(healedAfterOne).isFalse();
        assertThat(p.status()).isEqualTo(CareerStatus.INJURED);

        boolean healedAfterTwo = p.advanceInjuryRecovery(1);
        assertThat(healedAfterTwo).isTrue();
        assertThat(p.state().hasActiveInjury()).isFalse();
        assertThat(p.status()).isEqualTo(CareerStatus.ACTIVE);
    }

    @Test
    void onlyActivePlayersCanBeInjured() {
        Identity id = new Identity("Tom", "Bauer", Nationality.AUS, LocalDate.of(1990, 7, 7), Archetype.TOP_COLLEGE_GRADUATE);
        Player created = new Player("p", id, Attributes.uniform(50)); // status CREATED
        assertThatThrownBy(() -> created.applyInjury(Injury.of(Injury.InjuryType.KNEE, Injury.Severity.MINOR)))
                .isInstanceOf(IllegalStateException.class);
    }
}
