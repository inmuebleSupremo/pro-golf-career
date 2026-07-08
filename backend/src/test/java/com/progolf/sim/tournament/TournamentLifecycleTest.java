package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.player.CareerStatus;
import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tournament-definition & tournament-play specs: registration, lifecycle order, read-only completion. */
class TournamentLifecycleTest {

    @Test
    void statesAdvanceInOrderAndAreNotSkipped() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(1), TournamentFixtures.field(20));
        assertThat(t.state()).isEqualTo(TournamentState.FIELD_CONFIRMED);
        t.advance();
        assertThat(t.state()).isEqualTo(TournamentState.ROUND_1);
        t.advance();
        assertThat(t.state()).isEqualTo(TournamentState.ROUND_2);
        t.advance();
        assertThat(t.state()).isEqualTo(TournamentState.CUT);
        t.advance();
        assertThat(t.state()).isEqualTo(TournamentState.ROUND_3);
        t.advance();
        assertThat(t.state()).isEqualTo(TournamentState.ROUND_4);
        t.advance();
        // After the final round, either a playoff is required or the event completes.
        assertThat(t.state()).isIn(TournamentState.PLAYOFF, TournamentState.COMPLETED);
    }

    @Test
    void cannotAdvanceBeforeFieldConfirmed() {
        Tournament t = new Tournament(TournamentFixtures.standardDefinition(2));
        assertThatThrownBy(t::advance).isInstanceOf(IllegalStateException.class);
        t.openRegistration();
        assertThatThrownBy(t::advance).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registrationRejectsDuplicatesInactiveClosedAndFull() {
        List<ProfessionalGolfer> golfers = TournamentFixtures.field(5);
        // Closed before opening.
        Tournament t = new Tournament(TournamentFixtures.definition(TournamentFixtures.course(), 3,
                TournamentFormat.standard(), new EntryRequirements(2, true)));
        assertThat(t.register(golfers.get(0)).reason()).isEqualTo(RegistrationResult.Reason.REGISTRATION_CLOSED);

        t.openRegistration();
        assertThat(t.register(golfers.get(0)).succeeded()).isTrue();
        // Duplicate.
        assertThat(t.register(golfers.get(0)).reason()).isEqualTo(RegistrationResult.Reason.DUPLICATE_ENTRY);
        // Inactive.
        golfers.get(1).player().transitionTo(CareerStatus.RETIRED);
        assertThat(t.register(golfers.get(1)).reason()).isEqualTo(RegistrationResult.Reason.NOT_ELIGIBLE_INACTIVE);
        // Field full (max 2): one more valid entry fills it, the next is rejected.
        assertThat(t.register(golfers.get(2)).succeeded()).isTrue();
        assertThat(t.register(golfers.get(3)).reason()).isEqualTo(RegistrationResult.Reason.FIELD_FULL);
    }

    @Test
    void fieldIsFixedOncePlayBegins() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(4), TournamentFixtures.field(10));
        t.advance(); // Round 1 begins
        assertThat(t.register(TournamentFixtures.field(1).get(0)).reason())
                .isEqualTo(RegistrationResult.Reason.REGISTRATION_CLOSED);
    }

    @Test
    void completedTournamentIsReadOnly() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(5), TournamentFixtures.field(16));
        t.playToCompletion();
        assertThat(t.state()).isEqualTo(TournamentState.COMPLETED);
        assertThatThrownBy(t::advance).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> t.withdraw(t.field().get(0).golfer())).isInstanceOf(IllegalStateException.class);
    }
}
