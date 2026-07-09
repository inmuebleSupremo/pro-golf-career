package com.progolf.sim.tour;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tour-membership spec: exactly-one membership, recorded changes, transparent qualification. */
class TourMembershipTest {

    @Test
    void golferHasExactlyOneMembershipAndDuplicateRegistrationIsRejected() {
        TourSystem system = new TourSystem();
        system.register("g1", TourTier.DEVELOPMENT);
        assertThat(system.membershipOf("g1")).contains(TourTier.DEVELOPMENT);
        assertThat(system.isMember("g1")).isTrue();
        assertThatThrownBy(() -> system.register("g1", TourTier.SECONDARY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void qualificationChangesMembershipAndIsRecorded() {
        TourSystem system = new TourSystem();
        system.register("g1", TourTier.DEVELOPMENT);
        system.grantMembership("g1", TourTier.SECONDARY, "Qualifying School");

        assertThat(system.membershipOf("g1")).contains(TourTier.SECONDARY); // replaced previous
        assertThat(system.movementHistory()).anySatisfy(m -> {
            assertThat(m.golferId()).isEqualTo("g1");
            assertThat(m.type()).isEqualTo(MovementType.QUALIFICATION);
            assertThat(m.fromTier()).isEqualTo(TourTier.DEVELOPMENT);
            assertThat(m.toTier()).isEqualTo(TourTier.SECONDARY);
        });
    }

    @Test
    void unknownGolferHasNoMembership() {
        TourSystem system = new TourSystem();
        assertThat(system.membershipOf("nobody")).isEmpty();
        assertThat(system.isMember("nobody")).isFalse();
    }
}
