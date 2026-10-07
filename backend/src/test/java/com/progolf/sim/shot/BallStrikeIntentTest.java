package com.progolf.sim.shot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BallStrikeIntentTest {

    @Test
    void catalogueHasStableIndividualCarriesAndFamilyMappings() {
        assertThat(ClubSpec.of(ClubId.DRIVER).baseCarry()).isEqualTo(290.0);
        assertThat(ClubSpec.of(ClubId.SIX_IRON).baseCarry()).isEqualTo(180.0);
        assertThat(ClubSpec.of(ClubId.SAND_WEDGE).baseCarry()).isEqualTo(90.0);
        assertThat(ClubSpec.of(ClubId.THREE_WOOD).family()).isEqualTo(Club.FAIRWAY_WOOD);
        assertThat(ClubSpec.all()).hasSize(ClubId.values().length);
    }

    @Test
    void intentContainsOnlySelectedClubAndFiniteCanonicalPoint() {
        BallStrikeIntent intent = new BallStrikeIntent(ClubId.SEVEN_IRON, new AimPoint(-12.5, 155.0));
        assertThat(intent.club()).isEqualTo(ClubId.SEVEN_IRON);
        assertThat(intent.aimPoint()).isEqualTo(new AimPoint(-12.5, 155.0));
        assertThatThrownBy(() -> new AimPoint(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aimEnvelopeRejectsOnlyTechnicalOutliers() {
        AimEnvelope envelope = new AimEnvelope(-100, 100, -100, 500);
        assertThat(envelope.contains(new AimPoint(0, 250))).isTrue();
        assertThat(envelope.contains(new AimPoint(101, 250))).isFalse();
    }
}
